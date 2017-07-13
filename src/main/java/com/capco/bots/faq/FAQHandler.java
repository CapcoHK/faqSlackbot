package com.capco.bots.faq;

import com.capco.bots.IBotHandler;
import com.capco.bots.IBotsEnum;
import com.capco.bots.faq.data.Docs;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Handles FAQs
 * Created by vijayalaxmi on 23/6/2017.
 */
public class FAQHandler implements IBotHandler {

    private static Logger logger = LogManager.getLogger(FAQHandler.class);

    private final String unansweredQuestionFilePath;

    public FAQHandler(String unansweredQuestionFilePath) {
        this.unansweredQuestionFilePath = unansweredQuestionFilePath;
        logger.debug("Unanswered questions will be logged to : {}", unansweredQuestionFilePath);
    }

    @Override
    public void init() {
        logger.debug("FAQHandler init");
    }

    @Override
    public String processMessage(String user, String message) {
        logger.debug("User : {}, message : {}", user, message);
        if (message.trim().toLowerCase().equals("hi") || message.trim().toLowerCase().equals("hello")) {
            return message.trim() + "! I am FAQBot and can help you find answers for FAQs related to Capco. Please enter your question or partial question with keywords. ";
        }

        StringBuilder result = new StringBuilder();
        try {
            String queryableMessage = convertToQueryable(message);
            Map<String, String> questionAnswerMap = queryFAQWebService(queryableMessage);
            if (questionAnswerMap.isEmpty()) {
                logUnansweredQuestion(message);
                result.append("Couldn't find a perfect match for your query. We have stored your query and will look into it. ");
                questionAnswerMap = doApproximateSearch(message);
                if (!questionAnswerMap.isEmpty()) {
                    result.append("Meanwhile here are some approximate answers to your question.\n");
                    result.append(convertToString(questionAnswerMap));
                } else {
                    result.append("Meanwhile feel free to contact admin if urgent...");
                }
            } else {
                result.append(convertToString(questionAnswerMap));
            }
        } catch (Exception e) {
            result.append("Unable to process :").append(message);
            logger.error("Error while processing message : {}", message, e);
        }
        logger.debug("returning result : {}", result);
        return result.toString();
    }

    private Map<String, String> _queryFAQWebService(String queryString){
        try {
            return queryFAQWebService(queryString);
        } catch (Exception e) {
            logger.error("Exception while querying webservice", e);
        }
        return Collections.emptyMap();
    }

    private Map<String, String> doApproximateSearch(String message) {
        String[] queryTerms = message.split(" ");
        CompletableFuture<Map<String, String>> cf = null;
        for (String qt : queryTerms) {
            if (cf == null) {
                cf = CompletableFuture.supplyAsync(() -> _queryFAQWebService(qt));
            } else {
                cf = cf.thenCombineAsync(CompletableFuture.supplyAsync(() -> _queryFAQWebService(qt)), (m1, m2)-> {m1.putAll(m2);return m1;});
            }
        }
        try {
            return cf.get();
        } catch (InterruptedException|ExecutionException e) {
            logger.error("Exception while waiting for cumulative response", e);
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

    private String convertToString(Map<String, String> questionAnswerMap) {
        String result;
        StringBuilder res = new StringBuilder();
        questionAnswerMap.forEach((Q, A) -> res.append(System.lineSeparator()).append(Q).append(System.lineSeparator()).append(A).append(System.lineSeparator()));
        result = res.toString();
        return result;
    }

    private Map<String, String> queryFAQWebService(String queryableMessage) throws IOException {
        URL url = generateQueryURL(queryableMessage);
        HttpURLConnection conn = getHttpURLConnection(url);
        Map<String, String> questionAnswersMap = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            logger.debug("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                logger.debug("Received raw reply from Server : {}", output);
                questionAnswersMap.putAll(parseResponse(output));
            }
            logger.debug("QuestionAnswersMap : {}", questionAnswersMap);
        } catch(IOException e){
            throw new RuntimeException("Unable to process request", e);
        } finally {
            conn.disconnect();
        }
        return questionAnswersMap;
    }

    private HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }
        return conn;
    }

    private void logUnansweredQuestion(String message) throws IOException {
        Files.write(Paths.get(unansweredQuestionFilePath), (System.lineSeparator() + new Date() + " " + message).getBytes(), CREATE, APPEND);
    }

    private URL generateQueryURL(String message) throws MalformedURLException {
        //TODO host and port should be extracted as part of properties file
        URL url= new URL("http://localhost:8983/solr/answer?q=" + message + "%3F&defType=qa&qa=true&qa.qf=doctitle&wt=json");
        logger.debug("Querying FAQ webservice using URL : {}", url);
        return url;
    }

    private String convertToQueryable(String message) {
        message = message.replaceAll(" ", "+");
        return message;
    }

    Map<String, String> parseResponse(String response) {
        String questionResponse = "response";
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(response);
        } catch (IOException e) {
            logger.error("Unparsable JSON response sent by webservice {}", response, e);
            throw new IllegalArgumentException("Invalid JSON response received from webservice", e);
        }

        Map<Integer, String> questionsMap = new HashMap<>();
        Map<Integer, String> answersMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            if (field.getKey().equals(questionResponse)) {
                Docs[] docs = getDocs(response, field);
                questionsMap = Stream.of(docs)
                        .collect(Collectors.toMap(p -> Integer.parseInt(p.getDocid()),
                                p -> "Question: " + p.getDoctitle()[0]));
                answersMap = Stream.of(docs)
                        .collect(Collectors.toMap(p -> Integer.parseInt(p.getDocid()),
                                p -> "Answer: " + p.getBody()[0]));

            }
        }

        return questionsMap.keySet().stream().collect(Collectors.toMap(questionsMap::get, answersMap::get));
    }

    private Docs[] getDocs(String response, Map.Entry<String, JsonNode> field) {
        try {
            return new ObjectMapper().readValue(field.getValue().get("docs").toString(), Docs[].class);
        } catch (IOException e) {
            logger.error("Unparsable JSON response sent by webservice {}", response, e);
            logger.error("Specific error while parsing {} : {}", field.getKey(), field.getValue().toString());
            throw new IllegalArgumentException("Invalid JSON response received from webservice", e);
        }
    }

    @Override
    public String getId() {
        return IBotsEnum.FAQ.toString();
    }
}
