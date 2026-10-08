package com.nido.api.mfa.application.method;

import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.TwoFactorMethod;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Every method's implementation, by name. A method without exactly one implementation stops the start. */
@ApplicationService
public class TwoFactorMethods {

    private final Map<TwoFactorMethod, TwoFactorMethodHandler> handlers;

    public TwoFactorMethods(List<TwoFactorMethodHandler> all) {
        Map<TwoFactorMethod, TwoFactorMethodHandler> byMethod = new EnumMap<>(TwoFactorMethod.class);
        for (TwoFactorMethodHandler handler : all) {
            if (byMethod.put(handler.method(), handler) != null) {
                throw new IllegalStateException("Two implementations of the two-factor method " + handler.method());
            }
        }
        for (TwoFactorMethod method : TwoFactorMethod.values()) {
            if (!byMethod.containsKey(method)) {
                throw new IllegalStateException("No implementation of the two-factor method " + method);
            }
        }
        this.handlers = Collections.unmodifiableMap(byMethod);
    }

    public TwoFactorMethodHandler of(TwoFactorMethod method) {
        return handlers.get(method);
    }
}
