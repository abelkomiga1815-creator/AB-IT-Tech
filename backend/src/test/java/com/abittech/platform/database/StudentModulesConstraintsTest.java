package com.abittech.platform.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.abittech.platform.defense.entity.DefenseQuestion;
import com.abittech.platform.defense.repository.DefenseQuestionRepository;
import com.abittech.platform.document.entity.DocumentType;
import com.abittech.platform.document.entity.ProjectDocument;
import com.abittech.platform.document.repository.ProjectDocumentRepository;
import com.abittech.platform.project.entity.Project;
import com.abittech.platform.project.repository.ProjectRepository;
import com.abittech.platform.student.entity.StudentProfile;
import com.abittech.platform.student.repository.StudentProfileRepository;
import com.abittech.platform.support.IntegrationTestBase;
import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Database-level guarantees of projects, progress, documents and defense questions.
 */
class StudentModulesConstraintsTest extends IntegrationTestBase {

    @Autowired
    private StudentProfileRepository studentProfileRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectDocumentRepository documentRepository;
    @Autowired
    private DefenseQuestionRepository questionRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private StudentProfile newStudentProfile() {
        User user = userRepository.saveAndFlush(
                new User("DB Student", "sm-" + UUID.randomUUID() + "@example.com", "hash", Role.STUDENT));
        StudentProfile profile = new StudentProfile(user);
        profile.setUniversity("Constraint University " + UUID.randomUUID());
        profile.setProgramme("Computer Science");
        return studentProfileRepository.saveAndFlush(profile);
    }

    private Project newProject() {
        return projectRepository.saveAndFlush(new Project(newStudentProfile(), "DB project"));
    }

    private ProjectDocument document(Project project, String key, long size) {
        return new ProjectDocument(project, DocumentType.OTHER, "file.pdf", "application/pdf", size, key, null);
    }

    @Test
    void projectStatusStageAndProgressAreChecked() {
        UUID profileId = newStudentProfile().getId();
        String insert = "INSERT INTO projects (id, student_profile_id, title, %s) VALUES (?, ?, ?, ?)";

        assertThatThrownBy(() -> jdbc.update(insert.formatted("status"), UUID.randomUUID(), profileId, "T", "FINISHED"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert.formatted("current_stage"), UUID.randomUUID(), profileId, "T", "NAPPING"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert.formatted("progress_percent"), UUID.randomUUID(), profileId, "T", 101))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void projectNeedsAnExistingStudentProfile() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO projects (id, student_profile_id, title) VALUES (?, ?, ?)",
                UUID.randomUUID(), UUID.randomUUID(), "Orphan"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void progressPercentAndStageAreChecked() {
        Project project = newProject();
        String insert = "INSERT INTO project_progress (id, project_id, stage, percent_complete) VALUES (?, ?, ?, ?)";

        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), project.getId(), "WRITING", -1))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), project.getId(), "PARTYING", 10))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update(insert, UUID.randomUUID(), project.getId(), "WRITING", 10);
    }

    @Test
    void documentStorageKeyMustBeUniqueAndSizePositive() {
        Project project = newProject();
        String key = UUID.randomUUID() + ".pdf";
        documentRepository.saveAndFlush(document(project, key, 10));

        assertThatThrownBy(() -> documentRepository.saveAndFlush(document(project, key, 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> documentRepository.saveAndFlush(document(project, UUID.randomUUID() + ".pdf", 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void defenseQuestionCategoryIsChecked() {
        Project project = newProject();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO defense_questions (id, project_id, question, category) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), project.getId(), "Q?", "FOOD"))
                .isInstanceOf(DataIntegrityViolationException.class);
        questionRepository.saveAndFlush(new DefenseQuestion(project, "Valid question?"));
    }

    @Test
    void studentProfilesAndProjectsWithContentCannotBeDeleted() {
        Project project = newProject();
        documentRepository.saveAndFlush(document(project, UUID.randomUUID() + ".pdf", 10));
        questionRepository.saveAndFlush(new DefenseQuestion(project, "Question?"));

        assertThatThrownBy(() -> jdbc.update("DELETE FROM student_profiles WHERE id = ?",
                project.getStudentProfile().getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM projects WHERE id = ?", project.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(projectRepository.existsById(project.getId())).isTrue();
    }
}
