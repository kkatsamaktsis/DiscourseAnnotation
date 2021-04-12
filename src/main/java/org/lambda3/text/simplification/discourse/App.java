/*
 * ==========================License-Start=============================
 * DiscourseSimplification : App
 *
 * Copyright © 2017 Lambda³
 *
 * GNU General Public License 3
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 * ==========================License-End==============================
 */

package org.lambda3.text.simplification.discourse;

import org.lambda3.text.simplification.discourse.processing.DiscourseSimplifier;
import org.lambda3.text.simplification.discourse.processing.ProcessingType;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.SentenceAnnotation;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.SentenceAnnotator;
import org.lambda3.text.simplification.discourse.model.AnnotationContent;
import org.lambda3.text.simplification.discourse.model.MatchedRule;
import org.lambda3.text.simplification.discourse.model.SimplificationContent;
import org.lambda3.text.simplification.discourse.utils.ner.NERStringParser;
import org.lambda3.text.simplification.discourse.utils.tagging.TaggingUtils;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.TaggedWord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static void saveLines(File file, List<String> lines) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(lines.stream().collect(Collectors.joining("\n")));

            // no need to close it.
            // bw.close()
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveJSONLines(File file, List<String> lines) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("[\n");
            bw.write(lines.stream().collect(Collectors.joining(",\n")));
            bw.write("\n]\n");
            // no need to close it.
            // bw.close()
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveRelationsData(String corpusFile, DiscourseSimplifier simplifier, SimplificationContent content) {
        String outputFilesPrefix;
        if (corpusFile.length() > 4) // If the given file is .txt
            outputFilesPrefix = corpusFile.substring(0, corpusFile.length() - 4); 
        else
            outputFilesPrefix = corpusFile;
        
        // Save the tsv file containing sentences and rhetorical relations in them.
        saveLines(new File("relations_" + outputFilesPrefix + ".tsv"), 
                  simplifier.getDiscourseTreeCreator().getRelationsIdentified());
        saveLines(new File("pairs_of_sentences_relations_" + outputFilesPrefix + ".tsv"), 
                  simplifier.getDiscourseTreeCreator().getPairOfSpansRelationsIdentified());
        saveLines(new File("rules_matched_" + outputFilesPrefix + ".tsv"), 
                  simplifier.getDiscourseTreeCreator().getRulesMatched());

        saveLines(new File("relations_" + outputFilesPrefix + ".json"), 
                  Arrays.asList(simplifier.getDiscourseTreeCreator().getRelationsIdentifiedObj().toString()));
        saveLines(new File("pairs_of_sentences_relations_" + outputFilesPrefix + ".json"), 
                  Arrays.asList(simplifier.getDiscourseTreeCreator().getPairOfSpansRelationsIdentifiedObj().toString()));
        saveLines(new File("rules_matched_" + outputFilesPrefix + ".json"), 
                  Arrays.asList(simplifier.getDiscourseTreeCreator().getRulesMatchedObj().toString()));

        // Save simplified text - in simple text format not RTF
        saveLines(new File(outputFilesPrefix + "_simplified" + ".txt"), 
                  Arrays.asList(content.getSimplifiedSentencesAsSimpleText()));
    }

    private static void performSimplification(String corpusFile, boolean toSaveRelationsData) throws IOException {
        LOGGER.info("=======================================================================");
        LOGGER.info("Performing Discourse Simplification for file: " + corpusFile);
        LOGGER.info("=======================================================================");

        // Create fresh discourse simplifier (and DiscourseTreeCreator)
        DiscourseSimplifier DISCOURSE_SIMPLIFIER = new DiscourseSimplifier();
        SimplificationContent content = DISCOURSE_SIMPLIFIER.doDiscourseSimplification(new File(corpusFile),
                                                                                       ProcessingType.SEPARATE, 
                                                                                       true);
        content.serializeToJSON(new File("output.json"));
        saveLines(new File("output_default.txt"), Arrays.asList(content.defaultFormat(false)));
        saveLines(new File("output_flat.txt"), Arrays.asList(content.flatFormat(false)));

        if (toSaveRelationsData)
            saveRelationsData(corpusFile, DISCOURSE_SIMPLIFIER, content);

        LOGGER.info("done");
    }


    private static void performAnnotation(String corpusFile) throws IOException {
        LOGGER.info("=======================================================================");
        LOGGER.info("Performing Discourse Annotation for file: " + corpusFile);
        LOGGER.info("=======================================================================");

        // Create fresh discourse annotator (and DiscourseTreeCreator)
        SentenceAnnotator annotator = new SentenceAnnotator();
        List<AnnotationContent> annotations = annotator.doDiscourseAnnotation(new File(corpusFile),
                                                                              ProcessingType.SEPARATE, 
                                                                              true);

        String outputFilesPrefix;
        if (corpusFile.length() > 4) // If the given file is .txt
            outputFilesPrefix = corpusFile.substring(0, corpusFile.length() - 4); 
        else
            outputFilesPrefix = corpusFile;
        
        saveJSONLines(new File(outputFilesPrefix + "_annotated.json"), 
                      annotations.stream().map(annotation -> {
                                                   try {
                                                       return annotation.serializeToJSON();
                                                   } catch (JsonProcessingException e) {
                                                       // TODO Auto-generated catch block
                                                       e.printStackTrace();
                                                       return "JsonProcessingException";
                                                   }
                                               }).collect(Collectors.toList()));

        LOGGER.info("-----------------------------------------------------------------------");

        for(AnnotationContent annotation : annotations) {
            for(MatchedRule result : annotation.getMatchedRules()) {
                LOGGER.info("Sentence annotation: ");
                LOGGER.info(result.getDescription());
            }
            LOGGER.info("-----------------------------------------------------------------------");
        }

        LOGGER.info("done");
    }

    public static void main(String[] args) throws IOException {
        performSimplification("input.txt", true);
        performAnnotation("input.txt");
    }
}
