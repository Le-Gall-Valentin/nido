package com.nido.api.finance.infrastructure.persistence.repository;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceMonthTotalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FinanceMonthTotalJpaRepository extends JpaRepository<FinanceMonthTotalEntity, UUID> {

    @Query("""
        select new com.nido.api.finance.infrastructure.persistence.repository.MonthNetRow(t.id, t.monthStart, t.netEncrypted)
        from FinanceMonthTotalEntity t
        where t.spaceId = :spaceId
        """)
    List<MonthNetRow> findNetRows(@Param("spaceId") UUID spaceId);

    @Modifying(flushAutomatically = true)
    @Query("delete from FinanceMonthTotalEntity t where t.spaceId = :spaceId and t.monthStart in :monthStarts")
    void deleteMonths(@Param("spaceId") UUID spaceId, @Param("monthStarts") Collection<LocalDate> monthStarts);

    @Modifying(flushAutomatically = true)
    @Query("delete from FinanceMonthTotalEntity t where t.spaceId = :spaceId")
    void deleteAllOfSpace(@Param("spaceId") UUID spaceId);
}
