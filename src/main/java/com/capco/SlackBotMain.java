package com.capco;


import com.capco.bots.IBotHandler;
import com.capco.bots.IBotsEnum;
import com.capco.bots.faq.FAQHandler;
import com.capco.bots.phone.PhoneDirectory;
import com.capco.pool.ByteBufferPool;
import com.capco.server.ITcpConnectionHandler;
import com.capco.server.Server;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sridhar on 4/12/2017.
 */
public class SlackBotMain implements ITcpConnectionHandler {

    private static Logger iLogger = LogManager.getLogger(SlackBotMain.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, IBotHandler> iHandlerMap;

    private ByteBufferPool iPool = new ByteBufferPool();

//testing

    public static void main(String[] args) {

        if (args.length < 3) {
            iLogger.info("Usage : hostname port path");
            System.exit(0);

        }
        String hostname = args[0];
        Integer port = Integer.parseInt(args[1]);
        String phonePath = args[2];
        String questionStatsFilePath = "questionStats.xlsx";
        if (args.length > 3) {
            if (args[3] != null && !args[3].isEmpty()) {
                questionStatsFilePath = args[3];
            }
        }
        String stopWordsFile = null;
        if (args.length > 4) {
            if (args[4] != null && !args[4].isEmpty()) {
                stopWordsFile = args[4];
            }
        }
        new SlackBotMain().start(hostname, port, phonePath, questionStatsFilePath, stopWordsFile);
    }

    private void start(String hostname, Integer port, String phonePath, String questionStatsFilePath, String stopWordsFile) {
        iHandlerMap = new HashMap<>();
        initializeMap(phonePath, questionStatsFilePath, stopWordsFile);
        startServer(hostname, port);
    }

    private void initializeMap(String filepath, String questionStatsFilePath, String stopWordsFilePath) {
        PhoneDirectory directory = new PhoneDirectory(filepath);
        iHandlerMap.put(directory.getId(), directory);

        FAQHandler faqHandler = new FAQHandler(questionStatsFilePath, stopWordsFilePath);
        iHandlerMap.put(faqHandler.getId(), faqHandler);
    }


    private void startServer(String hostname, Integer port) {
        try {
            Server iServer = new Server(hostname, port);
            iServer.init();
            iServer.addConnectionListner(this);
            iServer.waitForKey();
        } catch (IOException e) {
            iLogger.error(e);
        }
    }

    @Override
    public void readTcpData(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = iPool.getNextByteBufferFree();
        try {
            client.read(byteBuffer); //lex
        } catch (IOException e) {
            client.close();
        }
        String input = new String(Arrays.copyOfRange(byteBuffer.array(), 0, byteBuffer.position()));
        Map<String, String> request = objectMapper.readValue(input, new TypeReference<Map<String, String>>(){});
        String response;
        if (request.size() != 3) {
            response = "Missing inputs, expected : 'username', 'botname', 'command', found : " + request;
        } else {
            String user = request.get("username");
            String botname = request.get("botname");
            String data = request.get("command");
            switch (IBotsEnum.valueOf(botname.trim().toUpperCase())) {
                case PHONE: {
                    response = iHandlerMap.get(IBotsEnum.PHONE.toString()).processMessage(user, data);
                    break;
                }
                case FAQ: {
                    response = iHandlerMap.get(IBotsEnum.FAQ.toString()).processMessage(user, data);
                    break;
                }
                default:
                    response = "Unknown bot";
            }
        }
        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
        client.write(buffer);
        client.close();
    }

    @Override
    public void acceptTcpConnection(Selector selector, ServerSocketChannel channel) throws IOException {
        SocketChannel client = channel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        if (iLogger.isTraceEnabled())
            iLogger.trace("Connection accepted from [ " + client + " ] ");
    }
}
