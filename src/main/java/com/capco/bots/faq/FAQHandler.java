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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.APPEND;

/**
 * Created by vijayalaxmi on 23/6/2017.
 */
public class FAQHandler implements IBotHandler {

    private static Logger iLogger = LogManager.getLogger(FAQHandler.class);

    private final String filepath;

    public String getFilepath() {
        return filepath;
    }

    public FAQHandler(String path) {
        iLogger.debug("FAQHandler(" + path + ")");
        filepath = path;
        //TODO code to be implemented
    }

    @Override
    public void init() {
        iLogger.debug("FAQHandler init");
        //TODO code to be implemented
    }

    @Override
    public String processMessage(String message) {
        iLogger.debug("Processing message :" + message);
        StringBuffer result = new StringBuffer();
        try {
            message = message.replaceAll(" ", "+");
            if (message.trim().toLowerCase().equals("hi") || message.trim().toLowerCase().equals("hello")) {
                return message.trim() + "! I am FAQBot and can help you find answers for FAQs related to Capco. Please enter your question or partial question with keywords. ";
            }
            String urlString = "http://localhost:8983/solr/answer?q=" + message + "%3F&defType=qa&qa=true&qa.qf=doctitle&wt=json";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            String output;
            Map<String, String> questionAnswersMap = new HashMap<>();
            Path file = Paths.get(getFilepath());
            iLogger.debug("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                iLogger.debug("Received raw reply from Server:"+output);
                Map<String, String> parseMap = parseResponse(output);
                iLogger.debug("parsed reply from server :"+parseMap);
                if(parseMap.isEmpty()) {
                    Files.write(file, (System.lineSeparator()+new Date()+" "+message).getBytes(), APPEND);
                    result.append("No answer found for your question. Please contact admin.");
                }else{
                    questionAnswersMap.putAll(parseMap);
                    questionAnswersMap.forEach((k, v) -> result.append(System.lineSeparator() + k).append(System.lineSeparator() + v).append(System.lineSeparator()));
                }
                iLogger.debug("QuestionAnswersMap :"+questionAnswersMap);
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            result.append("Unable to process :" + message);
            e.printStackTrace();
        } catch (IOException e) {
            result.append("Unable to process request:" + message);
            e.printStackTrace();
        }
        iLogger.debug("returning result :" + result);
        return result.toString();
    }

    public Map<String, String> parseResponse(String response) {
        try {
            String qaResponse = "qaResponse";
            String questionResponse = "response";
            JsonFactory factory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper(factory);
            JsonNode rootNode = mapper.readTree(response);

            Map<Integer, String> questionsMap = new HashMap<>();
            Map<Integer, String> answersMap = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                if (field.getKey().equals(questionResponse)) {
                    Docs[] docs = new ObjectMapper().readValue(field.getValue().get("docs").toString(), Docs[].class);
                    questionsMap = Stream.of(docs)
                            .collect(Collectors.toMap(p -> Integer.parseInt(p.getDocid()),
                                    p -> "Question: " + p.getDoctitle()[0]));
                    answersMap = Stream.of(docs)
                            .collect(Collectors.toMap(p -> Integer.parseInt(p.getDocid()),
                                    p -> "Answer: " + p.getBody()[0]));

                }
            }

            Map<String, String> qaMap = questionsMap.keySet().stream().collect(Collectors.toMap(questionsMap::get, answersMap::get));
            return qaMap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getId() {
        return IBotsEnum.FAQ.toString();
    }
}
