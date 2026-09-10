package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceSettlementRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FinanceSettlementRecordJpaRepository extends JpaRepository<FinanceSettlementRecordEntity, UUID> {
    List<FinanceSettlementRecordEntity> findBySpaceId(UUID spaceId);

    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockForSettlement(@Param("key") String key);

    /**
     * The settlements between one pair, either direction, most recent first. The caller used to
     * load every settlement in the space and filter in Java.
     */
    @Query("""
        select s from FinanceSettlementRecordEntity s
        where s.spaceId = :spaceId
          and ((s.fromUserId = :memberAId and s.toUserId = :memberBId)
            or (s.fromUserId = :memberBId and s.toUserId = :memberAId))
        order by s.settledDate desc
        """)
    List<FinanceSettlementRecordEntity> findBetweenMembers(@Param("spaceId") UUID spaceId,
                                                           @Param("memberAId") UUID memberAId,
                                                           @Param("memberBId") UUID memberBId);
}
