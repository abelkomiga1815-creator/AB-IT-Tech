package com.abittech.platform.database;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

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

class ResearchConstraintsTest extends IntegrationTestBase {

    @Autowired
    private StudentProfileRepository studentProfileRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private Project newProject() {
        User user = userRepository.saveAndFlush(
                new User("DB Student", "rc-" + UUID.randomUUID() + "@example.com", "hash", Role.STUDENT));
        StudentProfile profile = new StudentProfile(user);
        profile.setUniversity("DB University");
        profile.setProgramme("DB Programme");
        return projectRepository.saveAndFlush(new Project(studentProfileRepository.saveAndFlush(profile), "DB project"));
    }

    private void insertAnalysis(UUID projectId, String level, int papers) {
        jdbc.update("INSERT INTO research_analyses (id, project_id, project_title, similarity_level, papers_found, report_json) "
                + "VALUES (?, ?, 'T', ?, ?, '{}')", UUID.randomUUID(), projectId, level, papers);
    }

    private void insertSource(UUID projectId, String doi, Integer year) {
        jdbc.update("INSERT INTO research_sources (id, project_id, title, doi, publication_year) VALUES (?, ?, 'T', ?, ?)",
                UUID.randomUUID(), projectId, doi, year);
    }

    @Test
    void analysisLevelAndCountAreChecked() {
        Project project = newProject();
        insertAnalysis(project.getId(), "moderate", 3);
        assertThatThrownBy(() -> insertAnalysis(project.getId(), "ORIGINAL", 3))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertAnalysis(project.getId(), "low", -1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void analysesAndSourcesNeedAnExistingProject() {
        assertThatThrownBy(() -> insertAnalysis(UUID.randomUUID(), "low", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertSource(UUID.randomUUID(), "10.1/x", 2020))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aDoiCanOnlyBeSavedOncePerProjectButSeveralWithoutDoiAreFine() {
        Project first = newProject();
        Project second = newProject();
        insertSource(first.getId(), "10.1/same", 2020);
        insertSource(second.getId(), "10.1/same", 2020);          // another project: allowed
        insertSource(first.getId(), null, 2020);
        insertSource(first.getId(), null, 2021);                  // no DOI: never conflicts
        assertThatThrownBy(() -> insertSource(first.getId(), "10.1/same", 2022))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void publicationYearIsChecked() {
        Project project = newProject();
        assertThatThrownBy(() -> insertSource(project.getId(), "10.1/old", 12))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void projectsWithResearchDataCannotBeDeleted() {
        Project project = newProject();
        insertAnalysis(project.getId(), "low", 1);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM projects WHERE id = ?", project.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
