package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.MoveTransactionUseCase;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationService
public class MoveTransactionHandler implements MoveTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final ResolveMembershipUseCase resolveMembershipUseCase;

    public MoveTransactionHandler(TransactionRepository transactionRepository, ResolveMembershipUseCase resolveMembershipUseCase) {
        this.transactionRepository = transactionRepository;
        this.resolveMembershipUseCase = resolveMembershipUseCase;
    }

    @Override
    @Transactional
    public Transaction move(UUID transactionId, UUID destinationSpaceId, SpaceMembership caller) {
        caller.ensureCanWrite();
        if (destinationSpaceId.equals(caller.spaceId())) {
            throw new FinanceException.SameSpaceTransfer();
        }
        Transaction source = transactionRepository.findById(transactionId).orElseThrow(FinanceException.TransactionNotFound::new);
        if (!source.spaceId().equals(caller.spaceId())) {
            throw new FinanceException.TransactionNotFound();
        }
        SpaceMembership destination = resolveMembershipUseCase.resolve(destinationSpaceId, caller.userId());
        destination.ensureCanWrite();
        // Same shape as MoveTaskHandler: the copy starts with no payer/contributors and no
        // series link at the destination, regardless of the source's values.
        Transaction moved = transactionRepository.create(new CreateTransactionCommand(
            destinationSpaceId, source.label(), source.amount(), source.type(), source.categoryId(), source.date(),
            null, List.of(), null), List.of());
        transactionRepository.delete(transactionId);
        return moved;
    }
}
