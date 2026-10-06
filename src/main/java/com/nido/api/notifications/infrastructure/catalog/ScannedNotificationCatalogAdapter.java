package com.nido.api.notifications.infrastructure.catalog;

import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationCatalog;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.notifications.domain.model.NotificationType;
import com.nido.api.notifications.domain.port.out.NotificationCatalogPort;
import com.nido.api.notifications.domain.port.out.NotificationChannelPort;
import com.nido.api.shared.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The catalogue, filled by discovery: at startup, every class of the application that implements
 * {@link Notification} is found and read for its {@link NotificationKind}. A sending context declares a
 * kind by writing its record — this context never lists them. The scan only reads annotations; it creates
 * no dependency from this context to the ones it finds.
 *
 * <p>The application refuses to start on a declaration that could only fail later: no notification at all, a
 * notification without its kind, a kind declared twice, a malformed code, or a notification no channel can
 * write. The kinds it found are logged at startup.
 */
@Component
public class ScannedNotificationCatalogAdapter implements NotificationCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(ScannedNotificationCatalogAdapter.class);

    private final NotificationCatalog catalog;

    @Autowired
    public ScannedNotificationCatalogAdapter(BeanFactory beanFactory, List<NotificationChannelPort> channels) {
        this(AutoConfigurationPackages.get(beanFactory), channels);
    }

    ScannedNotificationCatalogAdapter(List<String> basePackages, List<NotificationChannelPort> channels) {
        this.catalog = scan(basePackages, channels);
        List<String> codes = catalog.types().stream().map(NotificationType::code).toList();
        log.info("Notification kinds ({}): {}", codes.size(), codes);
    }

    @Override
    public NotificationCatalog catalog() {
        return catalog;
    }

    private static NotificationCatalog scan(List<String> basePackages, List<NotificationChannelPort> channels) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Notification.class));
        Map<Class<? extends Notification>, NotificationType> byClass = new HashMap<>();
        Map<NotificationType, Set<Role>> reservedTo = new HashMap<>();
        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                Class<? extends Notification> notificationClass = load(candidate.getBeanClassName());
                NotificationType type = declaredKind(notificationClass, channels);
                byClass.put(notificationClass, type);
                Role[] roles = notificationClass.getAnnotation(NotificationKind.class).roles();
                if (roles.length > 0) {
                    reservedTo.put(type, Set.copyOf(Arrays.asList(roles)));
                }
            }
        }
        if (byClass.isEmpty()) {
            // The application declares notifications, so finding none means the scan could not read its
            // classes: every notification would fail at its first use. Better not to start.
            throw new IllegalStateException("No notification found under " + basePackages
                + ": the scan could not read the application's classes");
        }
        return new NotificationCatalog(byClass, reservedTo);
    }

    private static NotificationType declaredKind(Class<? extends Notification> notificationClass,
                                                 List<NotificationChannelPort> channels) {
        NotificationKind kind = notificationClass.getAnnotation(NotificationKind.class);
        if (kind == null) {
            throw new IllegalStateException(notificationClass.getName()
                + " implements Notification without @NotificationKind: declare its kind, \"<context>.<name>\"");
        }
        NotificationType type;
        try {
            type = new NotificationType(kind.value());
        } catch (IllegalArgumentException malformed) {
            throw new IllegalStateException(notificationClass.getName() + ": " + malformed.getMessage(), malformed);
        }
        if (channels.stream().noneMatch(channel -> channel.supports(notificationClass))) {
            throw new IllegalStateException(notificationClass.getName()
                + " cannot travel on any channel: implement the content type of one (MailContent for mail)");
        }
        return type;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Notification> load(String className) {
        try {
            return (Class<? extends Notification>) ClassUtils.forName(className,
                ScannedNotificationCatalogAdapter.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(className, e);
        }
    }
}
