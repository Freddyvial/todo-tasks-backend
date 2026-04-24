package com.imaginamos.todo.repository;

import com.imaginamos.todo.entity.Task;
import com.imaginamos.todo.entity.TaskStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @EntityGraph(attributePaths = "items")
    @Query("select t from Task t where t.id = :id")
    Optional<Task> findByIdWithItems(@Param("id") Long id);

    @EntityGraph(attributePaths = "items")
    @Query("select distinct t from Task t where t.id in :ids")
    List<Task> findAllByIdInWithItems(@Param("ids") Collection<Long> ids);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}
