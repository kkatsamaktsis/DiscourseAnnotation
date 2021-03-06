/*
 * ==========================License-Start=============================
 * DiscourseSimplification : SubordinationPreExtractor
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

package org.lambda3.text.simplification.discourse.runner.discourse_tree.extraction.rules;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.Relation;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.SentenceAnnotation;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.extraction.Extraction;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.extraction.ExtractionRule;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.model.Leaf;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeException;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeExtractionUtils;
import org.lambda3.text.simplification.discourse.utils.tagging.TaggingUtils;
import org.lambda3.text.simplification.discourse.utils.words.WordsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class SubordinationPreExtractor extends ExtractionRule {

    @Override
    public Optional<Extraction> extract(Leaf leaf) throws ParseTreeException {
        TregexPattern p = TregexPattern.compile("ROOT <<: (S < (SBAR=sbar < (S=s < (NP $.. VP)) $.. (NP $.. VP)))");
        TregexMatcher matcher = p.matcher(leaf.getParseTree());

        while (matcher.findAt(leaf.getParseTree())) {

            // the left, subordinate constituent
            List<Word> leftConstituentWords = ParseTreeExtractionUtils.getContainingWords(matcher.getNode("s"));
            Leaf leftConstituent = new Leaf(getClass().getSimpleName(), WordsUtils.wordsToProperSentenceString(leftConstituentWords));

            List<SentenceAnnotation> annotations = new ArrayList<SentenceAnnotation>();
            List<TaggedWord> taggedWords = TaggingUtils.tagWords(ParseTreeExtractionUtils.getPrecedingWords(leaf.getParseTree(), 
                                                                                                            matcher.getNode("s"), false), 
                                                                                                            "[O]");
            int taggedWordsStartIndex = taggedWords.size();
            taggedWords.addAll(TaggingUtils.tagWords(ParseTreeExtractionUtils.getContainingWords(matcher.getNode("s")), "[SUB-PRE]"));
            int taggedWordsEndIndex = taggedWords.size() - 1;
            taggedWords.addAll(TaggingUtils.tagWords(ParseTreeExtractionUtils.getFollowingWords(leaf.getParseTree(), matcher.getNode("s"), 
                                                                                                false), "[O]"));

            annotations.add(new SentenceAnnotation(taggedWords, taggedWordsStartIndex, taggedWordsEndIndex, 
                                                   Arrays.asList("SubordinationPreExtractor")));

            // the right, superordinate constituent
            List<Word> rightConstituentWords = new ArrayList<>();
            rightConstituentWords.addAll(ParseTreeExtractionUtils.getPrecedingWords(leaf.getParseTree(), matcher.getNode("sbar"), false));
            rightConstituentWords.addAll(ParseTreeExtractionUtils.getFollowingWords(leaf.getParseTree(), matcher.getNode("sbar"), false));
            Leaf rightConstituent = new Leaf(getClass().getSimpleName(), WordsUtils.wordsToProperSentenceString(rightConstituentWords));

            // relation
            List<Word> cuePhraseWords = ParseTreeExtractionUtils.getPrecedingWords(matcher.getNode("sbar"), matcher.getNode("s"), false);
            Relation relation = classifer.classifySubordinating(cuePhraseWords).orElse(Relation.UNKNOWN_SUBORDINATION);

            Extraction res = new Extraction(
                getClass().getSimpleName(),
                false,
                cuePhraseWords,
                relation,
                false,
                Arrays.asList(leftConstituent, rightConstituent)
            );

            // Add annotations
            res.getAnnotations().addAll(annotations);

            return Optional.of(res);
        }

        return Optional.empty();
    }
}
