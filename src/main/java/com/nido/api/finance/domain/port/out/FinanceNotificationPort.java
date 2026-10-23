package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.finance.domain.model.Involved;
import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.finance.domain.model.SeriesFacts;
import com.nido.api.space.domain.model.Addressee;

import java.util.List;

/**
 * What happens in the finances of a space, told to the members it concerns — on the channels they keep open, once
 * the change is committed. Names are resolved by the caller: this port only says, it never looks up.
 */
public interface FinanceNotificationPort {

    void transactionAdded(Gesture gesture, OperationFacts operation, List<Addressee> readers);

    void transactionUpdated(Gesture gesture, OperationFacts before, OperationFacts after, List<Involved> readers);

    void transactionDeleted(Gesture gesture, OperationFacts operation, List<Addressee> readers);

    void recurringCreated(Gesture gesture, SeriesFacts series, List<Addressee> readers);

    void recurringUpdated(Gesture gesture, SeriesFacts before, SeriesFacts after, List<Involved> readers);

    void recurringStopped(Gesture gesture, SeriesFacts series, List<Addressee> readers);
}
