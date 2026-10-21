package com.doping.prescription.content.application;

import com.doping.prescription.content.api.MicroQuestionCatalog;
import com.doping.prescription.content.domain.MicroQuestionStatus;
import com.doping.prescription.content.domain.QuestionContent;
import com.doping.prescription.content.persistence.MicroQuestion;
import com.doping.prescription.content.persistence.MicroQuestionRepository;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serves {@link MicroQuestionCatalog}. */
@Service
class MicroQuestionCatalogService implements MicroQuestionCatalog {

    private final MicroQuestionRepository questions;

    MicroQuestionCatalogService(MicroQuestionRepository questions) {
        this.questions = questions;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizQuestion> nextQuestion(Integer contentId, Set<Integer> answeredQuestionIds) {
        return questions.findByContentIdAndStatusOrderByIdAsc(contentId, MicroQuestionStatus.LIVE).stream()
                .filter(question -> !answeredQuestionIds.contains(question.getId()))
                .findFirst()
                .map(MicroQuestionCatalogService::view);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizQuestion> question(Integer microQuestionId) {
        return questions.findById(microQuestionId).map(MicroQuestionCatalogService::view);
    }

    private static QuizQuestion view(MicroQuestion question) {
        QuestionContent content = question.getContent();
        return new QuizQuestion(
                question.getId(),
                question.getContentId(),
                question.getTopicId(),
                content.stem(),
                content.options().stream()
                        .map(option -> new Option(option.key(), option.text()))
                        .toList(),
                content.correctKey(),
                content.explanation());
    }
}
