package com.capco.bots;

/**
 * Created by Sridhar on 4/25/2017.
 */
public interface IBotHandler {
    void init();
//Testing By AW
    String processMessage(String user, String message);

    String getId();
}
