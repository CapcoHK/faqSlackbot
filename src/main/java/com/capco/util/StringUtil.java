package com.capco.util;

/**
 * Created by Bhushan on 7/19/2017.
 */
public class StringUtil {
    public static String replacePunctuations(String s) {
        return s.replaceAll("[^a-zA-Z-/0-9 ]", "");
    }
}
