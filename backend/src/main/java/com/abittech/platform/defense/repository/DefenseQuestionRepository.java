package com.abittech.platform.defense.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.abittech.platform.defense.entity.DefenseQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefenseQuestionRepository extends JpaRepository<DefenseQuestion, UUID> {

    List<DefenseQuestion> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    Optional<DefenseQuestion> findByIdAndProjectId(UUID id, UUID projectId);
}
