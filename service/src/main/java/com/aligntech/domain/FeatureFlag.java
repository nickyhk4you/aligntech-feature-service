package com.aligntech.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "feature_flags")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flag_key", nullable = false, unique = true, length = 255)
    private String flagKey;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "flag_type", nullable = false, length = 50)
    @Builder.Default
    private String flagType = "boolean";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "draft";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    @Builder.Default
    private List<String> environments = new ArrayList<>(List.of("development", "staging", "production"));

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "owner_team", length = 255)
    private String ownerTeam;

    @Column(name = "release_version", length = 100)
    private String releaseVersion;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // rollout config: { "type": "percentage", "percentage": 30, "userIds": ["eng-*"] }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rollout", columnDefinition = "JSONB")
    private Map<String, Object> rollout;

    protected FeatureFlag() {
    }

    @PrePersist
    void prePersist() {
        this.createdAt = this.createdAt != null ? this.createdAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return "active".equals(status);
    }
}
