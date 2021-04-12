package org.lambda3.text.simplification.discourse.runner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MatchedRule {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String text;
    private String rule;

    public MatchedRule(String text, String rule) {
      this.text = text;
      this.rule = rule;
    }

    public String getText() {
        return text;
    }


    public String getRule() {
        return rule;
    }
    
    public String prettyPrintJSON() throws JsonProcessingException {
		return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
	}

    public String serializeToJSON() throws JsonProcessingException {
		return MAPPER.writeValueAsString(this);
    }
    
    public String toString() {
        try {
          return prettyPrintJSON();
        } catch (Exception e) {
          return e.getMessage();
        }
    }
}