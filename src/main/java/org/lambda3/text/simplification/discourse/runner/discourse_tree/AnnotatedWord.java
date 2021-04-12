package org.lambda3.text.simplification.discourse.runner.discourse_tree;

public class AnnotatedWord {
    private final String word;
    private final String tag;

    public AnnotatedWord(String word, String tag) {
        this.word = word;
        this.tag = tag;
    }

    public String getWord() {
        return word;
    }

    public String getTag() {
        return tag;
    }

    public String toString() {
        return "[" + getTag() + ": " + getWord() + "]";
    }

}