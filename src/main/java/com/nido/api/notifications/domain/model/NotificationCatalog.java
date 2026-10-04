package com.nido.api.notifications.domain.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every kind of notification the application can send, and the kind of each notification class. Built
 * once, at startup, from the classes that declare themselves with {@link NotificationKind}; it never
 * changes afterwards.
 */
public final class NotificationCatalog {

    private final Map<Class<? extends Notification>, NotificationType> byClass;
    private final List<NotificationType> types;

    public NotificationCatalog(Map<Class<? extends Notification>, NotificationType> byClass) {
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
