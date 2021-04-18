package diarsid.librarian.impl.logic.impl;

import java.util.ArrayList;
import java.util.List;

import diarsid.librarian.api.model.Entry;
import diarsid.support.objects.CommonEnum;
import diarsid.support.strings.PathUtils;
import diarsid.support.strings.StringUtils;

import static java.util.stream.Collectors.toList;

import static diarsid.librarian.api.model.Entry.Type.PATH;
import static diarsid.librarian.impl.logic.impl.StringTransformations.CaseConversion.CASE_ORIGINAL;
import static diarsid.librarian.impl.logic.impl.StringTransformations.CaseConversion.CASE_TO_LOWER;
import static diarsid.librarian.impl.logic.impl.WordsInEntriesImpl.joinSingleCharsToNextWord;
import static diarsid.support.strings.StringUtils.splitByAnySeparators;
import static diarsid.support.strings.StringUtils.splitCamelCase;

public class StringTransformations {

    public enum CaseConversion implements CommonEnum<CaseConversion> {
        CASE_TO_LOWER,
        CASE_ORIGINAL
    }

    public static String simplify(String original, CaseConversion caseConversion) {
        String unified = original.trim().strip();

        if ( caseConversion.equalTo(CASE_TO_LOWER) ) {
            unified = unified.toLowerCase();
        }

        unified = StringUtils.normalizeSpaces(unified);
        unified = StringUtils.normalizeDashes(unified);
        unified = StringUtils.normalizeUnderscores(unified);

        if ( StringUtils.containsPathSeparator(unified) ) {
            unified = PathUtils.normalizeSeparators(unified);
        }

        unified = unified.replace('-', ' ');
        unified = unified.replace('_', ' ');
        unified = unified.replace('#', 'n');
        unified = StringUtils.removeSpecialCharsFrom(unified, ' ', '/');

        return unified;
    }

    public static String simplify(String original, CaseConversion caseConversion, Entry.Type type) {
        String unified = original.trim().strip();

        if ( caseConversion.equalTo(CASE_TO_LOWER) ) {
            unified = unified.toLowerCase();
        }

        unified = StringUtils.normalizeSpaces(unified);
        unified = StringUtils.normalizeDashes(unified);
        unified = StringUtils.normalizeUnderscores(unified);

        if ( type.equalTo(PATH) ) {
            unified = PathUtils.normalizeSeparators(unified);
        }

        unified = unified.replace('-', ' ');
        unified = unified.replace('_', ' ');
        unified = unified.replace('#', 'n');
        unified = StringUtils.removeSpecialCharsFrom(unified, ' ', '/');

        return unified;
    }

    public static List<String> toSimplifiedWords(
            String target,
            CaseConversion caseConversion,
            boolean decomposeCamelCase,
            boolean joinSingleCharToNextWord,
            boolean allowSingleChar) {
        String simplified;
        List<String> words;

        if ( decomposeCamelCase ) {
            simplified = simplify(target, CASE_ORIGINAL);
            words = splitByAnySeparators(simplified);

            if ( joinSingleCharToNextWord ) {
                words = joinSingleCharsToNextWord(words);
            }

            List<String> wordsDecomposed = new ArrayList<>();
            for ( String word : words ) {
                wordsDecomposed.addAll(splitCamelCase(word, false));
            }
            words = wordsDecomposed;

            if ( caseConversion == CASE_TO_LOWER ) {
                words = words
                        .stream()
                        .map(String::toLowerCase)
                        .collect(toList());
            }
        }
        else {
            simplified = simplify(target, caseConversion);
            words = splitByAnySeparators(simplified);

            if ( joinSingleCharToNextWord ) {
                words = joinSingleCharsToNextWord(words);
            }
        }

        return words
                .stream()
                .distinct()
                .collect(toList());
    }
}
