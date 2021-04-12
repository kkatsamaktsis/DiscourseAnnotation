package org.lambda3.text.simplification.discourse.utils.tagging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.lambda3.text.simplification.discourse.model.AnnotationContent;
import org.lambda3.text.simplification.discourse.model.MatchedRule;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.SentenceAnnotation;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.model.Leaf;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeExtractionUtils;
import org.lambda3.text.simplification.discourse.utils.words.WordsUtils;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;

public class TaggingUtils {

    public static List<TaggedWord> tagWords(List<Word> words, String tag) {
        return words.stream()
                    .map(word -> new TaggedWord(word.value(), tag)).collect(Collectors.toList());
    }

    public static String taggedWordsToString(List<TaggedWord> words) {
        return words.stream()
                    .map(word -> word.toString())
                    .reduce("", (w1, w2) -> w1 + " " + w2);
    }

    public static AnnotationContent convertSentenceAnnotationsToAnnotationContent(List<SentenceAnnotation> annotations) {
        // Create new AnnotationContent instance.
        AnnotationContent content = new AnnotationContent();

        if (annotations.size() == 0) 
            return content;
        
        List<String> sentenceWords = new ArrayList<String>();

        // Add list of sentence words
        for(TaggedWord word : annotations.get(0).getTaggedWords()) {
            content.getWords().add(word.word());
            sentenceWords.add(word.word());
        }

        // Create list of MatchedRules
        for(SentenceAnnotation annotation : annotations) {
            content.getMatchedRules().add(new MatchedRule(new ArrayList<String>(annotation.getRulesMatched()),
                                                          annotation.getTaggedWords().stream().map(tw -> tw.tag()).collect(Collectors.toList()),
                                                          sentenceWords
                                                         )
                                         );
        }

        return content;
    }

    public static boolean canMergeAnnotations(SentenceAnnotation annotation1, 
                                              SentenceAnnotation annotation2) {
        Set<Integer> intersection = new HashSet<Integer>(annotation1.getTaggedWordsIndeces());
        intersection.retainAll(annotation2.getTaggedWordsIndeces());
        
        return intersection.isEmpty();
    }

    // It is assummed that the two annotations are over the same sentences, i.e. the same tokens.
    public static SentenceAnnotation mergeAnnotations(SentenceAnnotation annotation1, 
                                           SentenceAnnotation annotation2) {
        Set<Integer> combinedTaggedWordsIndeces = new HashSet<Integer>(annotation1.getTaggedWordsIndeces());
        combinedTaggedWordsIndeces.addAll(annotation2.getTaggedWordsIndeces());

        List<TaggedWord> combinedTaggedWords = new ArrayList<TaggedWord>(annotation1.getTaggedWords());
        for (int index : annotation2.getTaggedWordsIndeces()) {
            combinedTaggedWords.set(index, 
                                    new TaggedWord(annotation2.getTaggedWords().get(index).word(),
                                                   annotation2.getTaggedWords().get(index).tag()));
        }

        List<String> combinedRules = new ArrayList<String>(annotation1.getRulesMatched());
        combinedRules.addAll(annotation2.getRulesMatched());

        return new SentenceAnnotation(combinedTaggedWords, combinedTaggedWordsIndeces, combinedRules);
    }

    public static SentenceAnnotation getEmptyAnnotation(Leaf leaf) {
        List<TaggedWord> taggedWords = TaggingUtils.tagWords(
            ParseTreeExtractionUtils.getContainingWords(leaf.getParseTree()), "[O]");
   
        return new SentenceAnnotation(taggedWords, new HashSet<Integer>(), new ArrayList<String>());
    }

    public static SentenceAnnotation getEmptyAnnotation(String sentence) {
		List<TaggedWord> taggedWords = TaggingUtils.tagWords(WordsUtils.splitIntoWords(sentence), "[O]");
   
        return new SentenceAnnotation(taggedWords, new HashSet<Integer>(), new ArrayList<String>());
	}

	public static List<SentenceAnnotation> mergeAnnotations(List<SentenceAnnotation> annotations) {
        List<SentenceAnnotation> res = new ArrayList<SentenceAnnotation>();

        boolean annotationsMerged = false;
        Set<Integer> annotationsAlreadyIncluded = new HashSet<Integer>();

        for (int annotation1Index = 0; annotation1Index < annotations.size(); annotation1Index++) {
            SentenceAnnotation annotation1 = annotations.get(annotation1Index);
            for (int annotation2Index = 0; annotation2Index < annotation1Index; annotation2Index++) {
                SentenceAnnotation annotation2 = annotations.get(annotation2Index);

                if (!annotationsAlreadyIncluded.contains(annotation1Index)
                    && !annotationsAlreadyIncluded.contains(annotation2Index)
                    && canMergeAnnotations(annotation1, annotation2)) {
                    SentenceAnnotation mergedAnnotation = mergeAnnotations(annotation1, annotation2);
                    res.add(mergedAnnotation);

                    annotationsMerged = true;
                    annotationsAlreadyIncluded.add(annotation1Index);
                    annotationsAlreadyIncluded.add(annotation2Index);
                }
            }
        }

        // Add remainign annotations that were not merged.
        for (int annotationIndex = 0; annotationIndex < annotations.size(); annotationIndex++) {
            SentenceAnnotation annotation = annotations.get(annotationIndex);
            if (!annotationsAlreadyIncluded.contains(annotationIndex)) {
                res.add(annotation);
            }
        }
        // System.out.println(res);

        if (annotationsMerged) {
            return mergeAnnotations(res);
        }
        else
            return res;
    }
    
}