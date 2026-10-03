package com.aegispay.app.time;

import com.aegispay.app.platform.tenancy.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "punch_import_batch")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PunchImportBatch extends TenantEntity {

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_sha256", nullable = false)
    private String fileSha256;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(nullable = false)
    private String status;

    @Column(name = "imported_by")
    private UUID importedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public void setFileSha256(String fileSha256) {
        this.fileSha256 = fileSha256;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setImportedBy(UUID importedBy) {
        this.importedBy = importedBy;
    }
}
