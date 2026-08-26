package com.hirehub.aiinterview.repository;

import com.hirehub.aiinterview.entity.AiInterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiInterviewQuestionRepository extends JpaRepository<AiInterviewQuestion, UUID> {

    List<AiInterviewQuestion> findByInterviewIdOrderByQuestionNumberAsc(UUID interviewId);

    Optional<AiInterviewQuestion> findByInterviewIdAndQuestionNumber(UUID interviewId, Integer questionNumber);

    boolean existsByInterviewIdAndQuestionNumber(UUID interviewId, Integer questionNumber);

    long countByInterviewIdAndStudentAnswerIsNotNull(UUID interviewId);
}
