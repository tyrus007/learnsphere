package com.ashique.courseservice.repository;

import com.ashique.courseservice.entity.Module;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModuleRepository extends JpaRepository<Module, UUID> {
    List<Module> findAllByCourseIdOrderByPositionAsc(UUID courseId);

    Optional<Module> findTopByCourseIdOrderByPositionDesc(UUID courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Module m where m.id = :moduleId")
    Optional<Module> findByIdForUpdate(@Param("moduleId") UUID moduleId);
}
