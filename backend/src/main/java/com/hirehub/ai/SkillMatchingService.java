package com.hirehub.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic skill matching service.
 * Compares student skills against job required/preferred skills
 * and calculates overlap scores without AI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillMatchingService {

    private final ObjectMapper objectMapper;

    /**
     * Calculate deterministic skill match between student and job.
     *
     * @param studentSkills student's skills (JSON array string)
     * @param jobSkills job's required skills (JSON array string)
     * @return SkillMatchResult with matched/missing skills and score
     */
    public SkillMatchResult calculateMatch(String studentSkills, String jobSkills) {
        Set<String> normalizedStudent = parseAndNormalize(studentSkills);
        Set<String> normalizedJob = parseAndNormalize(jobSkills);

        Set<String> matched = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>();

        for (String jobSkill : normalizedJob) {
            if (normalizedStudent.contains(jobSkill)) {
                matched.add(jobSkill);
            } else {
                missing.add(jobSkill);
            }
        }

        double score = normalizedJob.isEmpty()
                ? 50.0
                : (double) matched.size() / normalizedJob.size() * 100.0;

        return SkillMatchResult.builder()
                .matchedSkills(new ArrayList<>(matched))
                .missingSkills(new ArrayList<>(missing))
                .score(Math.round(score))
                .build();
    }

    /**
     * Parse a JSON array string of skills and normalize them.
     */
    private Set<String> parseAndNormalize(String skillsJson) {
        try {
            if (skillsJson == null || skillsJson.isBlank() || "[]".equals(skillsJson.trim())) {
                return Set.of();
            }
            List<String> skills = objectMapper.readValue(skillsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return skills.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(this::normalizeSkill)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.debug("Failed to parse skills JSON, trying comma-separated: {}", skillsJson);
            return parseCommaSeparated(skillsJson);
        }
    }

    /**
     * Parse comma-separated skills as fallback.
     */
    private Set<String> parseCommaSeparated(String skills) {
        if (skills == null || skills.isBlank()) return Set.of();
        return Arrays.stream(skills.split(","))
                .map(this::normalizeSkill)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Normalize a skill name for comparison.
     * Lowercase, trim, handle common variations.
     */
    private String normalizeSkill(String skill) {
        if (skill == null) return "";
        String normalized = skill.trim().toLowerCase();

        // Common variations
        return switch (normalized) {
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "py" -> "python";
            case "k8s" -> "kubernetes";
            case "postgres" -> "postgresql";
            case "react.js" -> "react";
            case "reactjs" -> "react";
            case "node.js" -> "nodejs";
            case "nodejs" -> "nodejs";
            case "vue.js" -> "vue";
            case "vuejs" -> "vue";
            case "c#" -> "csharp";
            case ".net" -> "dotnet";
            case "spring" -> "spring boot";
            case "spring-boot" -> "spring boot";
            case "mysql" -> "mysql";
            case "mongo" -> "mongodb";
            case "mongo db" -> "mongodb";
            case "amazon web services" -> "aws";
            case "google cloud platform" -> "gcp";
            case "microsoft azure" -> "azure";
            case "ci/cd" -> "cicd";
            case "machine learning" -> "ml";
            case "artificial intelligence" -> "ai";
            case "rest api" -> "rest";
            case "rest apis" -> "rest";
            case "restful api" -> "rest";
            case "graphql api" -> "graphql";
            default -> normalized;
        };
    }

    public record SkillMatchResult(
            List<String> matchedSkills,
            List<String> missingSkills,
            long score
    ) {
        public static SkillMatchResultBuilder builder() {
            return new SkillMatchResultBuilder();
        }

        public static class SkillMatchResultBuilder {
            private List<String> matchedSkills = new ArrayList<>();
            private List<String> missingSkills = new ArrayList<>();
            private long score = 0;

            public SkillMatchResultBuilder matchedSkills(List<String> v) { this.matchedSkills = v; return this; }
            public SkillMatchResultBuilder missingSkills(List<String> v) { this.missingSkills = v; return this; }
            public SkillMatchResultBuilder score(long v) { this.score = v; return this; }
            public SkillMatchResult build() { return new SkillMatchResult(matchedSkills, missingSkills, score); }
        }
    }
}
