package com.aligntech.repository;

import com.aligntech.domain.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    Optional<FeatureFlag> findByFlagKey(String flagKey);

    List<FeatureFlag> findByStatus(String status);

    @Query("SELECT f FROM FeatureFlag f WHERE f.status = 'active'")
    List<FeatureFlag> findAllActive();

    @Query("SELECT f FROM FeatureFlag f WHERE f.updatedAt > :since")
    List<FeatureFlag> findFlagsUpdatedSince(@Param("since") Instant since);
}
