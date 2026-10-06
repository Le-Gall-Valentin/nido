package com.nido.api.finance.infrastructure.config;

import com.nido.api.infrastructure.config.EncryptionBackfill;
import com.nido.api.infrastructure.config.ExistingCiphertextCheck;
import com.nido.api.infrastructure.config.PlaintextTable;
import com.nido.api.infrastructure.config.PlaintextTableEncryptor;
import com.nido.api.infrastructure.config.PlaintextTablesBackfill;
import com.nido.api.infrastructure.config.SpaceCiphertextCheck;
import com.nido.api.infrastructure.config.SpaceEncryptorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
public class FinanceEncryptionConfig {

    // One key per space, shared with every module that encrypts a space's data — see SpaceKeyCache.
    @Bean
    FinanceEncryptorFactory financeEncryptorFactory(SpaceEncryptorFactory spaces) {
        return spaces::forSpace;
    }

    /** The labels of default categories, stored in clear before 0.13.1 — see EncryptionBackfillRunner. */
    @Bean
    EncryptionBackfill financeEncryptionBackfill(PlaintextTableEncryptor encryptor, SpaceEncryptorFactory spaces) {
        return new PlaintextTablesBackfill(encryptor, spaces::forSpace, PlaintextTable.ofSpace("finance_categories", "label"));
    }

    /** Finance has been encrypted since it exists: one of its values per space proves the key. */
    @Bean
    ExistingCiphertextCheck financeCiphertextCheck(JdbcClient jdbc, SpaceEncryptorFactory spaces) {
        return new SpaceCiphertextCheck(jdbc, spaces::forSpace, "finance data", """
            SELECT DISTINCT ON (space_id) space_id, value FROM (
                SELECT space_id, amount_encrypted AS value FROM finance_transactions
                UNION ALL
                SELECT space_id, label_encrypted FROM finance_categories WHERE label_encrypted IS NOT NULL
            ) sample
            ORDER BY space_id""");
    }
}
