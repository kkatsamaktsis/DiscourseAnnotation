package org.lambda3.text.simplification.discourse.runner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RelationIdentified {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String text;
    private String relation;

    public RelationIdentified(String text, String relation) {
        this.text = text;
        this.relation = relation;
    }

    public String getText() {
        return text;
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