package com.app.modules.permission.repository;

import com.app.common.enums.Status;
import com.app.modules.permission.entity.Modules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModuleRepository extends JpaRepository<Modules, Long> {

    List<Modules> findByStatusOrderByDisplayOrderAsc(Status status);

}