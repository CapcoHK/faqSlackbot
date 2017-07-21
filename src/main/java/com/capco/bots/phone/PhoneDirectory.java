package com.capco.bots.phone;


import com.capco.bots.IBotHandler;
import com.capco.bots.phone.data.PhoneEntry;
import com.capco.bots.IBotsEnum;
import com.capco.bots.phone.trie.Trie;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.capco.bots.phone.trie.Trie.removeLastChar;

/**
 * Created by Sridhar on 4/25/2017.
 */
public class PhoneDirectory implements IBotHandler {

    private static Logger logger = LogManager.getLogger(PhoneDirectory.class);

    private List<PhoneEntry> directoryList;

    private Trie trie = new Trie();

    public PhoneDirectory(String path) {
        directoryList = new ArrayList<>();
        Path exelPath = Paths.get(path);
        try (FileInputStream fileInputStream = new FileInputStream(exelPath.toFile())) {
            XSSFWorkbook myWorkBook = new XSSFWorkbook(fileInputStream);
            XSSFSheet mySheet = myWorkBook.getSheetAt(0);
            Iterator<Row> rowIterator = mySheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() == 0)
                    continue;
                Iterator<Cell> cellIterator = row.cellIterator();
                String name = "";
                String number = "";

                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING: {
                            if (cell.getColumnIndex() == 0) {
                                name = cell.getStringCellValue();
                                name = normalizeInput(name);
                            }

                            if (cell.getColumnIndex() == 2) {
                                number = cell.getStringCellValue();
                            }
                        }
                        default:
                    }
                }
                directoryList.add(new PhoneEntry(name, number));
            }
            init();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        for (PhoneEntry entry : directoryList) {
            String[] firstLastNames = entry.getName().trim().split(" ");
            for (String str : firstLastNames) {
                trie.insert(str.trim().toLowerCase(), entry);
            }
        }
    }

    @Override
    public String processMessage(String user, String inputName) {
        logger.debug("User : {}, requested : [{}] for search", user, inputName);
        String response = "";
        String normalizedInputName = normalizeInput(inputName);

        if(normalizedInputName.trim().isEmpty()){

            return "Please enter at least one valid alphabet";
        }
        try {
            //-- Welcome Message
            if (normalizedInputName.trim().toLowerCase().equals("hi") || normalizedInputName.trim().toLowerCase().equals("hello")) {
                return normalizedInputName.trim() + user + "! I am PhoneBot and can help you find Capco phone numbers. Please tell me the name or partial name you are looking for. ";
            }
            List<String> nameList = Arrays.stream(normalizedInputName.split(" ")).collect(Collectors.toList());

            //-- If we don't have any input
            if (nameList.size() == 0) {
                return "Please enter something...";
            }


            //-- Pick the first Name
            String requestedName = nameList.get(0);
            nameList.remove(nameList.get(0));

            //-- Normalize to lowercase
            String searchableName = requestedName.trim().toLowerCase();
            logger.debug("Searchable Name  {}", searchableName);

            //-- Try to look for Partial match. The input should partially match from the beginning of the cached entries

            Set<PhoneEntry> result = trie.lookUpName(searchableName);


            //-- If there is no partial match
            if (result == null) {
                logger.debug("Trying Brute force as tries returned null");

                //-- Do a brute force search.
                /*
                  Due to time constraints we are unable to implement a O(1) search via Tries.
                  Ideally tries should be able to return this to us.
                 */
                Set<PhoneEntry> brute = new HashSet<>();
                for (PhoneEntry entry : directoryList) {
                    if (entry.getName().toLowerCase().contains(searchableName.toLowerCase().trim())) {
                        brute.add(entry);
                    }
                }
                if (brute.size() == 1) {
                    return "Phone number of " + brute.stream().findAny().get().getName() + " is: " + brute.stream().findAny().get().getNumber();
                }

                if (brute.size() > 1) {
                    response = "I was able to find the following partial matches: \n";
                    for (PhoneEntry entry : brute) {
                        response += entry.getName() + ": " + entry.getNumber() + " \n ";
                    }
                    return response;
                }

                //-- We reach here if brute force matching fails
                response += " I was unable to find any names with the input \"" + searchableName + "\".";
                if (searchableName.length() > 1) {
                    //-- We now search by removing the last character of the first input word.
                    String newSearch = removeLastChar(searchableName);

                    logger.debug("Trying with last character removed {}", newSearch);
                    //-- See if Tries can find any match
                    result = trie.lookUpName(newSearch);

                    //-- if Tries did find some names... just display them
                    if (result != null) {
                        response += " Did you mean: \n ";
                        for (PhoneEntry entry : result) {
                            response += entry.getName() + ": " + entry.getNumber() + " \n ";
                        }
                        return response;
                    }
                }
            } else {
                logger.debug("Tries returned {} names", result.size());
                for (PhoneEntry ent : result) {
                    logger.debug(ent.getName());
                }

                if (result.size() == 1) {
                    return "Phone number of " + result.stream().findAny().get().getName() + " is: " + result.stream().findAny().get().getNumber();
                } else {
                    Set<PhoneEntry> finallist = new HashSet<>();
                    for (String names : nameList) {
                        for (PhoneEntry entry : result) {
                            if (entry.getName().toLowerCase().contains(names.trim().toLowerCase())) {
                                finallist.add(entry);
                            }
                        }
                    }
                    if (finallist.isEmpty()) {
                        finallist = result;
                    }
                    if (finallist.size() > 1) {
                        response = "I was able to find the following partial matches: \n";
                        for (PhoneEntry entry : finallist) {
                            response += entry.getName() + ": " + entry.getNumber() + " \n ";
                        }
                        return response;
                    } else {

                        return "Phone number of " + finallist.stream().findFirst().get().getName() + " is: " + finallist.stream().findFirst().get().getNumber();
                    }
                }
            }

        } catch (RuntimeException e) {
            return "Unable to process " + normalizedInputName;
        }
        return response;
    }

    public String normalizeInput(String inputName) {
        inputName = inputName.replaceAll("[^A-Za-z]", " ");
        return inputName.trim().replaceAll(" +", " ");
    }

    @Override
    public String getId() {
        return IBotsEnum.PHONE.toString();
    }
}
