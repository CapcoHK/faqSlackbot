package com.capco.bots.faq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;

/**
 * Created by Bhushan on 7/7/2017.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class FAQHandlerTest {
    private FAQHandler faqHandler = new FAQHandler(null, null);

    @Test
    public void WHEN_question_with_a_single_answer_is_parsed_THEN_it_is_correctly_parsed_into_key_value_pair(){
        Map<String, String> parsedResponse = faqHandler.parseResponse(getSingleHitResponse());
        assertThat(parsedResponse, is(notNullValue()));
        assertThat(parsedResponse.size(), is(1));
        String key = parsedResponse.keySet().iterator().next();
        String value = parsedResponse.values().iterator().next();
        assertThat(key, is("Question: How do I delete my CIT profile"));
        assertThat(value, is("Answer: Ask admin to help you delete your CIT profile"));
    }

    private String getSingleHitResponse() {
        return "{\n" +
                "                \"responseHeader\": {\n" +
                "                                \"status\": 0,\n" +
                "                                \"QTime\": 35\n" +
                "                },\n" +
                "                \"response\": {\n" +
                "                                \"numFound\": 1,\n" +
                "                                \"start\": 0,\n" +
                "                                \"maxScore\": 1.4545189,\n" +
                "                                \"docs\": [{\n" +
                "                                                \"file\": \"/home/sridhar/Downloads/slack/taming-text/book/bin/faq.txt_1\",\n" +
                "                                                \"docid\": \"2\",\n" +
                "                                                \"timestamp\": \"2017-07-05T03:21:47.156Z\",\n" +
                "                                                \"body\": [\"Ask admin to help you delete your CIT profile\"],\n" +
                "                                                \"doctitle\": [\"How do I delete my CIT profile\"],\n" +
                "                                                \"score\": 1.4545189\n" +
                "                                }]\n" +
                "                },\n" +
                "                \"qaResponse\": [\"answer\", [\"docid\", \"2\", \"field\", \"doctitle\", \"window\", \"How do I delete my CIT profile\"]]\n" +
                "}\n";
    }

    @Test
    public void WHEN_question_with_multiple_answers_is_parsed_THEN_it_is_correctly_parsed_into_key_value_pairs(){
        Map<String, String> parsedResponse = faqHandler.parseResponse(getMultipleHitsResponse());
        assertThat(parsedResponse, is(notNullValue()));
        assertThat(parsedResponse.size(), is(3));
        assertThat(parsedResponse.entrySet(), everyItem(isIn(expectedAnswersForMultipleHits().entrySet())));
        assertThat(expectedAnswersForMultipleHits().entrySet(), everyItem(isIn(parsedResponse.entrySet())));
    }

    private Map<String, String> expectedAnswersForMultipleHits(){
        Map<String, String> retval = new HashMap<>();
        retval.put("Question: How do I delete my CIT profile", "Answer: Please visit CIT (https://cit.capco.com/login) > Login > Me > About me > Edit profile.");
        retval.put("Question: How do I setup my CIT profile", "Answer: Please visit CIT (https://cit.capco.com/login) > Login > Me > About me > Edit profile.");
        retval.put("Question: Who will be viewing my CIT profile?", "Answer: People who login CIT / CIT community can search your name and view your profile.");
        return retval;
    }

    private String getMultipleHitsResponse(){
        return "{\n" +
                "\t\"responseHeader\": {\n" +
                "\t\t\"status\": 0,\n" +
                "\t\t\"QTime\": 10\n" +
                "\t},\n" +
                "\t\"response\": {\n" +
                "\t\t\"numFound\": 3,\n" +
                "\t\t\"start\": 0,\n" +
                "\t\t\"maxScore\": 1.6644546,\n" +
                "\t\t\"docs\": [{\n" +
                "\t\t\t\"file\": \"/home/sridhar/Downloads/slack/taming-text/book/bin/faq.txt_1\",\n" +
                "\t\t\t\"docid\": \"2\",\n" +
                "\t\t\t\"timestamp\": \"2017-07-07T08:08:50.216Z\",\n" +
                "\t\t\t\"body\": [\"Please visit CIT (https://cit.capco.com/login) > Login > Me > About me > Edit profile.\"],\n" +
                "\t\t\t\"doctitle\": [\"How do I delete my CIT profile\"],\n" +
                "\t\t\t\"score\": 1.6644546\n" +
                "\t\t}, {\n" +
                "\t\t\t\"file\": \"/home/sridhar/Downloads/slack/taming-text/book/bin/faq.txt_3\",\n" +
                "\t\t\t\"docid\": \"4\",\n" +
                "\t\t\t\"timestamp\": \"2017-07-07T08:08:50.216Z\",\n" +
                "\t\t\t\"body\": [\"People who login CIT / CIT community can search your name and view your profile.\"],\n" +
                "\t\t\t\"doctitle\": [\"Who will be viewing my CIT profile?\"],\n" +
                "\t\t\t\"score\": 1.6644546\n" +
                "\t\t}, {\n" +
                "\t\t\t\"file\": \"/home/sridhar/Downloads/slack/taming-text/book/bin/faq.txt_0\",\n" +
                "\t\t\t\"docid\": \"1\",\n" +
                "\t\t\t\"timestamp\": \"2017-07-07T08:08:50.216Z\",\n" +
                "\t\t\t\"body\": [\"Please visit CIT (https://cit.capco.com/login) > Login > Me > About me > Edit profile.\"],\n" +
                "\t\t\t\"doctitle\": [\"How do I setup my CIT profile\"],\n" +
                "\t\t\t\"score\": 1.4563978\n" +
                "\t\t}]\n" +
                "\t},\n" +
                "\t\"qaResponse\": [\"answer\", [\"docid\", \"4\", \"field\", \"doctitle\", \"window\", \"Who will be viewing my CIT profile?\"], \"answer\", [\"docid\", \"2\", \"field\", \"doctitle\", \"window\", \"How do I delete my CIT profile\"], \"answer\", [\"docid\", \"1\", \"field\", \"doctitle\", \"window\", \"How do I setup my CIT profile\"]]\n" +
                "}";
    }

    @Test
    public void WHEN_question_with_a_no_answer_is_parsed_THEN_it_is_correctly_parsed(){
        Map<String, String> parsedResponse = faqHandler.parseResponse(getAnswerNotFoundResponse());
        assertThat(parsedResponse, is(notNullValue()));
        assertThat(parsedResponse.size(), is(0));
    }

    private String getAnswerNotFoundResponse(){
        return "{\n" +
                "\t\"responseHeader\": {\n" +
                "\t\t\"status\": 0,\n" +
                "\t\t\"QTime\": 14\n" +
                "\t},\n" +
                "\t\"response\": {\n" +
                "\t\t\"numFound\": 0,\n" +
                "\t\t\"start\": 0,\n" +
                "\t\t\"maxScore\": 0.0,\n" +
                "\t\t\"docs\": []\n" +
                "\t},\n" +
                "\t\"qaResponse\": []\n" +
                "}";
    }
}
