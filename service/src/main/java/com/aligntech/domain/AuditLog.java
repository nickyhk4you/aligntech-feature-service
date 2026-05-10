package com.aligntech.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flag_id", nullable = false)
    private UUID flagId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "changed_by", nullable = false, length = 255)
    private String changedBy;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "JSONB")
    private Map<String, Object> oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "JSONB")
    private Map<String, Object> newValue;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt = Instant.now();

    public AuditLog(UUID flagId, String action, String changedBy, String changeSummary,
                    Map<String, Object> oldValue, Map<String, Object> newValue) {
        this.flagId = flagId;
        this.action = action;
        this.changedBy = changedBy;
        this.changeSummary = changeSummary;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedAt = Instant.now();
    }
}
