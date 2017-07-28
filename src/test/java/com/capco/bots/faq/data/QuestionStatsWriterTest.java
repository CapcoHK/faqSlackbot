package com.capco.bots.faq.data;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by Bhushan on 7/28/2017.
 */
@RunWith(BlockJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Ignore("Circle CI build fails as it doesn't permit creating new excel files")
public class QuestionStatsWriterTest {
    private final static String singleAnswerFilePath = "singleanswer.xlsx";
    private final static String multiAnsweredFilePath = "multianswered.xlsx";
    private final static String unansweredFilePath = "unanswered.xlsx";
    private final static String allScenarioFilePath = "questionstats.xlsx";

    @Test
    public void A_GIVEN_no_existing_file_WHEN_question_with_single_answer_is_logged_THEN_it_is_written_to_the_correct_excel_sheet() throws IOException, InvalidFormatException {
        //before
        try {
            Files.delete(Paths.get(singleAnswerFilePath));
        } catch (IOException e) {
            //do nothing
        }

        //when
        Map<String, String> results = new HashMap<>();
        String originalQue = "How to delete CIT profile?";
        results.put(originalQue, "Contact admin");
        Set<String> stopWords = new HashSet<>();
        stopWords.add("how");
        stopWords.add("to");
        QuestionStatsWriter writer = new QuestionStatsWriter("TestUser", originalQue, results, stopWords, singleAnswerFilePath);
        writer.write();

        //then
        XSSFWorkbook workbook = new XSSFWorkbook(singleAnswerFilePath);
        assertThat(workbook.getNumberOfSheets(), is(1));
        assertThat(workbook.getSheet("Single answer"), is(notNullValue()));
        assertThat(workbook.getSheet("Single answer").getLastRowNum(), is(1));
    }

    @Test
    public void B_GIVEN_an_existing_file_with_one_question_WHEN_question_with_single_answer_is_logged_THEN_it_is_appended_to_the_correct_excel_sheet() throws IOException, InvalidFormatException {
        Map<String, String> results = new HashMap<>();
        String originalQue = "How to create a CIT profile?";
        results.put(originalQue, "Auto created");
        Set<String> stopWords = new HashSet<>();
        stopWords.add("how");
        stopWords.add("to");
        stopWords.add("create");
        stopWords.add("a");
        QuestionStatsWriter writer = new QuestionStatsWriter("TestUser2", originalQue, results, stopWords, singleAnswerFilePath);
        writer.write();

        //assertions
        XSSFWorkbook workbook = new XSSFWorkbook(singleAnswerFilePath);
        assertThat(workbook.getNumberOfSheets(), is(1));
        assertThat(workbook.getSheet("Single answer"), is(notNullValue()));
        assertThat(workbook.getSheet("Single answer").getLastRowNum(), is(2));
    }

    @Test
    public void C_GIVEN_no_existing_file_WHEN_question_with_multiple_answer_is_logged_THEN_it_is_written_to_the_correct_excel_sheet() throws IOException, InvalidFormatException {
        //before
        try {
            Files.delete(Paths.get(multiAnsweredFilePath));
        } catch (IOException e) {
            //do nothing
        }

        Map<String, String> results = new HashMap<>();
        String originalQue = "CIT profile?";
        results.put("How to delete my CIT profile?", "Contact admin");
        results.put("How to create my CIT profile?", "Auto created");
        Set<String> stopWords = new HashSet<>();
        stopWords.add("how");
        stopWords.add("to");
        stopWords.add("my");
        QuestionStatsWriter writer = new QuestionStatsWriter("TestUser", originalQue, results, stopWords, multiAnsweredFilePath);
        writer.write();

        //assertions
        XSSFWorkbook workbook = new XSSFWorkbook(multiAnsweredFilePath);
        assertThat(workbook.getNumberOfSheets(), is(1));
        assertThat(workbook.getSheet("Multiple answers"), is(notNullValue()));
        assertThat(workbook.getSheet("Multiple answers").getLastRowNum(), is(1));
    }

    @Test
    public void D_GIVEN_an_existing_file_with_one_multi_question_entry_WHEN_question_with_multiple_answer_is_logged_THEN_it_is_appended_to_the_correct_excel_sheet() throws IOException, InvalidFormatException {
        Map<String, String> results = new HashMap<>();
        String originalQue = "salary?";
        results.put("How to get my salary?", "Finance dept handles it with your induction");
        results.put("When will my salary be credited?", "1st of every month");
        Set<String> stopWords = new HashSet<>();
        stopWords.add("how");
        stopWords.add("to");
        stopWords.add("my");
        stopWords.add("get");
        stopWords.add("when");
        stopWords.add("will");
        stopWords.add("be");
        QuestionStatsWriter writer = new QuestionStatsWriter("TestUser2", originalQue, results, stopWords, multiAnsweredFilePath);
        writer.write();

        //assertions
        XSSFWorkbook workbook = new XSSFWorkbook(multiAnsweredFilePath);
        assertThat(workbook.getNumberOfSheets(), is(1));
        assertThat(workbook.getSheet("Multiple answers"), is(notNullValue()));
        assertThat(workbook.getSheet("Multiple answers").getLastRowNum(), is(2));
    }

    @Test
    public void E_GIVEN_no_existing_file_WHEN_question_with_no_answer_is_logged_THEN_it_is_written_to_the_correct_excel_sheet() throws IOException, InvalidFormatException {
        //before
        try {
            Files.delete(Paths.get(unansweredFilePath));
        } catch (IOException e) {
            //do nothing
        }

        Map<String, String> results = new HashMap<>();
        String originalQue = "how to unanswered my question1";
        Set<String> stopWords = new HashSet<>();
        stopWords.add("how");
        stopWords.add("to");
        stopWords.add("my");
        QuestionStatsWriter writer = new QuestionStatsWriter("TestUser", originalQue, results, stopWords, unansweredFilePath);
        writer.write();

        //assertions
        XSSFWorkbook workbook = new XSSFWorkbook(unansweredFilePath);
        assertThat(workbook.getNumberOfSheets(), is(1));
        assertThat(workbook.getSheet("Unanswered"), is(notNullValue()));
        assertThat(workbook.getSheet("Unanswered").getLastRowNum(), is(1));
    }

    @Test
    public void F_GIVEN_an_existing_file_WHEN_question_with_no_answer_is_logged_THEN_it_is_appended_to_the_correct_excel_sheet() throws IOException, InvalidFormatException {
        //before

        Map<String, String> results = new HashMap<>();
        String originalQue = "how to ignore my question";
        Set<String> stopWords = new HashSet<>();
        stopWords.add("how");
        stopWords.add("to");
        stopWords.add("my");
        QuestionStatsWriter writer = new QuestionStatsWriter("TestUser2", originalQue, results, stopWords, unansweredFilePath);
        writer.write();

        //assertions
        XSSFWorkbook workbook = new XSSFWorkbook(unansweredFilePath);
        assertThat(workbook.getNumberOfSheets(), is(1));
        assertThat(workbook.getSheet("Unanswered"), is(notNullValue()));
        assertThat(workbook.getSheet("Unanswered").getLastRowNum(), is(2));
    }

    @Test
    public void G_GIVEN_no_existing_file_WHEN_questions_with_one_multiple_and_none_answers_are_logged_THEN_they_are_written_to_the_correct_excel_sheets() throws IOException, InvalidFormatException {
        //before
        try {
            Files.delete(Paths.get(allScenarioFilePath));
        } catch (IOException e) {
            //do nothing
        }

        //when A
        Map<String, String> results = new HashMap<>();
        String originalQue = "How to delete CIT profile?";
        results.put(originalQue, "Contact admin");
        Set<String> stopWords = new HashSet<>();
        stopWords.add("how");
        stopWords.add("to");
        QuestionStatsWriter writer = new QuestionStatsWriter("TestUser", originalQue, results, stopWords, allScenarioFilePath);
        writer.write();

        //then
        XSSFWorkbook workbook = new XSSFWorkbook(allScenarioFilePath);
        assertThat(workbook.getNumberOfSheets(), is(1));
        assertThat(workbook.getSheet("Single answer"), is(notNullValue()));
        assertThat(workbook.getSheet("Single answer").getLastRowNum(), is(1));

        //when B
        Map<String, String> results2 = new HashMap<>();
        String originalQue2 = "How to create a CIT profile?";
        results2.put(originalQue2, "Auto created");
        Set<String> stopWords2 = new HashSet<>();
        stopWords.add("how");
        stopWords.add("to");
        stopWords.add("create");
        stopWords.add("a");
        writer = new QuestionStatsWriter("TestUser2", originalQue2, results2, stopWords2, allScenarioFilePath);
        writer.write();

        //assertions
        workbook = new XSSFWorkbook(allScenarioFilePath);
        assertThat(workbook.getNumberOfSheets(), is(1));
        assertThat(workbook.getSheet("Single answer"), is(notNullValue()));
        assertThat(workbook.getSheet("Single answer").getLastRowNum(), is(2));


        //when C
        Map<String, String> results3 = new HashMap<>();
        String originalQue3 = "CIT profile?";
        results3.put("How to delete my CIT profile?", "Contact admin");
        results3.put("How to create my CIT profile?", "Auto created");
        Set<String> stopWords3 = new HashSet<>();
        stopWords3.add("how");
        stopWords3.add("to");
        stopWords3.add("my");
        writer = new QuestionStatsWriter("TestUser", originalQue3, results3, stopWords3, allScenarioFilePath);
        writer.write();

        //assertions
        workbook = new XSSFWorkbook(allScenarioFilePath);
        assertThat(workbook.getNumberOfSheets(), is(2));
        assertThat(workbook.getSheet("Multiple answers"), is(notNullValue()));
        assertThat(workbook.getSheet("Multiple answers").getLastRowNum(), is(1));

        //when D
        Map<String, String> results4 = new HashMap<>();
        String originalQue4 = "salary?";
        results4.put("How to get my salary?", "Finance dept handles it with your induction");
        results4.put("When will my salary be credited?", "1st of every month");
        Set<String> stopWords4 = new HashSet<>();
        stopWords4.add("how");
        stopWords4.add("to");
        stopWords4.add("my");
        stopWords4.add("get");
        stopWords4.add("when");
        stopWords4.add("will");
        stopWords4.add("be");
        writer = new QuestionStatsWriter("TestUser2", originalQue4, results4, stopWords4, allScenarioFilePath);
        writer.write();

        //assertions
        workbook = new XSSFWorkbook(allScenarioFilePath);
        assertThat(workbook.getNumberOfSheets(), is(2));
        assertThat(workbook.getSheet("Multiple answers"), is(notNullValue()));
        assertThat(workbook.getSheet("Multiple answers").getLastRowNum(), is(2));

        //when E
        Map<String, String> results5 = new HashMap<>();
        String originalQue5 = "how to unanswered my question1";
        Set<String> stopWords5 = new HashSet<>();
        stopWords5.add("how");
        stopWords5.add("to");
        stopWords5.add("my");
        writer = new QuestionStatsWriter("TestUser", originalQue5, results5, stopWords5, allScenarioFilePath);
        writer.write();

        //assertions
        workbook = new XSSFWorkbook(allScenarioFilePath);
        assertThat(workbook.getNumberOfSheets(), is(3));
        assertThat(workbook.getSheet("Unanswered"), is(notNullValue()));
        assertThat(workbook.getSheet("Unanswered").getLastRowNum(), is(1));

        //when F
        Map<String, String> results6 = new HashMap<>();
        String originalQue6 = "how to ignore my question";
        Set<String> stopWords6 = new HashSet<>();
        stopWords6.add("how");
        stopWords6.add("to");
        stopWords6.add("my");
        writer = new QuestionStatsWriter("TestUser2", originalQue6, results6, stopWords6, allScenarioFilePath);
        writer.write();

        //assertions
        workbook = new XSSFWorkbook(allScenarioFilePath);
        assertThat(workbook.getNumberOfSheets(), is(3));
        assertThat(workbook.getSheet("Unanswered"), is(notNullValue()));
        assertThat(workbook.getSheet("Unanswered").getLastRowNum(), is(2));
    }
}
