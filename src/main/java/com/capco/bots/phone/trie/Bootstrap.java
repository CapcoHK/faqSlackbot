package com.capco.bots.phone.trie;

import com.capco.bots.phone.data.PhoneEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vijayalaxmi on 18/4/2017.
 */
public class Bootstrap {

    private static List<PhoneEntry> phoneEntriesDictionary = new ArrayList<>();

    private static void populatePhoneEntries() {
        phoneEntriesDictionary.add(new PhoneEntry("Alan Lee", "1111111111"));
        phoneEntriesDictionary.add(new PhoneEntry("Alan Lau", "2222222222"));
        phoneEntriesDictionary.add(new PhoneEntry("Alex Lee", "3333333333"));
        phoneEntriesDictionary.add(new PhoneEntry("Alex Lau", "4444444444"));
        phoneEntriesDictionary.add(new PhoneEntry("Mary James", "5555555555"));
        phoneEntriesDictionary.add(new PhoneEntry("James Fox", "6666666666"));
        phoneEntriesDictionary.add(new PhoneEntry("Gigi Alice", "6666666666"));

    }

    public static void main(String args[]) {
        populatePhoneEntries();

        Trie trie = new Trie();
        for (PhoneEntry phoneEntry : phoneEntriesDictionary) {
            String[] firstLastNames = phoneEntry.getName().split(" ");
            for (String str : firstLastNames) {
                trie.insert(str.toLowerCase(), phoneEntry);
                System.out.println("trie :" + trie);
            }
        }


        System.out.println("===================");
        System.out.println("Search james: " + trie.search("james"));
        System.out.println("Search lee: " + trie.search("lee"));

        System.out.println("Lookup al:" + Arrays.toString(trie.lookUp("al").toArray()));
        System.out.println("Lookup jax:" + Arrays.toString(trie.lookUp("jax").toArray()));

    }
}
