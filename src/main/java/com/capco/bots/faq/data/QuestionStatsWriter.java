package com.capco.bots.faq.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.capco.util.StringUtil.replacePunctuations;
import static java.util.Arrays.stream;

/**
 * Created by Bhushan on 7/28/2017.
 */

public class QuestionStatsWriter {
    private static Logger logger = LogManager.getLogger(QuestionStatsWriter.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private final File outputFile;
    private final Date date = new Date();
    private final String user;
    private final String originalQue;
    private final Map<String, String> results;
    private final Set<String> stopWords;
    private XSSFWorkbook workbook;
    private boolean existingExcelFile;

    private List<SheetWriter> sheetWriters = new ArrayList<>();

    public QuestionStatsWriter(String user, String originalQue, Map<String, String> results, Set<String> stopWords, String outputFilePath) throws IOException, InvalidFormatException {
        this.user = user;
        this.originalQue = originalQue;
        this.results = results;
        this.outputFile = new File(outputFilePath);
        this.stopWords = stopWords;
        Path path = Paths.get(outputFilePath);
        logger.debug("OutputPath: {}",outputFilePath);
        this.existingExcelFile = Files.exists(path) && Files.isRegularFile(path);
        if (existingExcelFile) {
            try {
                logger.debug("Has the File: {}", outputFilePath);
                this.workbook = new XSSFWorkbook(outputFile);
                logger.debug("Open the file {}", outputFilePath);
            } catch (NotOfficeXmlFileException | InvalidFormatException e) {
                logger.debug("NotOfficeXmlFileException or Invalid Format Exception found");
                outputFile.delete();
                logger.debug("Deleted successfully");
                this.workbook = new XSSFWorkbook();
                this.existingExcelFile = false;
                logger.error("NotOffierXmlFileException or InvalidFormatException" + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("IOException" + e.getMessage());
            } catch (Exception ex)
            {
                logger.error("Exception" + ex.getMessage());
            }

        } else {
            logger.debug("Created new one");
            this.workbook = new XSSFWorkbook();
        }
    }

    public synchronized void write() throws IOException, InvalidFormatException {
        try{
            initSheetWriters();
            logger.debug("initSheetWriters ");
            sheetWriters.forEach(SheetWriter::write);
            logger.debug("Write on each worksheet");
            flushFile();
            logger.debug("flushFile");
        } catch (Exception e)
        {
            logger.error(e.getMessage());
        }

    }

    private void flushFile() throws IOException{
        FileOutputStream outputStream = new FileOutputStream(outputFile, true);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
    }

    private void initSheetWriters() throws IOException, InvalidFormatException {
        logger.info("Initializing sheet writers...");
        sheetWriters.add(newUnansweredQuestionWriter());
        sheetWriters.add(newMultipleAnswersToQuestionWriter());
        sheetWriters.add(newSingleAnswerQuestionWriter());
    }

    private SheetWriter<UnansweredQuestion> newUnansweredQuestionWriter() {
        SheetWriter<UnansweredQuestion> writer = new SheetWriter<>("Unanswered", workbook);
        if (results.isEmpty()) {
            logger.info("Writing unanswered question to file : {}", originalQue);
            String searchedKeywords = String.join(",",
                            stream(replacePunctuations(originalQue)
                                .split(" "))
                                .map(String::toLowerCase)
                                .filter(s -> !stopWords.contains(s))
                                .collect(Collectors.toSet()));
            writer.addRow(new UnansweredQuestion(date, user, originalQue, searchedKeywords));
        }
        return writer;
    }

    private SheetWriter<MultipleAnswersToQuestion> newMultipleAnswersToQuestionWriter() {
        SheetWriter<MultipleAnswersToQuestion> writer = new SheetWriter<>("Multiple answers", workbook);
        if (results.size() > 1) {
            logger.info("Writing question with {} answers to file : {}", results.size(), originalQue);
            String searchedKeywords = String.join(",",
                    stream(replacePunctuations(originalQue)
                            .split(" "))
                            .map(String::toLowerCase)
                            .filter(s -> !stopWords.contains(s))
                            .collect(Collectors.toSet()));
            writer.addRow(new MultipleAnswersToQuestion(date, user, originalQue, searchedKeywords, results.size()));
        }
        return writer;
    }

    private SheetWriter<SingleAnswerQuestion> newSingleAnswerQuestionWriter() {
        SheetWriter<SingleAnswerQuestion> writer = new SheetWriter<>("Single answer", workbook);
        if (results.size() == 1) {
            logger.info("Writing question with single answer to file : {}", originalQue);
            String searchedKeywords = String.join(",",
                    stream(replacePunctuations(originalQue)
                            .split(" "))
                            .map(String::toLowerCase)
                            .filter(s -> !stopWords.contains(s))
                            .collect(Collectors.toSet()));
            writer.addRow(new SingleAnswerQuestion(date, user, originalQue, results.values().iterator().next(), searchedKeywords));
        }
        return writer;
    }

    public class SheetWriter<T extends Writable> {
        private final String sheetName;
        private final XSSFWorkbook workbook;
        private final List<T> dataRows = new ArrayList<>();
        private final CellStyle headerStyle;
        private final int zoomPercent = 90;

        public SheetWriter(String sheetName, XSSFWorkbook workbook) {
            this.sheetName = sheetName;
            this.workbook = workbook;
            this.headerStyle = workbook.createCellStyle();
            this.headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            this.headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        public void addRow(T row) {
            this.dataRows.add(row);
        }

        public void write(){
            if (dataRows.isEmpty()) {
                logger.debug("No rows to write for sheet {}", sheetName);
                return;
            }

            XSSFSheet sheet = getOrCreate(sheetName);
            setZoom(sheet);
            writeHeaderRow(sheet);
            writeDataRows(sheet);
            autoSizeColumns(sheet);
        }

        private void setZoom(XSSFSheet sheet) {
            sheet.setZoom(zoomPercent);
        }

        private void autoSizeColumns(XSSFSheet sheet) {
            XSSFRow row = sheet.getRow(0);
            for(int i = 0; i < row.getLastCellNum(); i++){
                sheet.autoSizeColumn(i);
            }
        }

        private void writeDataRows(XSSFSheet sheet) {
            int lastRowNum = sheet.getLastRowNum();
            for (T dataRow : dataRows) {
                logger.debug("Writing in sheet : {}, row : {}", sheetName, dataRow.getDataRow());
                XSSFRow row = getOrCreateRow(sheet, ++lastRowNum);
                int cellIndex = 0;
                for (String data : dataRow.getDataRow()) {
                    row.getCell(cellIndex++, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(data);
                }
            }
        }

        private void writeHeaderRow(XSSFSheet sheet) {
            XSSFRow row = getOrCreateRow(sheet, 0);
            if (row.getCell(0) != null) {
                return; //assuming that is the sheet already exists then header must be pre-written
            }
            T dataRow = dataRows.iterator().next();
            int colIndex = 0;
            logger.debug("Writing header row {} for sheet {}", dataRow.getHeaders(), sheetName);
            for (String headerCol : dataRow.getHeaders()) {
                XSSFCell cell = row.getCell(colIndex++, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellValue(headerCol);
                cell.setCellStyle(headerStyle);
            }
        }

        private XSSFRow getOrCreateRow(XSSFSheet sheet, int rowNum) {
            XSSFRow row = sheet.getRow(rowNum);
            return row != null ? row : sheet.createRow(rowNum);
        }

        private XSSFSheet getOrCreate(String sheetName) {
            XSSFSheet sheet = workbook.getSheet(sheetName);
            return sheet != null ? sheet : workbook.createSheet(sheetName);
        }
    }

    public interface Writable {
        String[] getHeaders();
        String[] getDataRow();
    }

    static class UnansweredQuestion implements Writable{
        private static final String[] HEADERS = new String[]{"Date", "User", "Original Question", "Keywords"};
        private final Date date;
        private final String user;
        private final String originalQue;
        private final String keyWordsCSV;

        UnansweredQuestion(Date date, String user, String originalQue, String keyWordsCSV) {
            this.date = date;
            this.user = user;
            this.originalQue = originalQue;
            this.keyWordsCSV = keyWordsCSV;
        }

        @Override
        public String[] getHeaders() {
            return HEADERS;
        }

        @Override
        public String[] getDataRow() {
            return new String[]{sdf.format(date), user, originalQue, keyWordsCSV};
        }
    }

    static class MultipleAnswersToQuestion implements Writable {
        private static final String[] HEADERS = new String[]{"Date", "User", "Original Question", "Keywords", "No. of answers"};
        private final Date date;
        private final String user;
        private final String originalQue;
        private final String keyWordsCSV;
        private final int count;

        MultipleAnswersToQuestion(Date date, String user, String originalQue, String keyWordsCSV, int count) {
            this.date = date;
            this.user = user;
            this.originalQue = originalQue;
            this.keyWordsCSV = keyWordsCSV;
            this.count = count;
        }

        @Override
        public String[] getHeaders() {
            return HEADERS;
        }

        @Override
        public String[] getDataRow() {
            return new String[]{sdf.format(date), user, originalQue, keyWordsCSV, ""+count};
        }
    }

    static class SingleAnswerQuestion implements Writable {
        private static final String[] HEADERS = new String[]{"Date", "User", "Original Question", "Single Answer", "Keywords"};
        private final Date date;
        private final String user;
        private final String originalQue;
        private final String answer;
        private final String keyWordsCSV;

        SingleAnswerQuestion(Date date, String user, String originalQue, String answer, String keyWordsCSV) {
            this.date = date;
            this.user = user;
            this.originalQue = originalQue;
            this.answer = answer;
            this.keyWordsCSV = keyWordsCSV;
        }

        @Override
        public String[] getHeaders() {
            return HEADERS;
        }

        @Override
        public String[] getDataRow() {
            return new String[]{sdf.format(date), user, originalQue, answer, keyWordsCSV};
        }
    }

}
