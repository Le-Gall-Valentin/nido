package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.finance.infrastructure.config.FinanceEncryptorFactory;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsContributionEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsGoalEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSavingsContributionJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSavingsGoalJpaRepository;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SavingsGoalRepositoryAdapter implements SavingsGoalRepository {

    private final FinanceSavingsGoalJpaRepository goals;
    private final FinanceSavingsContributionJpaRepository goalContributions;
    private final FinanceEncryptorFactory encryptorFactory;

    public SavingsGoalRepositoryAdapter(
            FinanceSavingsGoalJpaRepository goals, FinanceSavingsContributionJpaRepository goalContributions,
            FinanceEncryptorFactory encryptorFactory) {
        this.goals = goals;
        this.goalContributions = goalContributions;
        this.encryptorFactory = encryptorFactory;
    }

    @Override
    public Optional<SavingsGoal> findById(UUID goalId) {
        return goals.findById(goalId).map(e -> toDomain(e, e.getSpaceId()));
    }

    @Override
    public List<SavingsGoal> findBySpaceId(UUID spaceId) {
        return goals.findBySpaceId(spaceId).stream().map(e -> toDomain(e, spaceId)).toList();
    }

    @Override
    @Transactional
    public SavingsGoal create(CreateSavingsGoalCommand command) {
        TextEncryptor encryptor = encryptorFactory.forSpace(command.spaceId());
        FinanceSavingsGoalEntity e = new FinanceSavingsGoalEntity();
        e.setSpaceId(command.spaceId());
        e.setNameEncrypted(encryptor.encrypt(command.name()));
        e.setTargetAmountEncrypted(encryptor.encrypt(command.targetAmount().toPlainString()));
        e.setTargetDate(command.targetDate());
        e.setColor(command.color());
        e.setGlyph(command.glyph());
        FinanceSavingsGoalEntity saved = goals.saveAndFlush(e);
        return toDomain(saved, command.spaceId());
    }

    @Override
    @Transactional
    public SavingsGoal update(UpdateSavingsGoalCommand command) {
        TextEncryptor encryptor = encryptorFactory.forSpace(command.spaceId());
        FinanceSavingsGoalEntity e = goals.findById(command.goalId()).orElseThrow(FinanceException.SavingsGoalNotFound::new);
        e.setNameEncrypted(encryptor.encrypt(command.name()));
        e.setTargetAmountEncrypted(encryptor.encrypt(command.targetAmount().toPlainString()));
        e.setTargetDate(command.targetDate());
        e.setColor(command.color());
        e.setGlyph(command.glyph());
        FinanceSavingsGoalEntity saved = goals.saveAndFlush(e);
        return toDomain(saved, command.spaceId());
    }

    @Override
    public void delete(UUID goalId) {
        goals.deleteById(goalId);
        goals.flush();
    }

    @Override
    public List<SavingsContribution> findContributionsByGoalId(UUID goalId) {
        FinanceSavingsGoalEntity goal = goals.findById(goalId).orElseThrow(FinanceException.SavingsGoalNotFound::new);
        TextEncryptor encryptor = encryptorFactory.forSpace(goal.getSpaceId());
        return goalContributions.findByGoalId(goalId).stream()
            .map(ce -> new SavingsContribution(ce.getId(), ce.getGoalId(), ce.getUserId(),
                new BigDecimal(encryptor.decrypt(ce.getAmountEncrypted())), ce.getContributedDate()))
            .toList();
    }

    @Override
    @Transactional
    public SavingsContribution addContribution(AddSavingsContributionCommand command) {
        TextEncryptor encryptor = encryptorFactory.forSpace(command.spaceId());
        FinanceSavingsContributionEntity ce = new FinanceSavingsContributionEntity();
        ce.setGoalId(command.goalId());
        ce.setUserId(command.memberId());
        ce.setAmountEncrypted(encryptor.encrypt(command.amount().toPlainString()));
        ce.setContributedDate(command.date());
        FinanceSavingsContributionEntity saved = goalContributions.saveAndFlush(ce);
        return new SavingsContribution(saved.getId(), saved.getGoalId(), saved.getUserId(), command.amount(), saved.getContributedDate());
    }

    private SavingsGoal toDomain(FinanceSavingsGoalEntity e, UUID spaceId) {
        TextEncryptor encryptor = encryptorFactory.forSpace(spaceId);
        return new SavingsGoal(e.getId(), e.getSpaceId(), encryptor.decrypt(e.getNameEncrypted()),
            new BigDecimal(encryptor.decrypt(e.getTargetAmountEncrypted())), e.getTargetDate(), e.getColor(), e.getGlyph());
    }
}
