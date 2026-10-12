package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceMonthMemberTotalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FinanceMonthMemberTotalJpaRepository extends JpaRepository<FinanceMonthMemberTotalEntity, UUID> {

    /** Oldest month first: members come out in the order they first took part, as the whole-history fold listed them. */
    @Query("""
        select new com.nido.api.finance.infrastructure.persistence.repository.MemberNetRow(t.id, t.memberId, t.netEncrypted)
        from FinanceMonthMemberTotalEntity t
        where t.spaceId = :spaceId
        order by t.monthStart, t.memberId
        """)
    List<MemberNetRow> findNetRows(@Param("spaceId") UUID spaceId);

    @Modifying(flushAutomatically = true)
    @Query("delete from FinanceMonthMemberTotalEntity t where t.spaceId = :spaceId and t.monthStart in :monthStarts")
    void deleteMonths(@Param("spaceId") UUID spaceId, @Param("monthStarts") Collection<LocalDate> monthStarts);

    @Modifying(flushAutomatically = true)
    @Query("delete from FinanceMonthMemberTotalEntity t where t.spaceId = :spaceId")
    void deleteAllOfSpace(@Param("spaceId") UUID spaceId);
}
