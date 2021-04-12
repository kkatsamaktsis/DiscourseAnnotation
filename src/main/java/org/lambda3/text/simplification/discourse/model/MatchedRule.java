package org.lambda3.text.simplification.discourse.model;

import java.util.ArrayList;
import java.util.List;

public class MatchedRule {
    private List<String> rules;
    private String description;
    private List<String> tags;
    private List<String> tagsIOB;

    // for deserialization
	public MatchedRule() {
        this.rules = new ArrayList<>();
        this.description = null;
        this.tags = new ArrayList<>();
        this.tagsIOB = new ArrayList<>();
    }
    
    public MatchedRule(List<String> rules, List<String> tags, List<String> words) {
        this.rules = rules;
        this.description = getDescriptionAndtagsIOB(rules, tags, words);
        this.tags = tags;
	}

    private String getDescriptionAndtagsIOB(List<String> givenRules, List<String> givenTags, List<String> words) {
        this.tagsIOB = new ArrayList<>();

        String res = "";
        String previousWordTag = "[O]";
        
        for (int i = 0; i < words.size(); i++) {
            String tag = givenTags.get(i);
            String tagNoBrackets = tag.substring(1, tag.length() - 1);
            String word = words.get(i);

            // No tag necessary
            if (tag.equals("[O]")) {
                
                // Previous chunk has ended.
                if (!previousWordTag.equals("[O]")) {
                    res += "] ";
                }
                
                res += word;
                if (!(i == words.size() - 1))
                    res += " "; // Add space before next word.
                
                this.tagsIOB.add("[O]");

            } else {  // Current word belongs to a tagged chunk 

                // Tagged span starts
                if (!previousWordTag.equals(tag)) { 
                
                    // Previous chunk has ended.
                    if (!previousWordTag.equals("[O]")) {
                        res += "] ";
                    }

                    res += "[" + tagNoBrackets + ": " + word;

                    // Chunk includes more than 1 tokens.
                    if (i <= words.size() - 2 && tag.equals(givenTags.get(i + 1))) {
                        this.tagsIOB.add("[B-" + tagNoBrackets + "]");
                        
                        // Add space before the next word.
                        res += " ";
                    } else {
                        this.tagsIOB.add("[I-" + tagNoBrackets + "]");
                    }

                    
                } else {
                    // Tagged span continues.
                    this.tagsIOB.add("[I-" + tagNoBrackets + "]");
                    res += word;

                    // Sequence ends, so close the square brackets
                    if (i == words.size() - 1)
                        res += "]";
                    else if (tag.equals(givenTags.get(i + 1))) // Add space before next word.
                        res += " ";
                }
            }

            previousWordTag = tag;
        }

        return res;
    }

    public List<String> getRules() {
        return rules;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getDescription() {
        return description;
    }

}
