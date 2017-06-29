package com.capco.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * Created by Sridhar on 4/12/2017.
 */
public interface ITcpConnectionHandler {
    void readTcpData(SelectionKey key) throws IOException;
    void acceptTcpConnection(Selector selector, ServerSocketChannel channel) throws IOException;
}
