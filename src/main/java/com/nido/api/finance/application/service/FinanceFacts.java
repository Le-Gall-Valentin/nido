package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.SeriesFacts;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;

import java.util.List;
import java.util.UUID;

/**
 * What a mail tells about an operation or a recurring transaction: its category by label and its members by name.
 * A category that cannot be read leaves its label out rather than failing the gesture the mail is about.
 */
@ApplicationService
public class FinanceFacts {

    private final CategoryRepository categories;

    public FinanceFacts(CategoryRepository categories) {
        this.categories = categories;
    }

    public OperationFacts of(Transaction operation, Audience audience) {
        return new OperationFacts(operation.label(), operation.amount(), operation.type(), categoryLabel(operation.categoryId()),
            operation.date(), operation.payerId(), audience.nameOf(operation.payerId()),
            shares(operation.contributors(), audience));
    }

    public SeriesFacts of(RecurringTransactionSeries series, Audience audience) {
        return new SeriesFacts(series.label(), series.amount(), series.type(), categoryLabel(series.categoryId()),
            series.payerId(), audience.nameOf(series.payerId()), shares(series.contributors(), audience),
            series.intervalType(), series.intervalCount(), series.anchorDate(), series.endDate());
    }

    private String categoryLabel(UUID categoryId) {
        return categories.findById(categoryId).map(Category::label).orElse(null);
    }

    private static List<MemberShare> shares(List<Contribution> contributors, Audience audience) {
        return contributors.stream()
            .map(share -> new MemberShare(share.memberId(), audience.nameOf(share.memberId()), share.shareAmount()))
            .toList();
    }
}
