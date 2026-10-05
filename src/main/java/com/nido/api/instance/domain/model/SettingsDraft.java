package com.nido.api.instance.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Values typed in a form, checked one by one, as changes to apply: a value to store, or a row to delete. */
public final class SettingsDraft {

    private final Map<SettingKey, Optional<String>> changes = new EnumMap<>(SettingKey.class);
    private final List<SettingProblem> problems = new ArrayList<>();

    /**
     * Blank clears the setting — back to its default — except a secret: a password field is always
     * shown empty, so leaving it empty must keep the stored password. Clearing one is {@link #clear}.
     * A required setting is never cleared: blank is a problem.
     */
    public SettingsDraft set(SettingKey key, String typed) {
        String value = typed == null ? "" : typed;
        if (value.isBlank()) {
            if (!key.secret()) {
                clear(key);
            }
            return this;
        }
        SettingRules.problem(key, value).ifPresentOrElse(
            code -> problems.add(new SettingProblem(key, code)),
            () -> changes.put(key, Optional.of(SettingRules.normalize(key, value))));
        return this;
    }

    public SettingsDraft clear(SettingKey key) {
        if (key.required()) {
            problems.add(new SettingProblem(key, SettingProblem.REQUIRED));
        } else {
            changes.put(key, Optional.empty());
        }
        return this;
    }

    public SettingsDraft problem(SettingProblem problem) {
        problems.add(problem);
        return this;
    }

    public Map<SettingKey, Optional<String>> changes() {
        return Collections.unmodifiableMap(changes);
    }

    public List<SettingProblem> problems() {
        return List.copyOf(problems);
    }

    public Map<SettingKey, String> applyTo(Map<SettingKey, String> stored) {
        Map<SettingKey, String> result = new EnumMap<>(SettingKey.class);
        result.putAll(stored);
        changes.forEach((key, value) -> value.ifPresentOrElse(v -> result.put(key, v), () -> result.remove(key)));
        return result;
    }
}
