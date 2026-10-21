package com.doping.prescription.exam.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.doping.prescription.IntegrationTest;
import com.doping.prescription.exam.domain.ExamOption;
import com.doping.prescription.exam.persistence.ExamQuestion;
import com.doping.prescription.exam.persistence.ExamQuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class ExamDemoDataLoaderIT {

    @Autowired
    private ExamDemoDataLoader loader;

    @Autowired
    private ExamQuestionRepository questions;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void seedsTwoExamsWithTwentyFourQuestionsAndDoesNotDuplicateOnRerun() {
        // When
        loader.load();
        loader.load();

        // Then
        assertThat(count("select count(*) from exam where id in (1, 2)")).isEqualTo(2);
        assertThat(count("select count(*) from exam_question where exam_id in (1, 2)"))
                .isEqualTo(24);
        ExamQuestion question4 = questions.findById(104).orElseThrow();
        assertThat(question4.getOrderNo()).isEqualTo(4);
        assertThat(question4.getTopicId()).isEqualTo(9);
        assertThat(question4.getContent().correctKey()).isEqualTo("C");
        assertThat(question4.getContent().options()).hasSize(5).first().isEqualTo(new ExamOption("A", "3/5"));
        assertThat(question4.getScenarioAnswer()).contains("A");
        assertThat(questions.findById(108).orElseThrow().getScenarioAnswer()).isEmpty();
    }

    private int count(String sql) {
        return jdbc.sql(sql).query(Integer.class).single();
    }
}
