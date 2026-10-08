package com.nido.api.mfa.infrastructure.mail;

import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.mfa.application.method.TwoFactorMethods;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.shared.model.TwoFactorMethod;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@MailIntegrationTestConfig
class TwoFactorCodeMailIT {

    @Autowired TwoFactorMethods methods;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcClient jdbc;

    private UUID jane;

    @BeforeEach
    void account() throws Exception {
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        jdbc.sql("DELETE FROM two_factor_mail_codes").update();
        jdbc.sql("DELETE FROM users WHERE username = 'code-jane'").update();
        jane = jdbc.sql("INSERT INTO users (username, email, role, language) VALUES ('code-jane', 'code-jane@example.fr', 'USER', 'fr') RETURNING id")
            .query(UUID.class).single();
    }

    @Test
    void the_code_in_the_mail_is_the_one_the_method_accepts() throws Exception {
        transactions.executeWithoutResult(status -> methods.of(TwoFactorMethod.MAIL).startEnrolment(jane));

        assertThat(SharedGreenMail.server().waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage mail = SharedGreenMail.server().getReceivedMessages()[0];
        assertThat(mail.getSubject()).isEqualTo("Confirmez votre adresse pour la double authentification");
        // The plain-text part puts the code on a line of its own.
        Matcher code = Pattern.compile("(?m)^(\\d{6})\\s*$").matcher(textOf(mail));
        assertThat(code.find()).isTrue();

        CodeCheck check = transactions.execute(status ->
            methods.of(TwoFactorMethod.MAIL).check(jane, CodePurpose.ENROL, jane.toString(), code.group(1)));
        assertThat(check).isEqualTo(CodeCheck.SUCCESS);
    }

    private static String textOf(Part part) throws Exception {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String text = textOf(multipart.getBodyPart(i));
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }
}
