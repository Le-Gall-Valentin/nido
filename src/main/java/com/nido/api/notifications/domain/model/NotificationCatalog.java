package com.nido.api.notifications.domain.model;

import com.nido.api.shared.model.Role;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Every kind of notification the application can send, and the kind of each notification class. Built
 * once, at startup, from the classes that declare themselves with {@link NotificationKind}; it never
 * changes afterwards.
 */
public final class NotificationCatalog {

    private final Map<Class<? extends Notification>, NotificationType> byClass;
    private final List<NotificationType> types;
    private final Map<NotificationType, Set<Role>> reservedTo;

    /** Every kind open to every account. */
    public NotificationCatalog(Map<Class<? extends Notification>, NotificationType> byClass) {
        this(byClass, Map.of());
    }

    /**
     * @param reservedTo the kinds only some roles may receive, with those roles; a kind absent from it is open
     *                   to every account
     */
    public NotificationCatalog(Map<Class<? extends Notification>, NotificationType> byClass,
                               Map<NotificationType, Set<Role>> reservedTo) {
        Map<NotificationType, Class<?>> declaredBy = new HashMap<>();
        byClass.forEach((notificationClass, type) -> {
            Class<?> other = declaredBy.putIfAbsent(type, notificationClass);
            if (other != null) {
                throw new IllegalStateException("Notification kind " + type.code() + " is declared by both "
                    + other.getName() + " and " + notificationClass.getName());
            }
        });
        this.byClass = Map.copyOf(byClass);
        this.types = byClass.values().stream()
            .sorted(Comparator.comparing(NotificationType::group).thenComparing(NotificationType::code))
            .toList();
        this.reservedTo = reservedTo.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    /** Whether an account of this role may receive the kind, see it on its card and switch it. */
    public boolean isOpenTo(NotificationType type, Role role) {
        Set<Role> roles = reservedTo.get(type);
        return roles == null || roles.contains(role);
    }

    /** The kinds an account of this role may receive, in the order of {@link #types()}. */
    public List<NotificationType> typesFor(Role role) {
        return types.stream().filter(type -> isOpenTo(type, role)).toList();
    }

    /** Grouped by context, then by code: the order of the preferences card. */
    public List<NotificationType> types() {
        return types;
    }

    public Optional<NotificationType> find(String code) {
        return types.stream().filter(type -> type.code().equals(code)).findFirst();
    }

    /** @throws IllegalStateException for a class the catalogue does not know — a declaration the startup check missed */
    public NotificationType typeOf(Class<? extends Notification> notificationClass) {
        NotificationType type = byClass.get(notificationClass);
        if (type == null) {
            throw new IllegalStateException("Not a catalogued notification: " + notificationClass.getName());
        }
        return type;
    }
}
