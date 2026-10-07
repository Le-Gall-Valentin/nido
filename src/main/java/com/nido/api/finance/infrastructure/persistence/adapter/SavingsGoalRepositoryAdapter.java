package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsContributionEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSavingsGoalEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSavingsContributionJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSavingsGoalJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SavingsGoalRepositoryAdapter implements SavingsGoalRepository {

    private final FinanceSavingsGoalJpaRepository goals;
    private final FinanceSavingsContributionJpaRepository goalContributions;
    private final SpaceSealers sealers;

    public SavingsGoalRepositoryAdapter(
            FinanceSavingsGoalJpaRepository goals, FinanceSavingsContributionJpaRepository goalContributions,
            SpaceSealers sealers) {
        this.goals = goals;
        this.goalContributions = goalContributions;
        this.sealers = sealers;
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
        SpaceSealer sealer = sealers.forSpace(command.spaceId());
        FinanceSavingsGoalEntity e = new FinanceSavingsGoalEntity();
        e.setSpaceId(command.spaceId());
        e.setNameEncrypted(sealer.seal(FinanceSavingsGoalEntity.NAME, e.getId(), command.name()));
        e.setTargetAmountEncrypted(sealer.seal(FinanceSavingsGoalEntity.TARGET_AMOUNT, e.getId(), command.targetAmount().toPlainString()));
        e.setTargetDate(command.targetDate());
        e.setColor(command.color());
        e.setGlyph(command.glyph());
        FinanceSavingsGoalEntity saved = goals.saveAndFlush(e);
        return toDomain(saved, saved.getSpaceId());
    }

    @Override
    @Transactional
    public SavingsGoal update(UpdateSavingsGoalCommand command) {
        FinanceSavingsGoalEntity e = goals.findById(command.goalId()).orElseThrow(FinanceException.SavingsGoalNotFound::new);
        // The key of the space the row is stored in, whatever the command says.
        SpaceSealer sealer = sealers.forSpace(e.getSpaceId());
        e.setNameEncrypted(sealer.seal(FinanceSavingsGoalEntity.NAME, e.getId(), command.name()));
        e.setTargetAmountEncrypted(sealer.seal(FinanceSavingsGoalEntity.TARGET_AMOUNT, e.getId(), command.targetAmount().toPlainString()));
        e.setTargetDate(command.targetDate());
        e.setColor(command.color());
        e.setGlyph(command.glyph());
        FinanceSavingsGoalEntity saved = goals.saveAndFlush(e);
        return toDomain(saved, saved.getSpaceId());
    }

    @Override
    public void delete(UUID goalId) {
        goals.deleteById(goalId);
        goals.flush();
    }

    @Override
    public List<SavingsContribution> findContributionsByGoalId(UUID goalId) {
        FinanceSavingsGoalEntity goal = goals.findById(goalId).orElseThrow(FinanceException.SavingsGoalNotFound::new);
        SpaceSealer sealer = sealers.forSpace(goal.getSpaceId());
        return goalContributions.findByGoalIdOrderByContributedDateDesc(goalId).stream()
            .map(ce -> new SavingsContribution(ce.getId(), ce.getGoalId(), ce.getUserId(),
                new BigDecimal(sealer.open(FinanceSavingsContributionEntity.AMOUNT, ce.getId(), ce.getAmountEncrypted())), ce.getContributedDate()))
            .toList();
    }

    @Override
    public Map<UUID, List<SavingsContribution>> findContributionsByGoalIds(UUID spaceId, List<UUID> goalIds) {
        if (goalIds.isEmpty()) {
            return Map.of();
        }
        SpaceSealer sealer = sealers.forSpace(spaceId);
        return goalContributions.findByGoalIdInOrderByContributedDateDesc(goalIds).stream()
            .map(ce -> new SavingsContribution(ce.getId(), ce.getGoalId(), ce.getUserId(),
                new BigDecimal(sealer.open(FinanceSavingsContributionEntity.AMOUNT, ce.getId(), ce.getAmountEncrypted())), ce.getContributedDate()))
            .collect(Collectors.groupingBy(SavingsContribution::goalId));
    }

    @Override
    @Transactional
    public void lockForContribution(UUID goalId) {
        goals.lockContribution("finance-savings-contribution|" + goalId);
    }

    @Override
    @Transactional
    public SavingsContribution addContribution(AddSavingsContributionCommand command) {
        // The key of the goal's space, whatever the command says: the contribution is read under it.
        UUID spaceId = goals.findById(command.goalId()).map(FinanceSavingsGoalEntity::getSpaceId)
            .orElseThrow(FinanceException.SavingsGoalNotFound::new);
        SpaceSealer sealer = sealers.forSpace(spaceId);
        FinanceSavingsContributionEntity ce = new FinanceSavingsContributionEntity();
        ce.setGoalId(command.goalId());
        ce.setUserId(command.memberId());
        ce.setAmountEncrypted(sealer.seal(FinanceSavingsContributionEntity.AMOUNT, ce.getId(), command.amount().toPlainString()));
        ce.setContributedDate(command.date());
        FinanceSavingsContributionEntity saved = goalContributions.saveAndFlush(ce);
        return new SavingsContribution(saved.getId(), saved.getGoalId(), saved.getUserId(), command.amount(), saved.getContributedDate());
    }

    private SavingsGoal toDomain(FinanceSavingsGoalEntity e, UUID spaceId) {
        SpaceSealer sealer = sealers.forSpace(spaceId);
        return new SavingsGoal(e.getId(), e.getSpaceId(), sealer.open(FinanceSavingsGoalEntity.NAME, e.getId(), e.getNameEncrypted()),
            new BigDecimal(sealer.open(FinanceSavingsGoalEntity.TARGET_AMOUNT, e.getId(), e.getTargetAmountEncrypted())), e.getTargetDate(), e.getColor(), e.getGlyph());
    }
}
