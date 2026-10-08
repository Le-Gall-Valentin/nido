package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.method.MailCodeIssuer;
import com.nido.api.mfa.application.port.in.AddressChangeCodeUseCase;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.port.out.TwoFactorMailPort;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@ApplicationService
public class AddressChangeCodeHandler implements AddressChangeCodeUseCase {

    private final TwoFactorMethodStorePort store;
    private final MailCodeIssuer issuer;
    private final TwoFactorMailPort mails;

    public AddressChangeCodeHandler(TwoFactorMethodStorePort store, MailCodeIssuer issuer, TwoFactorMailPort mails) {
        this.store = store;
        this.issuer = issuer;
        this.mails = mails;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean mailMethodOn(UUID userId) {
        return store.activeMethods(userId).contains(TwoFactorMethod.MAIL);
    }

    @Override
    @Transactional
    public CodeDelivery send(UUID userId, String newAddress) {
        return issuer.issue(userId, CodePurpose.EMAIL_CHANGE, newAddress, newAddress);
    }

    @Override
    @Transactional
    public CodeCheck check(UUID userId, String newAddress, String code) {
        return issuer.check(userId, CodePurpose.EMAIL_CHANGE, newAddress, code);
    }

    @Override
    @Transactional
    public void forgoMailMethod(UUID userId) {
        Set<TwoFactorMethod> active = store.activeMethods(userId);
        if (store.disable(userId, TwoFactorMethod.MAIL)) {
            issuer.forget(userId);
            // Mail is off when this happens, so this one is dropped like any other mail — the screen that made the
            // change says it instead. Asked all the same: whatever turns a method off tells its holder.
            mails.methodDisabled(userId, TwoFactorMethod.MAIL, active.size() > 1);
        }
    }
}
