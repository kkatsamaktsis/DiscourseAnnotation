package org.lambda3.text.simplification.discourse.model;

import java.util.ArrayList;
import java.util.List;

public class AnnotationContent extends Content {
    private List<MatchedRule> matchedRules;
	private List<String> words;

	// for deserialization
	public AnnotationContent() {
        this.matchedRules = new ArrayList<>();
        this.words = new ArrayList<>();
	}

    @Override
    public String toString() {
        return "AnnotationContent";
    }

    public List<MatchedRule> getMatchedRules() {
        return matchedRules;
    }

    public List<String> getWords() {
        return words;
    }
}