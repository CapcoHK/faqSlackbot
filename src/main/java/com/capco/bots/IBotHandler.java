package com.capco.bots;

/**
 * Created by Sridhar on 4/25/2017.
 */
public interface IBotHandler {
    void init();

    String processMessage(String message);

    String getId();
}
