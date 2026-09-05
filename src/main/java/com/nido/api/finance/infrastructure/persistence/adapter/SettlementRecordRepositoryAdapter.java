package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
import com.nido.api.finance.infrastructure.config.FinanceEncryptorFactory;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSettlementRecordEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSettlementRecordJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class SettlementRecordRepositoryAdapter implements SettlementRecordRepository {

    private final FinanceSettlementRecordJpaRepository settlements;
    private final FinanceEncryptorFactory encryptorFactory;

    public SettlementRecordRepositoryAdapter(FinanceSettlementRecordJpaRepository settlements, FinanceEncryptorFactory encryptorFactory) {
        this.settlements = settlements;
        this.encryptorFactory = encryptorFactory;
    }

    @Override
    public List<SettlementRecord> findBySpaceId(UUID spaceId) {
        return settlements.findBySpaceId(spaceId).stream().map(e -> toDomain(e, spaceId)).toList();
    }

    @Override
    @Transactional
    public SettlementRecord create(CreateSettlementCommand command) {
        FinanceSettlementRecordEntity e = new FinanceSettlementRecordEntity();
        e.setSpaceId(command.spaceId());
        e.setFromUserId(command.fromMemberId());
        e.setToUserId(command.toMemberId());
        e.setAmountEncrypted(encryptorFactory.forSpace(command.spaceId()).encrypt(command.amount().toPlainString()));
        e.setSettledDate(command.date());
        FinanceSettlementRecordEntity saved = settlements.saveAndFlush(e);
        return toDomain(saved, command.spaceId());
    }

    private SettlementRecord toDomain(FinanceSettlementRecordEntity e, UUID spaceId) {
        BigDecimal amount = new BigDecimal(encryptorFactory.forSpace(spaceId).decrypt(e.getAmountEncrypted()));
        return new SettlementRecord(e.getId(), e.getSpaceId(), e.getFromUserId(), e.getToUserId(), amount, e.getSettledDate());
    }
}
