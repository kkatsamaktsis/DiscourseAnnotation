/*
 * ==========================License-Start=============================
 * DiscourseSimplification : SubordinationPostISAExtractor2
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
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 *
 */
public class PreAttributionExtractor extends ExtractionRule {

    @Override
    public Optional<Extraction> extract(Leaf leaf) throws ParseTreeException {
        TregexPattern p = TregexPattern.compile("ROOT <<: (S < (S|SBAR|SBARQ=s $.. (NP=np [$,, VP=vpb | $.. VP=vpa])))");
        TregexMatcher matcher = p.matcher(leaf.getParseTree());

        while (matcher.findAt(leaf.getParseTree())) {

            // the left, !superordinate! constituent
            List<Word> leftConstituentWords = ParseTreeExtractionUtils.getContainingWords(matcher.getNode("s"));
            Leaf leftConstituent = new Leaf(getClass().getSimpleName(), WordsUtils.wordsToProperSentenceString(leftConstituentWords));

            // the right, !subordinate! constituent
            List<Word> rightConstituentWords = new ArrayList<>();
            rightConstituentWords.addAll(ParseTreeExtractionUtils.getPrecedingWords(leaf.getParseTree(), matcher.getNode("s"), false));
            rightConstituentWords.addAll(ParseTreeExtractionUtils.getContainingWords(matcher.getNode("np")));
            
            List<SentenceAnnotation> annotations = new ArrayList<SentenceAnnotation>();
            List<TaggedWord> taggedWords = TaggingUtils.tagWords(ParseTreeExtractionUtils.getPrecedingWords(leaf.getParseTree(), matcher.getNode("np"), false), "[O]");
            int taggedWordsStartIndex = taggedWords.size();
            int taggedWordsEndIndex;
            taggedWords.addAll(TaggingUtils.tagWords(ParseTreeExtractionUtils.getContainingWords(matcher.getNode("np")), "[PRE-ATTRB]"));
            
            if (matcher.getNode("vpb") != null) {
                taggedWords.addAll(TaggingUtils.tagWords(ParseTreeExtractionUtils.getWordsInBetween(leaf.getParseTree(), matcher.getNode("np"), matcher.getNode("vpb"), false, false), "[PRE-ATTRB]"));

                rightConstituentWords.addAll(ParseTreeExtractionUtils.getContainingWords(matcher.getNode("vpb")));
                rightConstituentWords.addAll(ParseTreeExtractionUtils.getFollowingWords(leaf.getParseTree(), matcher.getNode("np"), false));
                taggedWords.addAll(TaggingUtils.tagWords(ParseTreeExtractionUtils.getContainingWords(matcher.getNode("vpb")), "[PRE-ATTRB]"));
                taggedWordsEndIndex = taggedWords.size() - 1;
                taggedWords.addAll(TaggingUtils.tagWords(ParseTreeExtractionUtils.getFollowingWords(leaf.getParseTree(), matcher.getNode("vpb"), false), "[O]"));
            } else {
                taggedWords.addAll(TaggingUtils.tagWords(ParseTreeExtractionUtils.getWordsInBetween(leaf.getParseTree(), matcher.getNode("np"), matcher.getNode("vpa"), false, false), "[PRE-ATTRB]"));

                rightConstituentWords.addAll(ParseTreeExtractionUtils.getContainingWords(matcher.getNode("vpa")));
                rightConstituentWords.addAll(ParseTreeExtractionUtils.getFollowingWords(leaf.getParseTree(), matcher.getNode("vpa"), false));
                taggedWords.addAll(TaggingUtils.tagWords(ParseTreeExtractionUtils.getContainingWords(matcher.getNode("vpa")), "[PRE-ATTRB]"));
                taggedWordsEndIndex = taggedWords.size() - 1;
                taggedWords.addAll(TaggingUtils.tagWords(ParseTreeExtractionUtils.getFollowingWords(leaf.getParseTree(), matcher.getNode("vpa"), false), "[O]"));
            }

            // rephrase
            rightConstituentWords = rephraseIntraSententialAttribution(rightConstituentWords);
            Leaf rightConstituent = new Leaf(getClass().getSimpleName(), WordsUtils.wordsToProperSentenceString(rightConstituentWords));
            rightConstituent.dontAllowSplit();
            rightConstituent.setToSimpleContext(true);

            annotations.add(new SentenceAnnotation(taggedWords, taggedWordsStartIndex, taggedWordsEndIndex, Arrays.asList("PreAttributionExtractor")));

            // relation
            Optional<Word> headVerb;
            if (matcher.getNode("vpb") != null) {
                headVerb = getHeadVerb(matcher.getNode("vpb"));
            } else {
                headVerb = getHeadVerb(matcher.getNode("vpa"));
            }

            // only extract if verb matches
            if (headVerb.isPresent() && classifer.checkAttribution(headVerb.get())) {
                Relation relation = Relation.ATTRIBUTION;

                Extraction res = new Extraction(
                    getClass().getSimpleName(),
                    false,
                    null,
                    relation,
                    true,
                    Arrays.asList(leftConstituent, rightConstituent)
                );

                // Add annotations
                res.getAnnotations().addAll(annotations);

                return Optional.of(res);
            }
        }

        return Optional.empty();
    }
}
