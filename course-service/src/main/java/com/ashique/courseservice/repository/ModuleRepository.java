package com.ashique.courseservice.repository;

import com.ashique.courseservice.entity.Module;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleRepository extends JpaRepository<Module, UUID> {
}
