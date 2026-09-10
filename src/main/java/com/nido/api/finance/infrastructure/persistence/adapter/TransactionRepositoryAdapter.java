package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SplitTransaction;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.finance.infrastructure.config.FinanceEncryptorFactory;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionContributorEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionContributorJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.ContributorShareRow;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.SplitTransactionRow;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final FinanceEncryptorFactory encryptorFactory;

    public TransactionRepositoryAdapter(
            FinanceTransactionJpaRepository transactions, FinanceTransactionContributorJpaRepository contributors,
            FinanceEncryptorFactory encryptorFactory) {
        this.transactions = transactions;
        this.contributors = contributors;
        this.encryptorFactory = encryptorFactory;
    }

    @Override
    public Optional<Transaction> findById(UUID transactionId) {
        return transactions.findById(transactionId).map(e -> toDomain(e, contributors.findByTransactionId(e.getId())));
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
        // One encryptor for the space, and only the amounts pass through it: the labels stay
        // ciphertext in the database, unread.
        TextEncryptor encryptor = encryptorFactory.forSpace(spaceId);
        Map<UUID, List<ContributorShareRow>> sharesByTransaction = contributors
            .findShareRowsByTransactionIdIn(rows.stream().map(SplitTransactionRow::transactionId).toList())
            .stream()
            .collect(Collectors.groupingBy(ContributorShareRow::transactionId));
        return rows.stream()
            .map(row -> new SplitTransaction(
                row.payerId(),
                new BigDecimal(encryptor.decrypt(row.amountEncrypted())),
                sharesByTransaction.getOrDefault(row.transactionId(), List.of()).stream()
                    .map(share -> new Contribution(
                        share.userId(), new BigDecimal(encryptor.decrypt(share.shareAmountEncrypted()))))
                    .toList()))
            .toList();
    }

    @Override
    public List<Transaction> findAllBySpaceId(UUID spaceId) {
        return toDomainList(transactions.findBySpaceId(spaceId));
    }

    @Override
    @Transactional
    public Transaction create(CreateTransactionCommand command, List<Contribution> resolvedContributors) {
        TextEncryptor encryptor = encryptorFactory.forSpace(command.spaceId());
        FinanceTransactionEntity e = new FinanceTransactionEntity();
        e.setSpaceId(command.spaceId());
        e.setLabelEncrypted(encryptor.encrypt(command.label()));
        e.setAmountEncrypted(encryptor.encrypt(command.amount().toPlainString()));
        e.setType(command.type());
        e.setCategoryId(command.categoryId());
        e.setDate(command.date());
        e.setPayerId(command.payerId());
        e.setRecurringSeriesId(command.recurringSeriesId());
        FinanceTransactionEntity saved = transactions.saveAndFlush(e);
        List<FinanceTransactionContributorEntity> savedContributors = saveContributors(saved.getId(), encryptor, resolvedContributors);
        return toDomain(saved, savedContributors);
    }

    @Override
    @Transactional
    public Transaction update(UpdateTransactionCommand command, List<Contribution> resolvedContributors) {
        TextEncryptor encryptor = encryptorFactory.forSpace(command.spaceId());
        FinanceTransactionEntity e = transactions.findById(command.transactionId()).orElseThrow(FinanceException.TransactionNotFound::new);
        e.setLabelEncrypted(encryptor.encrypt(command.label()));
        e.setAmountEncrypted(encryptor.encrypt(command.amount().toPlainString()));
        e.setType(command.type());
        e.setCategoryId(command.categoryId());
        e.setDate(command.date());
        e.setPayerId(command.payerId());
        FinanceTransactionEntity saved = transactions.saveAndFlush(e);
        contributors.deleteByTransactionId(e.getId());
        contributors.flush();
        List<FinanceTransactionContributorEntity> savedContributors = saveContributors(e.getId(), encryptor, resolvedContributors);
        return toDomain(saved, savedContributors);
    }

    @Override
    public void delete(UUID transactionId) {
        transactions.deleteById(transactionId);
        transactions.flush();
    }

    private List<FinanceTransactionContributorEntity> saveContributors(UUID transactionId, TextEncryptor encryptor, List<Contribution> resolved) {
        List<FinanceTransactionContributorEntity> entities = resolved.stream().map(c -> {
            FinanceTransactionContributorEntity ce = new FinanceTransactionContributorEntity();
            ce.setTransactionId(transactionId);
            ce.setUserId(c.memberId());
            ce.setShareAmountEncrypted(encryptor.encrypt(c.shareAmount().toPlainString()));
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
        TextEncryptor encryptor = encryptorFactory.forSpace(e.getSpaceId());
        List<Contribution> resolvedContributors = contributorEntities.stream()
            .map(ce -> new Contribution(ce.getUserId(), new BigDecimal(encryptor.decrypt(ce.getShareAmountEncrypted()))))
            .toList();
        return new Transaction(e.getId(), e.getSpaceId(), encryptor.decrypt(e.getLabelEncrypted()),
            new BigDecimal(encryptor.decrypt(e.getAmountEncrypted())), e.getType(), e.getCategoryId(), e.getDate(),
            e.getPayerId(), resolvedContributors, e.getRecurringSeriesId(), e.getCreatedAt());
    }
}
