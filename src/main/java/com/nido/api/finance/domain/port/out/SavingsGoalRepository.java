package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SavingsGoalRepository {
    Optional<SavingsGoal> findById(UUID goalId);
    List<SavingsGoal> findBySpaceId(UUID spaceId);
    SavingsGoal create(CreateSavingsGoalCommand command);
    SavingsGoal update(UpdateSavingsGoalCommand command);
    void delete(UUID goalId);
    List<SavingsContribution> findContributionsByGoalId(UUID goalId);
    /**
     * Batches the per-goal contribution lookup into one query — {@code goalIds} must all
     * belong to {@code spaceId} (the caller's responsibility), since a single encryptor for
     * that space is used to decrypt every contribution returned.
     */
    Map<UUID, List<SavingsContribution>> findContributionsByGoalIds(UUID spaceId, List<UUID> goalIds);
    SavingsContribution addContribution(AddSavingsContributionCommand command);
}
