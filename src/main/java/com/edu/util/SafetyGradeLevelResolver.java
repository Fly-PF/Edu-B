package com.edu.util;

import com.edu.pojo.enums.safety.SafetyGradeLevel;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves user-facing grade labels into the three policy levels used by the
 * safety gateway: PRIMARY, JUNIOR and SENIOR.
 */
public final class SafetyGradeLevelResolver {
    private static final Pattern SUFFIX_GRADE_PATTERN = Pattern.compile("(?<!\\d)([1-9]|1[0-2])\\s*(?:\\u5e74\\u7ea7|\\u7ea7|grade|g)(?!\\d)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PREFIX_GRADE_PATTERN = Pattern.compile("(?i)\\b(?:grade|g)\\s*([1-9]|1[0-2])\\b");

    private SafetyGradeLevelResolver() {
    }

    public static SafetyGradeLevel resolve(String gradeText) {
        if (!StringUtils.hasText(gradeText)) {
            return null;
        }

        String normalized = normalize(gradeText);
        if (isOutOfK12Scope(normalized)) {
            return null;
        }

        SafetyGradeLevel explicitStage = resolveExplicitStage(normalized);
        if (explicitStage != null) {
            return explicitStage;
        }

        Integer gradeNumber = resolveGradeNumber(normalized);
        if (gradeNumber == null) {
            return null;
        }
        return fromGradeNumber(gradeNumber);
    }

    public static SafetyGradeLevel resolveOrDefault(String gradeText, SafetyGradeLevel fallback) {
        SafetyGradeLevel resolved = resolve(gradeText);
        return resolved == null ? fallback : resolved;
    }

    public static SafetyGradeLevel fromGradeNumber(Integer gradeNumber) {
        if (gradeNumber == null) {
            return null;
        }
        if (gradeNumber >= 1 && gradeNumber <= 6) {
            return SafetyGradeLevel.PRIMARY;
        }
        if (gradeNumber >= 7 && gradeNumber <= 9) {
            return SafetyGradeLevel.JUNIOR;
        }
        if (gradeNumber >= 10 && gradeNumber <= 12) {
            return SafetyGradeLevel.SENIOR;
        }
        return null;
    }

    private static boolean isOutOfK12Scope(String value) {
        return containsAny(value,
                "\u5927\u5b66",
                "\u672c\u79d1",
                "\u7814\u7a76\u751f",
                "\u7855\u58eb",
                "\u535a\u58eb",
                "college",
                "university",
                "undergraduate",
                "graduate");
    }

    private static SafetyGradeLevel resolveExplicitStage(String value) {
        if (containsAny(value,
                "senior",
                "highschool",
                "high-school",
                "\u9ad8\u4e2d",
                "\u9ad8\u4e00",
                "\u9ad8\u4e8c",
                "\u9ad8\u4e09")) {
            return SafetyGradeLevel.SENIOR;
        }
        if (containsAny(value,
                "junior",
                "middleschool",
                "middle-school",
                "\u521d\u4e2d",
                "\u521d\u4e00",
                "\u521d\u4e8c",
                "\u521d\u4e09")) {
            return SafetyGradeLevel.JUNIOR;
        }
        if (containsAny(value,
                "primary",
                "elementary",
                "\u5c0f\u5b66",
                "\u5c0f\u4e00",
                "\u5c0f\u4e8c",
                "\u5c0f\u4e09",
                "\u5c0f\u56db",
                "\u5c0f\u4e94",
                "\u5c0f\u516d")) {
            return SafetyGradeLevel.PRIMARY;
        }
        return null;
    }

    private static Integer resolveGradeNumber(String value) {
        Integer arabicGrade = matchArabicGrade(value);
        if (arabicGrade != null) {
            return arabicGrade;
        }
        return matchChineseGrade(value);
    }

    private static Integer matchArabicGrade(String value) {
        Matcher matcher = SUFFIX_GRADE_PATTERN.matcher(value);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        matcher = PREFIX_GRADE_PATTERN.matcher(value);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

    private static Integer matchChineseGrade(String value) {
        if (containsAny(value, "\u5341\u4e8c\u5e74\u7ea7", "\u5341\u4e8c\u7ea7")) {
            return 12;
        }
        if (containsAny(value, "\u5341\u4e00\u5e74\u7ea7", "\u5341\u4e00\u7ea7")) {
            return 11;
        }
        if (containsAny(value, "\u5341\u5e74\u7ea7", "\u5341\u7ea7")) {
            return 10;
        }
        if (containsAny(value, "\u4e5d\u5e74\u7ea7", "\u4e5d\u7ea7")) {
            return 9;
        }
        if (containsAny(value, "\u516b\u5e74\u7ea7", "\u516b\u7ea7")) {
            return 8;
        }
        if (containsAny(value, "\u4e03\u5e74\u7ea7", "\u4e03\u7ea7")) {
            return 7;
        }
        if (containsAny(value, "\u516d\u5e74\u7ea7", "\u516d\u7ea7")) {
            return 6;
        }
        if (containsAny(value, "\u4e94\u5e74\u7ea7", "\u4e94\u7ea7")) {
            return 5;
        }
        if (containsAny(value, "\u56db\u5e74\u7ea7", "\u56db\u7ea7")) {
            return 4;
        }
        if (containsAny(value, "\u4e09\u5e74\u7ea7", "\u4e09\u7ea7")) {
            return 3;
        }
        if (containsAny(value, "\u4e8c\u5e74\u7ea7", "\u4e8c\u7ea7")) {
            return 2;
        }
        if (containsAny(value, "\u4e00\u5e74\u7ea7", "\u4e00\u7ea7")) {
            return 1;
        }
        return null;
    }

    private static String normalize(String value) {
        return value.trim()
                .replaceAll("\\s+", "")
                .replace("\uff08", "(")
                .replace("\uff09", ")")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
