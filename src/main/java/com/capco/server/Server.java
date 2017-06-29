package com.capco.server;

import com.capco.SlackBotMain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Sridhar on 4/12/2017.
 */
public class Server {
    private Selector iSelector = Selector.open();
    private ServerSocketChannel iServerSocketChannel = ServerSocketChannel.open();
    private String iHostname;
    private int iPort;
    private ITcpConnectionHandler iTcpHandler;
    private static Logger iLogger = LogManager.getLogger(SlackBotMain.class);


    public Server(String hostname, int port) throws IOException {
        iHostname = hostname;
        iPort = port;

    }

    public void init() throws IOException {
        iServerSocketChannel.bind(new InetSocketAddress(iHostname, iPort));
        iServerSocketChannel.configureBlocking(false);
        iServerSocketChannel.register(iSelector, SelectionKey.OP_ACCEPT);
    }

    public void waitForKey() throws IOException {
        boolean flag = true;


        while (flag) {
            iSelector.select();
            Set selectedKeys = iSelector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                if(key.isAcceptable()){
                   iTcpHandler.acceptTcpConnection(iSelector, iServerSocketChannel);
                }
                if (key.isReadable()) {
                    try {
                        iTcpHandler.readTcpData(key);
                    }catch (Exception o) {
                        if (iLogger.isDebugEnabled())
                        iLogger.debug(" Connection closed for [ " + key.channel() + " ] ");

                    }
                }
                it.remove();
            }

        }

    }

    public void addConnectionListner(ITcpConnectionHandler handler){
        iTcpHandler = handler;
    }


}