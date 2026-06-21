package com.app.modules.payment.repository;

import com.app.modules.payment.entity.ReconciliationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationLogRepository extends JpaRepository<ReconciliationLog, Long> {
}
