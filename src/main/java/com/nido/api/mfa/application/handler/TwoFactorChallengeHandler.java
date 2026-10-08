package com.nido.api.mfa.application.handler;

import com.nido.api.mfa.application.method.TwoFactorMethods;
import com.nido.api.mfa.application.port.in.TwoFactorChallengeUseCase;
import com.nido.api.mfa.domain.model.CodeCheck;
import com.nido.api.mfa.domain.model.CodeDelivery;
import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@ApplicationService
public class TwoFactorChallengeHandler implements TwoFactorChallengeUseCase {

    private final TwoFactorMethods methods;
    private final TwoFactorMethodStorePort store;

    public TwoFactorChallengeHandler(TwoFactorMethods methods, TwoFactorMethodStorePort store) {
        this.methods = methods;
        this.store = store;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<TwoFactorMethod> usableMethods(UUID userId) {
        Set<TwoFactorMethod> usable = EnumSet.noneOf(TwoFactorMethod.class);
        store.activeMethods(userId).stream().filter(method -> methods.of(method).usableNow()).forEach(usable::add);
        return usable;
    }

    @Override
    @Transactional
    public CodeDelivery sendMailCode(UUID userId, String challengeId) {
        return methods.sender(TwoFactorMethod.MAIL)
            .map(mail -> mail.sendCode(userId, CodePurpose.LOGIN, challengeId))
            .orElseGet(CodeDelivery.Unavailable::new);
    }

    @Override
    @Transactional
    public CodeCheck verify(UUID userId, TwoFactorMethod method, String challengeId, String code) {
        return methods.of(method).check(userId, CodePurpose.LOGIN, challengeId, code);
    }
}
