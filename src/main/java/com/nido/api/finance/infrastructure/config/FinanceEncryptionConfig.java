package com.nido.api.finance.infrastructure.config;

import com.nido.api.finance.infrastructure.persistence.entity.FinanceBudgetEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceCategoryEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceRecurringSeriesContributorEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceRecurringSeriesEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsContributionEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsGoalEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSettlementRecordEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionContributorEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import com.nido.api.infrastructure.sealing.SealedColumns;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FinanceEncryptionConfig {

    /** The columns of finance that hold sealed values — see SealedValueMigration and SpaceKeyGuard. */
    @Bean
    SealedColumns financeSealedColumns() {
        return SealedColumns.of(FinanceCategoryEntity.LABEL, FinanceTransactionEntity.LABEL, FinanceTransactionEntity.AMOUNT,
            FinanceTransactionContributorEntity.SHARE_AMOUNT, FinanceRecurringSeriesEntity.LABEL, FinanceRecurringSeriesEntity.AMOUNT,
            FinanceRecurringSeriesContributorEntity.SHARE_AMOUNT, FinanceBudgetEntity.MONTHLY_LIMIT, FinanceSavingsGoalEntity.NAME,
            FinanceSavingsGoalEntity.TARGET_AMOUNT, FinanceSavingsContributionEntity.AMOUNT, FinanceSettlementRecordEntity.AMOUNT);
    }
}
