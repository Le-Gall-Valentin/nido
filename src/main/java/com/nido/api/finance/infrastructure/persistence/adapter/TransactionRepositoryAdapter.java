package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SplitTransaction;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionContributorEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionContributorJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.ContributorShareRow;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.SplitTransactionRow;
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

    public TransactionRepositoryAdapter(
            FinanceTransactionJpaRepository transactions, FinanceTransactionContributorJpaRepository contributors,
            SpaceSealers sealers) {
        this.transactions = transactions;
        this.contributors = contributors;
        this.sealers = sealers;
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
    public List<SplitTransaction> findSplitsBySpaceId(UUID spaceId) {
        List<SplitTransactionRow> rows = transactions.findSplitRowsBySpaceId(spaceId);
        if (rows.isEmpty()) {
            return List.of();
        }
        // One sealer for the space, and only the amounts pass through it: the labels stay
        // ciphertext in the database, unread.
        SpaceSealer sealer = sealers.forSpace(spaceId);
        Map<UUID, List<ContributorShareRow>> sharesByTransaction = contributors
            .findShareRowsByTransactionIdIn(rows.stream().map(SplitTransactionRow::transactionId).toList())
            .stream()
            .collect(Collectors.groupingBy(ContributorShareRow::transactionId));
        return rows.stream()
            .map(row -> new SplitTransaction(
                row.payerId(),
                new BigDecimal(sealer.open(FinanceTransactionEntity.AMOUNT, row.transactionId(), row.amountEncrypted())),
                sharesByTransaction.getOrDefault(row.transactionId(), List.of()).stream()
                    .map(share -> new Contribution(
                        share.userId(),
                        new BigDecimal(sealer.open(FinanceTransactionContributorEntity.SHARE_AMOUNT, share.id(), share.shareAmountEncrypted()))))
                    .toList(),
                row.type()))
            .toList();
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
        e.setAmountEncrypted(sealer.seal(FinanceTransactionEntity.AMOUNT, e.getId(), command.amount().toPlainString()));
        e.setType(command.type());
        e.setCategoryId(command.categoryId());
        e.setDate(command.date());
        e.setPayerId(command.payerId());
        e.setRecurringSeriesId(command.recurringSeriesId());
        FinanceTransactionEntity saved = transactions.saveAndFlush(e);
        List<FinanceTransactionContributorEntity> savedContributors = saveContributors(saved.getId(), sealer, resolvedContributors);
        return toDomain(saved, savedContributors);
    }

    @Override
    @Transactional
    public Transaction update(UpdateTransactionCommand command, List<Contribution> resolvedContributors) {
        FinanceTransactionEntity e = transactions.findById(command.transactionId()).orElseThrow(FinanceException.TransactionNotFound::new);
        // The key of the space the row is stored in, whatever the command says.
        SpaceSealer sealer = sealers.forSpace(e.getSpaceId());
        e.setLabelEncrypted(sealer.seal(FinanceTransactionEntity.LABEL, e.getId(), command.label()));
        e.setAmountEncrypted(sealer.seal(FinanceTransactionEntity.AMOUNT, e.getId(), command.amount().toPlainString()));
        e.setType(command.type());
        e.setCategoryId(command.categoryId());
        e.setDate(command.date());
        e.setPayerId(command.payerId());
        FinanceTransactionEntity saved = transactions.saveAndFlush(e);
        contributors.deleteByTransactionId(e.getId());
        contributors.flush();
        List<FinanceTransactionContributorEntity> savedContributors = saveContributors(e.getId(), sealer, resolvedContributors);
        return toDomain(saved, savedContributors);
    }

    @Override
    public void delete(UUID transactionId) {
        transactions.deleteById(transactionId);
        transactions.flush();
    }

    private List<FinanceTransactionContributorEntity> saveContributors(UUID transactionId, SpaceSealer sealer, List<Contribution> resolved) {
        List<FinanceTransactionContributorEntity> entities = resolved.stream().map(c -> {
            FinanceTransactionContributorEntity ce = new FinanceTransactionContributorEntity();
            ce.setTransactionId(transactionId);
            ce.setUserId(c.memberId());
            ce.setShareAmountEncrypted(sealer.seal(FinanceTransactionContributorEntity.SHARE_AMOUNT, ce.getId(), c.shareAmount().toPlainString()));
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
                new BigDecimal(sealer.open(FinanceTransactionContributorEntity.SHARE_AMOUNT, ce.getId(), ce.getShareAmountEncrypted()))))
            .toList();
        return new Transaction(e.getId(), e.getSpaceId(), sealer.open(FinanceTransactionEntity.LABEL, e.getId(), e.getLabelEncrypted()),
            new BigDecimal(sealer.open(FinanceTransactionEntity.AMOUNT, e.getId(), e.getAmountEncrypted())), e.getType(), e.getCategoryId(), e.getDate(),
            e.getPayerId(), resolvedContributors, e.getRecurringSeriesId(), e.getCreatedAt());
    }
}
