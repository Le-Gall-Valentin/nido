package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.UpdateRecurringSeriesCommand;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.infrastructure.config.FinanceEncryptorFactory;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceRecurringSeriesContributorEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceRecurringSeriesEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceRecurringSeriesContributorJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceRecurringSeriesJpaRepository;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RecurringTransactionSeriesRepositoryAdapter implements RecurringTransactionSeriesRepository {

    private final FinanceRecurringSeriesJpaRepository series;
    private final FinanceRecurringSeriesContributorJpaRepository contributors;
    private final FinanceEncryptorFactory encryptorFactory;

    public RecurringTransactionSeriesRepositoryAdapter(
            FinanceRecurringSeriesJpaRepository series, FinanceRecurringSeriesContributorJpaRepository contributors,
            FinanceEncryptorFactory encryptorFactory) {
        this.series = series;
        this.contributors = contributors;
        this.encryptorFactory = encryptorFactory;
    }

    @Override
    public Optional<RecurringTransactionSeries> findById(UUID seriesId) {
        return series.findById(seriesId).map(e -> toDomain(e, contributors.findBySeriesId(e.getId())));
    }

    @Override
    public List<RecurringTransactionSeries> findBySpaceId(UUID spaceId) {
        return toDomainList(series.findBySpaceId(spaceId));
    }

    @Override
    public List<RecurringTransactionSeries> findActiveBySpaceId(UUID spaceId, LocalDate asOf) {
        return toDomainList(series.findActiveBySpaceId(spaceId, asOf));
    }

    /** Batches the contributor lookup into one query instead of one per series. */
    private List<RecurringTransactionSeries> toDomainList(List<FinanceRecurringSeriesEntity> entities) {
        List<UUID> ids = entities.stream().map(FinanceRecurringSeriesEntity::getId).toList();
        Map<UUID, List<FinanceRecurringSeriesContributorEntity>> bySeriesId = contributors.findBySeriesIdIn(ids).stream()
            .collect(Collectors.groupingBy(FinanceRecurringSeriesContributorEntity::getSeriesId));
        return entities.stream().map(e -> toDomain(e, bySeriesId.getOrDefault(e.getId(), List.of()))).toList();
    }

    @Override
    @Transactional
    public RecurringTransactionSeries create(CreateRecurringSeriesCommand command, List<Contribution> resolvedContributors) {
        TextEncryptor encryptor = encryptorFactory.forSpace(command.spaceId());
        FinanceRecurringSeriesEntity e = new FinanceRecurringSeriesEntity();
        e.setSpaceId(command.spaceId());
        e.setLabelEncrypted(encryptor.encrypt(command.label()));
        e.setAmountEncrypted(encryptor.encrypt(command.amount().toPlainString()));
        e.setType(command.type());
        e.setCategoryId(command.categoryId());
        e.setPayerId(command.payerId());
        e.setIntervalType(command.intervalType());
        e.setIntervalCount(command.intervalCount());
        e.setAnchorDate(command.anchorDate());
        e.setEndDate(command.endDate());
        FinanceRecurringSeriesEntity saved = series.saveAndFlush(e);
        saveContributors(saved.getId(), encryptor, resolvedContributors);
        return findById(saved.getId()).orElseThrow(FinanceException.RecurringSeriesNotFound::new);
    }

    @Override
    @Transactional
    public RecurringTransactionSeries update(UpdateRecurringSeriesCommand command, List<Contribution> resolvedContributors) {
        TextEncryptor encryptor = encryptorFactory.forSpace(command.spaceId());
        FinanceRecurringSeriesEntity e = series.findById(command.seriesId()).orElseThrow(FinanceException.RecurringSeriesNotFound::new);
        e.setLabelEncrypted(encryptor.encrypt(command.label()));
        e.setAmountEncrypted(encryptor.encrypt(command.amount().toPlainString()));
        e.setType(command.type());
        e.setCategoryId(command.categoryId());
        e.setPayerId(command.payerId());
        e.setIntervalType(command.intervalType());
        e.setIntervalCount(command.intervalCount());
        e.setAnchorDate(command.anchorDate());
        e.setEndDate(command.endDate());
        series.saveAndFlush(e);
        contributors.deleteBySeriesId(e.getId());
        contributors.flush();
        saveContributors(e.getId(), encryptor, resolvedContributors);
        return findById(e.getId()).orElseThrow(FinanceException.RecurringSeriesNotFound::new);
    }

    @Override
    public void delete(UUID seriesId) {
        series.deleteById(seriesId);
        series.flush();
    }

    @Override
    @Transactional
    public RecurringTransactionSeries advanceLastMaterializedDate(UUID seriesId, LocalDate newDate) {
        FinanceRecurringSeriesEntity e = series.findById(seriesId).orElseThrow(FinanceException.RecurringSeriesNotFound::new);
        e.setLastMaterializedDate(newDate);
        series.saveAndFlush(e);
        return findById(seriesId).orElseThrow(FinanceException.RecurringSeriesNotFound::new);
    }

    @Override
    @Transactional
    public void lockForMaterialization(UUID spaceId) {
        series.lockMaterialization("finance-materialize|" + spaceId);
    }

    private void saveContributors(UUID seriesId, TextEncryptor encryptor, List<Contribution> resolved) {
        for (Contribution c : resolved) {
            FinanceRecurringSeriesContributorEntity ce = new FinanceRecurringSeriesContributorEntity();
            ce.setSeriesId(seriesId);
            ce.setUserId(c.memberId());
            ce.setShareAmountEncrypted(encryptor.encrypt(c.shareAmount().toPlainString()));
            contributors.save(ce);
        }
        contributors.flush();
    }

    private RecurringTransactionSeries toDomain(FinanceRecurringSeriesEntity e, List<FinanceRecurringSeriesContributorEntity> contributorEntities) {
        TextEncryptor encryptor = encryptorFactory.forSpace(e.getSpaceId());
        List<Contribution> resolvedContributors = contributorEntities.stream()
            .map(ce -> new Contribution(ce.getUserId(), new BigDecimal(encryptor.decrypt(ce.getShareAmountEncrypted()))))
            .toList();
        return new RecurringTransactionSeries(e.getId(), e.getSpaceId(), encryptor.decrypt(e.getLabelEncrypted()),
            new BigDecimal(encryptor.decrypt(e.getAmountEncrypted())), e.getType(), e.getCategoryId(), e.getPayerId(),
            resolvedContributors, e.getIntervalType(), e.getIntervalCount(), e.getAnchorDate(), e.getEndDate(),
            e.getLastMaterializedDate());
    }
}
