package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionContributorEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionContributorJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.LedgerRow;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final FinanceTransactionJpaRepository transactions;
    private final FinanceTransactionContributorJpaRepository contributors;
    private final SpaceSealers sealers;
    private final MonthTotalsWriter monthTotalsWriter;

    public TransactionRepositoryAdapter(
            FinanceTransactionJpaRepository transactions, FinanceTransactionContributorJpaRepository contributors,
            SpaceSealers sealers, MonthTotalsWriter monthTotalsWriter) {
        this.transactions = transactions;
        this.contributors = contributors;
        this.sealers = sealers;
        this.monthTotalsWriter = monthTotalsWriter;
    }

    @Override
    public Optional<Transaction> findById(UUID transactionId) {
        return transactions.findById(transactionId).map(e -> toDomain(e, contributors.findByTransactionId(e.getId())));
    }

    @Override
    public List<Transaction> findBySpaceIdAndDateBetween(UUID spaceId, LocalDate from, LocalDate to) {
        return toDomainList(transactions.findBySpaceIdAndDateBetween(spaceId, from, to));
    }

    @Override
    public List<Transaction> findBySpaceIdAndMonth(UUID spaceId, YearMonth month) {
        List<FinanceTransactionEntity> found = transactions.findBySpaceIdAndDateBetween(
            spaceId, month.atDay(1), month.atEndOfMonth());
        return toDomainList(found);
    }

    @Override
    public List<Transaction> findAllBySpaceId(UUID spaceId) {
        return toDomainList(transactions.findBySpaceId(spaceId));
    }

    @Override
    @Transactional
    public Transaction create(CreateTransactionCommand command, List<Contribution> resolvedContributors) {
        SpaceSealer sealer = sealers.forSpace(command.spaceId());
        FinanceTransactionEntity e = new FinanceTransactionEntity();
        e.setSpaceId(command.spaceId());
        e.setLabelEncrypted(sealer.seal(FinanceTransactionEntity.LABEL, e.getId(), command.label()));
        e.setAmountEncrypted(SealedAmounts.seal(sealer, FinanceTransactionEntity.AMOUNT, e.getId(), command.amount()));
        e.setType(command.type());
        e.setCategoryId(command.categoryId());
        e.setDate(command.date());
        e.setPayerId(command.payerId());
        e.setRecurringSeriesId(command.recurringSeriesId());
        FinanceTransactionEntity saved = transactions.saveAndFlush(e);
        List<FinanceTransactionContributorEntity> savedContributors = saveContributors(saved.getId(), sealer, resolvedContributors);
        monthTotalsWriter.refresh(saved.getSpaceId(), List.of(YearMonth.from(saved.getDate())));
        return toDomain(saved, savedContributors);
    }

    @Override
    @Transactional
    public Transaction update(UpdateTransactionCommand command, List<Contribution> resolvedContributors) {
        FinanceTransactionEntity e = transactions.findById(command.transactionId()).orElseThrow(FinanceException.TransactionNotFound::new);
        // The key of the space the row is stored in, whatever the command says.
        SpaceSealer sealer = sealers.forSpace(e.getSpaceId());
        // Read before the date changes: an operation moved to another month leaves the first one.
        YearMonth previousMonth = YearMonth.from(e.getDate());
        e.setLabelEncrypted(sealer.seal(FinanceTransactionEntity.LABEL, e.getId(), command.label()));
        e.setAmountEncrypted(SealedAmounts.seal(sealer, FinanceTransactionEntity.AMOUNT, e.getId(), command.amount()));
        e.setType(command.type());
        e.setCategoryId(command.categoryId());
        e.setDate(command.date());
        e.setPayerId(command.payerId());
        FinanceTransactionEntity saved = transactions.saveAndFlush(e);
        contributors.deleteByTransactionId(e.getId());
        contributors.flush();
        List<FinanceTransactionContributorEntity> savedContributors = saveContributors(e.getId(), sealer, resolvedContributors);
        monthTotalsWriter.refresh(e.getSpaceId(), List.of(previousMonth, YearMonth.from(saved.getDate())));
        return toDomain(saved, savedContributors);
    }

    @Override
    @Transactional
    public void delete(UUID transactionId) {
        // A missing row is silently ignored, as deleteById did.
        transactions.findById(transactionId).ifPresent(e -> {
            transactions.delete(e);
            transactions.flush();
            monthTotalsWriter.refresh(e.getSpaceId(), List.of(YearMonth.from(e.getDate())));
        });
    }

    @Override
    public BigDecimal sumRecordedBetween(UUID spaceId, LocalDate from, LocalDate to) {
        SpaceSealer sealer = sealers.forSpace(spaceId);
        BigDecimal sum = BigDecimal.ZERO;
        for (LedgerRow row : transactions.findLedgerRows(spaceId, from, to)) {
            BigDecimal amount = SealedAmounts.open(sealer, FinanceTransactionEntity.AMOUNT, row.transactionId(), row.amountEncrypted());
            sum = sum.add(row.type() == TransactionType.INCOME ? amount : amount.negate());
        }
        return sum;
    }

    private List<FinanceTransactionContributorEntity> saveContributors(UUID transactionId, SpaceSealer sealer, List<Contribution> resolved) {
        List<FinanceTransactionContributorEntity> entities = resolved.stream().map(c -> {
            FinanceTransactionContributorEntity ce = new FinanceTransactionContributorEntity();
            ce.setTransactionId(transactionId);
            ce.setUserId(c.memberId());
            ce.setShareAmountEncrypted(SealedAmounts.seal(sealer, FinanceTransactionContributorEntity.SHARE_AMOUNT, ce.getId(), c.shareAmount()));
            return ce;
        }).toList();
        return contributors.saveAllAndFlush(entities);
    }

    private List<Transaction> toDomainList(List<FinanceTransactionEntity> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = found.stream().map(FinanceTransactionEntity::getId).toList();
        Map<UUID, List<FinanceTransactionContributorEntity>> contributorsByTransaction = contributors
            .findByTransactionIdInOrderByTransactionIdAsc(ids).stream()
            .collect(Collectors.groupingBy(FinanceTransactionContributorEntity::getTransactionId));
        return found.stream()
            .map(e -> toDomain(e, contributorsByTransaction.getOrDefault(e.getId(), List.of())))
            .toList();
    }

    private Transaction toDomain(FinanceTransactionEntity e, Collection<FinanceTransactionContributorEntity> contributorEntities) {
        SpaceSealer sealer = sealers.forSpace(e.getSpaceId());
        List<Contribution> resolvedContributors = contributorEntities.stream()
            .map(ce -> new Contribution(ce.getUserId(),
                SealedAmounts.open(sealer, FinanceTransactionContributorEntity.SHARE_AMOUNT, ce.getId(), ce.getShareAmountEncrypted())))
            .toList();
        return new Transaction(e.getId(), e.getSpaceId(), sealer.open(FinanceTransactionEntity.LABEL, e.getId(), e.getLabelEncrypted()),
            SealedAmounts.open(sealer, FinanceTransactionEntity.AMOUNT, e.getId(), e.getAmountEncrypted()), e.getType(), e.getCategoryId(), e.getDate(),
            e.getPayerId(), resolvedContributors, e.getRecurringSeriesId(), e.getCreatedAt());
    }
}
