package com.hirehub.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillMatchingServiceTest {

    private final SkillMatchingService service = new SkillMatchingService(
            new com.fasterxml.jackson.databind.ObjectMapper());

    @Test
    void jsonArraySkillsMatch() {
        String student = "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"Docker\"]";
        String job = "[\"Java\", \"Spring Boot\", \"PostgreSQL\", \"Kubernetes\", \"Kafka\"]";

        var result = service.calculateMatch(student, job);

        assertEquals(3, result.matchedSkills().size());
        assertEquals(2, result.missingSkills().size());
        assertTrue(result.matchedSkills().contains("java"));
        assertTrue(result.matchedSkills().contains("spring boot"));
        assertTrue(result.matchedSkills().contains("postgresql"));
        assertTrue(result.missingSkills().contains("kubernetes"));
        assertTrue(result.missingSkills().contains("kafka"));
        assertEquals(60, result.score());
    }

    @Test
    void commaSeparatedSkillsMatch() {
        String student = "Java, Python, React";
        String job = "Java, React, Angular";

        var result = service.calculateMatch(student, job);

        assertEquals(2, result.matchedSkills().size());
        assertEquals(1, result.missingSkills().size());
        assertEquals(67, result.score());
    }

    @Test
    void emptyJobSkillsReturns50() {
        String student = "Java, Python";
        String job = "[]";

        var result = service.calculateMatch(student, job);

        assertEquals(50, result.score());
        assertTrue(result.matchedSkills().isEmpty());
        assertTrue(result.missingSkills().isEmpty());
    }

    @Test
    void emptyStudentSkillsReturns0() {
        String student = "[]";
        String job = "[\"Java\", \"Python\"]";

        var result = service.calculateMatch(student, job);

        assertEquals(0, result.score());
        assertEquals(2, result.missingSkills().size());
    }

    @Test
    void normalizationHandlesVariations() {
        String student = "[\"JS\", \"React.js\", \"K8s\", \"Postgres\", \"C#\"]";
        String job = "[\"JavaScript\", \"React\", \"Kubernetes\", \"PostgreSQL\", \"CSharp\"]";

        var result = service.calculateMatch(student, job);

        assertEquals(5, result.matchedSkills().size());
        assertEquals(0, result.missingSkills().size());
        assertEquals(100, result.score());
    }

    @Test
    void caseInsensitiveMatch() {
        String student = "[\"JAVA\", \"spring boot\"]";
        String job = "[\"java\", \"Spring Boot\"]";

        var result = service.calculateMatch(student, job);

        assertEquals(2, result.matchedSkills().size());
        assertEquals(100, result.score());
    }

    @Test
    void nullSkillsHandled() {
        var result = service.calculateMatch(null, null);
        assertEquals(50, result.score());
    }

    @Test
    void allMatch() {
        String student = "[\"Java\", \"Spring Boot\", \"Docker\"]";
        String job = "[\"Java\", \"Spring Boot\", \"Docker\"]";

        var result = service.calculateMatch(student, job);

        assertEquals(100, result.score());
        assertEquals(3, result.matchedSkills().size());
        assertTrue(result.missingSkills().isEmpty());
    }

    @Test
    void noneMatch() {
        String student = "[\"Ruby\", \"Rails\"]";
        String job = "[\"Java\", \"Spring Boot\"]";

        var result = service.calculateMatch(student, job);

        assertEquals(0, result.score());
        assertTrue(result.matchedSkills().isEmpty());
        assertEquals(2, result.missingSkills().size());
    }
}
