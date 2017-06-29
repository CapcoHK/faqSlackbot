package com.capco.bots.phone.trie;

import com.capco.bots.phone.data.PhoneEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Created by vijayalaxmi on 18/4/2017.
 */
public class Trie {

    private static Logger iLogger = LogManager.getLogger(Trie.class);

    private static char[] alphabet = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    private TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    // Inserts a word into the trie.
    public void insert(String word, PhoneEntry phoneEntry) {
        if (iLogger.isTraceEnabled())
            iLogger.trace("Word :" + word);

        TrieNode p = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            int index = c - 'a';
            if (iLogger.isTraceEnabled())
                iLogger.trace("char: " + c + " ,index: " + index);
            if (p.arr[index] == null) {
                TrieNode temp = new TrieNode();
                p.arr[index] = temp;
                p = temp;
            } else {
                p = p.arr[index];
            }
        }
        p.isEnd = true;
        if (p.getPhoneEntries() == null) {
            p.setPhoneEntries(new LinkedList<>());
        }
        p.getPhoneEntries().add(phoneEntry);
    }

    // Returns if the word is in the trie.
    public LinkedList<PhoneEntry> search(String word) {
        TrieNode p = searchNode(word);
        if (p == null) {
            return null;
        } else {
            if (p.isEnd)
                return p.getPhoneEntries();
        }

        return null;
    }

    public static String removeLastChar(String str) {
        return str.substring(0, str.length() - 1);
    }

    public Set<PhoneEntry> lookUp(String word) {
        Set<PhoneEntry> result = lookUpName(word);
        if (result == null) {
            if (word.length() > 1) {
                String newSearch = removeLastChar(word);
                if (iLogger.isDebugEnabled())
                    iLogger.debug("No results found for " + word + ". New search will be done for " + newSearch);
                result = lookUpName(newSearch);
            }
        }
        return result;
    }

    // Returns if the word is in the trie.
    public Set<PhoneEntry> lookUpName(String word) {
        TrieNode root = searchNode(word);
        if (iLogger.isTraceEnabled())
            iLogger.trace("Root node :" + root);
        Set<PhoneEntry> phoneEntrySet = new HashSet<>();
        if (root == null) {
            return null;
        }
        if (root.isEnd) {
            if (iLogger.isTraceEnabled())
                iLogger.trace("root is end node");
            phoneEntrySet.addAll(root.phoneEntries);

        } //else {
        lookUpNextChar(word, word, root, phoneEntrySet);
        return phoneEntrySet;
    }

    private String lookUpNextChar(String word, String temp, TrieNode trieNode, Set<PhoneEntry> phoneEntriesHashSet) {
        for (int i = 0; i < trieNode.arr.length; i++) {
            if (trieNode.arr[i] != null) {
                temp = word + (alphabet[i]);
                if (!trieNode.arr[i].isEnd) {
                    temp = lookUpNextChar(temp, temp, trieNode.arr[i], phoneEntriesHashSet);
                }


                if (trieNode.arr[i].isEnd) {
                    phoneEntriesHashSet.addAll(trieNode.arr[i].phoneEntries);
                    temp = lookUpNextChar(temp, temp, trieNode.arr[i], phoneEntriesHashSet);
                }
            }

        }
        return temp;
    }

    // Returns if there is any word in the trie
    // that starts with the given prefix.
    public boolean startsWith(String prefix) {
        TrieNode p = searchNode(prefix);
        if (p == null) {
            return false;
        } else {
            return true;
        }
    }


    public TrieNode searchNode(String s) {
        TrieNode p = root;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int index = c - 'a';
            if (p.arr[index] != null) {
                p = p.arr[index];
            } else {
                return null;
            }
        }

        if (p == root)
            return null;

        return p;
    }

    @Override
    public String toString() {
        return "Trie{" +
                "root=" + root +
                '}';
    }
}
