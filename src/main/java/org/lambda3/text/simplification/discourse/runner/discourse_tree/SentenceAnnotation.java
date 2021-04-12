package org.lambda3.text.simplification.discourse.runner.discourse_tree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.TaggedWord;

public class SentenceAnnotation {
    private final List<TaggedWord> taggedWords;
    private final Set<Integer> taggedWordsIndeces;
    private final List<String> rulesMatched;

    public SentenceAnnotation(List<TaggedWord> taggedWords, 
                             int taggedWordsStartIndex, 
                             int taggedWordsEndIndex,
                             List<String> rulesMatched) {
        this.taggedWords = taggedWords;
        this.taggedWordsIndeces = new HashSet<Integer>();
        this.rulesMatched = rulesMatched;

        for (int i = taggedWordsStartIndex; i <= taggedWordsEndIndex; i++)
            this.taggedWordsIndeces.add(i);
    }

    public SentenceAnnotation(List<TaggedWord> taggedWords,
                              Set<Integer> taggedWordIndeces,
                              List<String> rulesMatched) {
        this.taggedWords = taggedWords;
        this.taggedWordsIndeces = taggedWordIndeces;
        this.rulesMatched = rulesMatched;
    }

    public List<TaggedWord> getTaggedWords() {
        return taggedWords;
    }

    public Set<Integer> getTaggedWordsIndeces() {
        return taggedWordsIndeces;
    }

    public List<String> getRulesMatched() {
        return rulesMatched;
    }

    public String toString() {
        return getTaggedWords().toString();
    }

}