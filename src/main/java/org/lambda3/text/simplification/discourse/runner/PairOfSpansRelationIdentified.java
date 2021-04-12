package org.lambda3.text.simplification.discourse.runner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PairOfSpansRelationIdentified {
	private static final ObjectMapper MAPPER = new ObjectMapper();

    private String textSpan1;
    private String textSpan2;
    private String relation;

    public PairOfSpansRelationIdentified(String textSpan1, String textSpan2, String relation) {
        this.textSpan1 = textSpan1;
        this.textSpan2 = textSpan2;
        this.relation = relation;
    }

    public String getTextSpan1() {
        return textSpan1;
    }

    public String getTextSpan2() {
        return textSpan2;
    }

    public String getRelation() {
        return relation;
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