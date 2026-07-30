package com.edu.util;

import com.edu.pojo.enums.safety.SafetyGradeLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SafetyGradeLevelResolverTests {
    @Test
    void shouldResolvePrimaryGrades() {
        assertEquals(SafetyGradeLevel.PRIMARY, SafetyGradeLevelResolver.resolve("\u5c0f\u5b66"));
        assertEquals(SafetyGradeLevel.PRIMARY, SafetyGradeLevelResolver.resolve("\u5c0f\u5b66\u4e94\u5e74\u7ea7"));
        assertEquals(SafetyGradeLevel.PRIMARY, SafetyGradeLevelResolver.resolve("\u516d\u5e74\u7ea7"));
        assertEquals(SafetyGradeLevel.PRIMARY, SafetyGradeLevelResolver.resolve("G6"));
        assertEquals(SafetyGradeLevel.PRIMARY, SafetyGradeLevelResolver.fromGradeNumber(1));
        assertEquals(SafetyGradeLevel.PRIMARY, SafetyGradeLevelResolver.fromGradeNumber(6));
    }

    @Test
    void shouldResolveJuniorGrades() {
        assertEquals(SafetyGradeLevel.JUNIOR, SafetyGradeLevelResolver.resolve("\u521d\u4e2d"));
        assertEquals(SafetyGradeLevel.JUNIOR, SafetyGradeLevelResolver.resolve("\u521d\u4e8c"));
        assertEquals(SafetyGradeLevel.JUNIOR, SafetyGradeLevelResolver.resolve("\u516b\u5e74\u7ea7"));
        assertEquals(SafetyGradeLevel.JUNIOR, SafetyGradeLevelResolver.resolve("Grade 9"));
        assertEquals(SafetyGradeLevel.JUNIOR, SafetyGradeLevelResolver.fromGradeNumber(7));
        assertEquals(SafetyGradeLevel.JUNIOR, SafetyGradeLevelResolver.fromGradeNumber(9));
    }

    @Test
    void shouldResolveSeniorGrades() {
        assertEquals(SafetyGradeLevel.SENIOR, SafetyGradeLevelResolver.resolve("\u9ad8\u4e2d"));
        assertEquals(SafetyGradeLevel.SENIOR, SafetyGradeLevelResolver.resolve("\u9ad8\u4e00"));
        assertEquals(SafetyGradeLevel.SENIOR, SafetyGradeLevelResolver.resolve("\u5341\u5e74\u7ea7"));
        assertEquals(SafetyGradeLevel.SENIOR, SafetyGradeLevelResolver.resolve("12\u5e74\u7ea7"));
        assertEquals(SafetyGradeLevel.SENIOR, SafetyGradeLevelResolver.fromGradeNumber(10));
        assertEquals(SafetyGradeLevel.SENIOR, SafetyGradeLevelResolver.fromGradeNumber(12));
    }

    @Test
    void shouldReturnNullWhenGradeCannotBeResolved() {
        assertNull(SafetyGradeLevelResolver.resolve(null));
        assertNull(SafetyGradeLevelResolver.resolve(""));
        assertNull(SafetyGradeLevelResolver.resolve("\u5927\u5b66\u4e00\u5e74\u7ea7"));
        assertNull(SafetyGradeLevelResolver.resolve("\u672c\u79d1\u4e00\u5e74\u7ea7"));
        assertNull(SafetyGradeLevelResolver.fromGradeNumber(0));
        assertNull(SafetyGradeLevelResolver.fromGradeNumber(13));
    }

    @Test
    void shouldUseFallbackWhenGradeCannotBeResolved() {
        assertEquals(
                SafetyGradeLevel.JUNIOR,
                SafetyGradeLevelResolver.resolveOrDefault("\u672a\u77e5\u5b66\u6bb5", SafetyGradeLevel.JUNIOR)
        );
    }
}
