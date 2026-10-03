package com.aegispay.app.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExportFileRepository extends JpaRepository<ExportFile, UUID> {
    List<ExportFile> findByTenantIdAndPayRunId(UUID tenantId, UUID payRunId);
}
