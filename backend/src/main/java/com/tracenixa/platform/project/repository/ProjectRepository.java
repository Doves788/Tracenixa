package com.tracenixa.platform.project.repository;

import com.tracenixa.platform.project.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findByOwnerId(String ownerId);
    Optional<Project> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
