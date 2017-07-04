package com.capco;


import com.capco.bots.IBotHandler;
import com.capco.bots.IBotsEnum;
import com.capco.bots.faq.FAQHandler;
import com.capco.bots.phone.PhoneDirectory;
import com.capco.pool.ByteBufferPool;
import com.capco.server.ITcpConnectionHandler;
import com.capco.server.Server;
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

    private Server iServer;

    private Map<String, IBotHandler> iHandlerMap;

    private ByteBufferPool iPool = new ByteBufferPool();


    public static void main(String[] args) {

        if (args.length < 3) {
            iLogger.info("Usage : hostname port path");
            System.exit(0);

        }
        Integer port = Integer.parseInt(args[1]);
        String hostname = args[0];
        String phonePath = args[2];

        new SlackBotMain().start(hostname, port, phonePath);
    }

    private void start(String hostname, Integer port, String phonePath) {
        iHandlerMap = new HashMap<>();
        initializeMap(phonePath);
        startServer(hostname, port);

    }

    private void initializeMap(String filepath) {
        PhoneDirectory directory = new PhoneDirectory(filepath);
        iHandlerMap.put(directory.getId(), directory);

        FAQHandler faqHandler = new FAQHandler(filepath);
        iHandlerMap.put(faqHandler.getId(), faqHandler);
    }


    private void startServer(String hostname, Integer port) {
        try {
            iServer = new Server(hostname, port);
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
        int indexOfFirstSpace = input.indexOf(' ');
        String response = "Unable to identify the command " + input + " . Please type in phone name ";
        if (indexOfFirstSpace > 0) {
            String command = input.substring(0, indexOfFirstSpace);
            String data = input.substring(indexOfFirstSpace + 1);
            switch (IBotsEnum.valueOf(command.trim().toUpperCase())) {
                case PHONE: {
                    response = iHandlerMap.get(IBotsEnum.PHONE.toString()).processMessage(data);
                    break;
                }
                case FAQ: {
                    response = iHandlerMap.get(IBotsEnum.FAQ.toString()).processMessage(data);
                    break;
                }
                default:
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
