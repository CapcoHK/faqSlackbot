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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by vijayalaxmi on 23/6/2017.
 */
public class FAQHandler implements IBotHandler {

    private static Logger iLogger = LogManager.getLogger(FAQHandler.class);

    public FAQHandler(String path) {
        iLogger.debug("FAQHandler(" + path + ")");
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
            String urlString = "http://localhost:8983/solr/answer?q=" + message + "%3F&defType=qa&qa=true&qa.qf=body&wt=json";
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
            iLogger.debug("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                questionAnswersMap.putAll(parseResponse(output));
                questionAnswersMap.forEach((k, v) -> result.append(System.lineSeparator() + k).append(System.lineSeparator() + v));
                iLogger.debug(questionAnswersMap);
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
            HashMap<Integer, String> answersMap = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                if (field.getKey().equals(questionResponse)) {
                    Docs[] docs = new ObjectMapper().readValue(field.getValue().get("docs").toString(), Docs[].class);
                    questionsMap = Stream.of(docs)
                            .collect(Collectors.toMap(p -> Integer.parseInt(p.getDocid()),
                                    p -> "Question: " + p.getDoctitle()[0]));
                }
                if (field.getKey().equals(qaResponse)) {
                    for (int i = 1; i < field.getValue().size(); i += 2) {
                        answersMap.put(Integer.parseInt(field.getValue().get(i).get(1).textValue()),
                                "Answer: " + field.getValue().get(i).get(5).textValue());
                    }
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
