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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.capco.util.StringUtil.replacePunctuations;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Handles FAQs
 * Created by vijayalaxmi on 23/6/2017.
 */
public class FAQHandler implements IBotHandler {

    private static Logger logger = LogManager.getLogger(FAQHandler.class);

    private final String unansweredQuestionFilePath;
    private final Set<String> stopWords;

    public FAQHandler(String unansweredQuestionFilePath, String stopWordsFilePath) {
        this.unansweredQuestionFilePath = unansweredQuestionFilePath;
        this.stopWords = new HashSet<>();
        try {
            if(stopWordsFilePath!=null && !stopWordsFilePath.isEmpty()) {
                logger.info("Reading stop words from path {}", stopWordsFilePath);
                Files.readAllLines(Paths.get(stopWordsFilePath)).forEach(s -> stopWords.add(s.trim().toLowerCase()));
                logger.info("These stop words will be ignored {}", stopWords);
            }
        } catch (IOException e) {
            logger.error("Unable to read stop words file at path {}", stopWordsFilePath, e);
        }
        logger.debug("Unanswered questions will be logged to : {}", unansweredQuestionFilePath);
    }

    @Override
    public void init() {

    }

    @Override
    public String processMessage(String user, String message) {
        logger.debug("User : {}, message : {}", user, message);
        if (message.trim().toLowerCase().equals("hi") || message.trim().toLowerCase().equals("hello")) {
            return message.trim() + " " + user + "! I am FAQBot and can help you find answers for FAQs related to Capco. Please enter your question or partial question with keywords. ";
        }
        String messageWithoutPunctuations = replacePunctuations(message);
        StringBuilder result = new StringBuilder();
        try {
            String queryableMessage = convertToQueryable(messageWithoutPunctuations);
            Map<String, String> questionAnswerMap = queryFAQWebService(queryableMessage);
            if (questionAnswerMap.isEmpty()) {
                logUnansweredQuestion(message, messageWithoutPunctuations);
                result.append("Couldn't find a perfect match for your query. We have stored your query and will look into it. ");
                questionAnswerMap = doApproximateSearch(messageWithoutPunctuations);
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
        Set<String> queryTerms = Arrays.stream(message.split(" ")).map(String::toLowerCase).filter(s -> !stopWords.contains(s)).collect(Collectors.toSet());
        logger.info("Following query terms will be used for approx search {}", queryTerms);
        CompletableFuture<Map<String, String>> cf = null;
        for (String qt : queryTerms) {
            if (cf == null) {
                cf = CompletableFuture.supplyAsync(() -> _queryFAQWebService(qt));
            } else {
                cf = cf.thenCombineAsync(CompletableFuture.supplyAsync(() -> _queryFAQWebService(qt)), (m1, m2)-> {m1.putAll(m2);return m1;});
            }
        }
        try {
            Map<String, String> possibleMatches = cf.get();
            return refineSearchResults(possibleMatches, queryTerms);
        } catch (InterruptedException|ExecutionException e) {
            logger.error("Exception while waiting for cumulative response", e);
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

    private Map<String, String> refineSearchResults(Map<String, String> possibleMatches, Set<String> queryTerms) {
        if(possibleMatches.isEmpty()){
            return possibleMatches;
        }
        Map<String, String> refinedSearchResult = new HashMap<>();
        for (String que : possibleMatches.keySet()) {
            String queLowerCase = replacePunctuations(que.toLowerCase());
            String[] queWords = queLowerCase.split(" ");
            boolean allQTFound = true;
            for (String queryTerm : queryTerms) {
                boolean thisQTFound = false;
                for (String queWord : queWords) {
                    if (queWord.startsWith(queryTerm) || queryTerm.startsWith(queWord)) {
                        thisQTFound = true;
                        break;
                    }
                }
                allQTFound = allQTFound && thisQTFound;
            }
            if (allQTFound) {
                refinedSearchResult.put(que, possibleMatches.get(que));
            }
        }
        return refinedSearchResult.isEmpty() ? possibleMatches : refinedSearchResult;
    }

    private String convertToString(Map<String, String> questionAnswerMap) {
        StringBuilder res = new StringBuilder();
        questionAnswerMap.forEach((Q, A) -> res.append(System.lineSeparator()).append(Q).append(System.lineSeparator()).append(A).append(System.lineSeparator()));
        return res.toString();
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

    private void logUnansweredQuestion(String message, String messageWithoutPuncuations) throws IOException {
        String searchedKeywords = String.join(",", Arrays.stream(messageWithoutPuncuations.split(" ")).filter(s -> !stopWords.contains(s)).collect(Collectors.toSet()));
        Files.write(Paths.get(unansweredQuestionFilePath), (System.lineSeparator() + new Date() + "\t" + message + "\t" + searchedKeywords).getBytes(), CREATE, APPEND);
    }

    private URL generateQueryURL(String message) throws MalformedURLException {
        //TODO host and port should be extracted as part of properties file
        URL url= new URL("http://localhost:8983/solr/answer?q=" + message + "%3F&defType=qa&qa=true&qa.qf=doctitle&wt=json");
        logger.debug("Querying FAQ webservice using URL : {}", url);
        return url;
    }

    private String convertToQueryable(String message) {
        return message.replaceAll(" ", "+");
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
