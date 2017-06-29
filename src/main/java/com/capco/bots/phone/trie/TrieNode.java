package com.capco.bots.phone.trie;

import com.capco.bots.phone.data.PhoneEntry;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by vijayalaxmi on 18/4/2017.
 */
public class TrieNode {

    TrieNode[] arr;
    boolean isEnd;
    LinkedList<PhoneEntry> phoneEntries;

    // Initialize your data structure here.
    public TrieNode() {
        this.arr = new TrieNode[26];
    }

    public LinkedList<PhoneEntry> getPhoneEntries() {
        return phoneEntries;
    }

    public void setPhoneEntries(LinkedList<PhoneEntry> phoneEntries) {
        this.phoneEntries = phoneEntries;
    }

    @Override
    public String toString() {
        return "TrieNode{" +
                "arr=" + Arrays.toString(arr) +
                ", isEnd=" + isEnd +
                '}';
    }
}

