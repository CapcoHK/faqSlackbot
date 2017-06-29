package com.capco.bots.faq.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

/**
 * Created by vijayalaxmi on 28/6/2017.
 */
public class Docs {
    @JsonProperty("file")
    String file;

    @JsonProperty("docid")
    String docid;

    @JsonProperty("timestamp")
    String timestamp;

    @JsonProperty("body")
    String[] body;

    @JsonProperty("doctitle")
    String[] doctitle;

    @JsonProperty("score")
    String score;

    public Docs(){}

    public Docs(String file, String docid, String timestamp, String[] body, String[] doctitle) {
        this.file = file;
        this.docid = docid;
        this.timestamp = timestamp;
        this.body = body;
        this.doctitle = doctitle;
        this.score = score;
    }

    @Override
    public String toString() {
        return "Docs{" +
                "file='" + file + '\'' +
                ", docid='" + docid + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", body=" + Arrays.toString(body) +
                ", doctitle=" + Arrays.toString(doctitle) +
                ", score=" + score +
                '}';
    }

    public String getFile() {
        return file;
    }

    public String getDocid() {
        return docid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String[] getBody() {
        return body;
    }

    public String[] getDoctitle() {
        return doctitle;
    }

    public String getScore() {
        return score;
    }
}
