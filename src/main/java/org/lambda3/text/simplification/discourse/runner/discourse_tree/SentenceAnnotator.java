package org.lambda3.text.simplification.discourse.runner.discourse_tree;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

import org.lambda3.text.simplification.discourse.model.AnnotationContent;
import org.lambda3.text.simplification.discourse.model.MatchedRule;
import org.lambda3.text.simplification.discourse.processing.ProcessingType;
import org.lambda3.text.simplification.discourse.processing.SentencePreprocessor;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.extraction.Extraction;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.extraction.ExtractionRule;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.model.DiscourseTree;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.model.Leaf;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.model.SentenceLeaf;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeException;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeExtractionUtils;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeVisualizer;
import org.lambda3.text.simplification.discourse.utils.sentences.SentencesUtils;
import org.lambda3.text.simplification.discourse.utils.tagging.TaggingUtils;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.Word;

import org.slf4j.Logger;

public class SentenceAnnotator {
    private final Config config;
    private final SentencePreprocessor preprocessor;
    private final List<ExtractionRule> rules;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private List<List<SentenceAnnotation>> annotatedSentences;

    public SentenceAnnotator(Config config) {
        SentencePreprocessor preprocessor = new SentencePreprocessor(config);

        this.config = config;
        this.preprocessor = preprocessor;

        // create rules from config
        this.rules = new ArrayList<>();
        for (String className : this.config.getStringList("rules")) {
            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> constructor = clazz.getConstructor();
                ExtractionRule rule = (ExtractionRule) constructor.newInstance();
                rule.setConfig(config);
                rules.add(rule);
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                logger.error("{}", e);
                logger.error("Failed to create instance of {}", className);
                throw new ConfigException.BadValue("rules." + className, "Failed to create instance.");
            }
        }
    }

    public SentenceAnnotator() {
        this(ConfigFactory.load().getConfig("discourse-simplification"));
    }

    public List<AnnotationContent> doDiscourseAnnotation(File file, ProcessingType type, boolean separateLines) throws IOException {
        List<String> sentences = SentencesUtils.splitIntoSentencesFromFile(file, separateLines);
        
        return doDiscourseAnnotation(sentences, type);
    }

    public List<AnnotationContent> doDiscourseAnnotation(List<String> sentences, ProcessingType type) {
        if (type.equals(ProcessingType.SEPARATE)) {
            return processSeparate(sentences);
        } else if (type.equals(ProcessingType.WHOLE)) {
            return processWhole(sentences);
        } else {
            throw new IllegalArgumentException("Unknown ProcessingType.");
        }
    }

    private List<AnnotationContent> processWhole(List<String> sentences) {
        // TODO Implementation.
        return null;
    }

    // creates discourse trees for each individual sentence (investigates
    // intra-sentential relations only)
    private List<AnnotationContent> processSeparate(List<String> sentences) {
        List<AnnotationContent> annotations = new ArrayList<AnnotationContent>();

        int idx = 0;
        for (String sentence : sentences) {
            logger.info("# Processing sentence " + (idx + 1) + "/" + sentences.size() + " #");
            logger.info("'" + sentence + "'\n");

            // Step 1) create sentence discourse tree
            logger.info("### Step 1) ANNOTATE SENTENCE ###");
            
            try {                
                List<SentenceAnnotation> sentenceAnnotations = annotateSentence(sentence, idx);

                AnnotationContent newAnnotationContent = TaggingUtils.convertSentenceAnnotationsToAnnotationContent(sentenceAnnotations);
                for (MatchedRule matchedRule : newAnnotationContent.getMatchedRules()) {
                    logger.info("Annotation generated: ");
                    logger.info(matchedRule.getDescription() + "\n");
                }

                annotations.add(newAnnotationContent);
                
                if (annotations.size() != idx + 1) {
                    logger.error("The annotation for some given sentence was not created.");
                    System.exit(-1);
                }

            } catch (ParseTreeException e) {
                logger.error("Failed to process sentence: {}", sentence);

                List<SentenceAnnotation> sentenceAnnotations = new ArrayList<SentenceAnnotation>();
                sentenceAnnotations.add(TaggingUtils.getEmptyAnnotation(sentence));
                AnnotationContent newAnnotationContent = TaggingUtils.convertSentenceAnnotationsToAnnotationContent(sentenceAnnotations);
                annotations.add(newAnnotationContent);
            }

            ++idx;
        }

        logger.info("### FINISHED");
        
        return annotations;
    }


    public List<SentenceAnnotation> annotateSentence(String sentence, int sentenceIdx) throws ParseTreeException {
        String preprocessedSentence = preprocessor.preprocessSentence(sentence);
        SentenceLeaf sentenceLeaf = new SentenceLeaf(preprocessedSentence, sentenceIdx); 
        
        // List<Word> sentence_words = ParseTreeExtractionUtils.getContainingWords(sentenceLeaf.getParseTree()); 
        // 1. Get all annotations, based on the discourse simplification rules, for this sentence.
        List<SentenceAnnotation> annotations = applyAllRules(sentenceLeaf);

        // 2. Merge non conflicting annotations together, to create a smaller set of them.
        annotations = TaggingUtils.mergeAnnotations(annotations);

        // 3. Return this set of annotations.
        return annotations;
    }

    private List<SentenceAnnotation> applyAllRules(Leaf leaf) {
        List<SentenceAnnotation> newAnnotations = new ArrayList<SentenceAnnotation>();

        newAnnotations.add(TaggingUtils.getEmptyAnnotation(leaf));

        logger.debug("Processing leaf:");
        if (logger.isDebugEnabled()) {
            logger.debug(leaf.toString());
        }

        if (!leaf.isAllowSplit()) {
            logger.debug("Leaf will not be check.");
            return newAnnotations;
        }

        logger.debug("Process leaf:");
        if (logger.isDebugEnabled()) {
            logger.debug(ParseTreeVisualizer.prettyPrint(leaf.getParseTree()));
        }

        // check rules
        for (ExtractionRule rule : rules) {

            Optional<Extraction> extraction = null;
            try {
                extraction = rule.extract(leaf);
            } catch (ParseTreeException e) {
                continue;
            }

            if (extraction.isPresent()) {
                logger.debug("Extraction rule " + rule.getClass().getSimpleName() + " matched.");

                // handle extractions
                Optional<DiscourseTree> r = extraction.get().generate(leaf);
                if (r.isPresent()) {
                    newAnnotations.addAll(extraction.get().getAnnotations());
                } else {
                    logger.debug("Reference could not be used, checking other model rules.");
                }
            }
        }

        // logger.debug("No model rule applied.");

        return newAnnotations;
    }
}
