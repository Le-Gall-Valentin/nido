package com.nido.api.infrastructure.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.infrastructure.sealing.SealedColumn;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.shared.security.StoredValueRejected;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;

class SealedValueRejectedHandlingTest {

    @Test
    void a_refused_value_answers_500_data_integrity_and_logs_its_place_never_its_value() {
        SealedColumn amount = SealedColumn.ofSpace("finance_transactions", "amount_encrypted");
        UUID row = UUID.randomUUID();
        SpaceSealer sealer = SpaceSealer.of(Encryptors.delux("test-encryption-secret-32chars!!", "00112233445566778899aabbccddeeff"));
        String sealedElsewhere = sealer.seal(amount, UUID.randomUUID(), "850.00");
        SealedValueRejected rejected = catchThrowableOfType(SealedValueRejected.class, () -> sealer.open(amount, row, sealedElsewhere));
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        logger.addAppender(logged);
        try {
            ResponseEntity<ProblemDetail> answer = new GlobalExceptionHandler(mock(AuthenticationEntryPoint.class),
                mock(AccessDeniedHandler.class)).handleRejectedValue(rejected, new MockHttpServletRequest("GET", "/api/x"));

            assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(answer.getBody().getProperties()).containsEntry("error_code", "data_integrity");
            assertThat(answer.getBody().getDetail()).doesNotContain("finance").doesNotContain("850");
            assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage)
                .singleElement().asString()
                .contains("finance_transactions.amount_encrypted", row.toString()).doesNotContain("850");
        } finally {
            logger.detachAppender(logged);
        }
    }

    @Test
    void every_stored_value_refused_is_answered_alike_whoever_refused_it() {
        // The web layer knows the shared kind of refusal, not the sealing that raises it today.
        StoredValueRejected refused = new StoredValueRejected("The value of x in row y does not decrypt") {};

        assertThat(new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class).resolveMethod(refused))
            .extracting(Method::getName).isEqualTo("handleRejectedValue");
    }
}
