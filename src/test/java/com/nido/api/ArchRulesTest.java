package com.nido.api;

import com.nido.api.authentication.domain.port.out.RefreshTokenConfigPort;
import com.nido.api.notifications.domain.model.Notification;
import com.nido.api.notifications.domain.model.NotificationKind;
import com.nido.api.shared.annotation.ApplicationService;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchRulesTest {

    private static final String BASE = "com.nido.api.";
    private static final List<String> BCS = List.of("authentication", "identity", "mfa", "space", "mail", "notifications", "instance");

    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.nido.api");

    private static DescribedPredicate<JavaClass> excludeTests() {
        // By name, and by where it was compiled: a test's helpers and anonymous classes are test code too.
        return DescribedPredicate.describe("excluding tests",
            c -> !c.getSimpleName().endsWith("Test") && !c.getSimpleName().endsWith("IT") && !compiledFromTests(c));
    }

    /** Where this very class was compiled: the test output, whatever the build tool calls it. */
    private static final Path TEST_OUTPUT = testOutput();

    private static Path testOutput() {
        try {
            return Path.of(ArchRulesTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean compiledFromTests(JavaClass c) {
        return c.getSource()
            .filter(source -> "file".equals(source.getUri().getScheme()))
            .map(source -> Path.of(source.getUri()).startsWith(TEST_OUTPUT))
            .orElse(false);
    }

    // -------------------------------------------------------------------------
    // Generic hexagonal architecture rules
    // -------------------------------------------------------------------------

    @Test
    void domain_should_not_depend_on_infrastructure() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .check(classes);
    }

    @Test
    void domain_should_not_depend_on_spring_framework() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .check(classes);
    }

    @Test
    void application_should_not_depend_on_infrastructure() {
        noClasses()
            .that().resideInAPackage("..application..")
            .and(excludeTests())
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .check(classes);
    }

    @Test
    void application_should_not_depend_on_spring_except_transactions() {
        // The application layer may use Spring's transaction API (declarative @Transactional and
        // programmatic transaction support such as TransactionSynchronization for after-commit hooks),
        // but nothing else from Spring.
        DescribedPredicate<JavaClass> springExceptTransactions = DescribedPredicate.describe(
            "reside in org.springframework.. but not org.springframework.transaction..",
            clazz -> clazz.getPackageName().startsWith("org.springframework.")
                     && !clazz.getPackageName().startsWith("org.springframework.transaction")
        );
        noClasses()
            .that().resideInAPackage("..application..")
            .and(excludeTests())
            .should().dependOnClassesThat(springExceptTransactions)
            .check(classes);
    }

    @Test
    void web_layer_should_not_instantiate_handler_implementations() {
        noClasses()
            .that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application.handler..")
            .check(classes);
    }

    @Test
    void jpa_entities_should_not_reside_in_domain_or_application() {
        noClasses()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..domain..")
            .orShould().resideInAPackage("..application..")
            .check(classes);
    }

    @Test
    void jpa_entities_should_reside_in_persistence_entity_package() {
        classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.persistence.entity..")
            .check(classes);
    }

    @Test
    void application_services_should_reside_in_an_application_package() {
        classes()
            .that().areAnnotatedWith(ApplicationService.class)
            .should().resideInAPackage("..application..")
            .check(classes);
    }

    @Test
    void ports_in_should_be_interfaces() {
        classes()
            .that().resideInAPackage("..application.port.in..")
            .should().beInterfaces()
            .check(classes);
    }

    @Test
    void ports_out_should_be_interfaces() {
        classes()
            .that().resideInAPackage("..domain.port.out..")
            .and(excludeTests())
            .should().beInterfaces()
            .check(classes);
    }

    @Test
    void outbound_adapters_should_implement_a_domain_port() {
        classes()
            .that().resideInAPackage("..infrastructure..")
            .and().haveSimpleNameEndingWith("Adapter")
            .and().areAnnotatedWith(org.springframework.stereotype.Component.class)
            .should().implement(DescribedPredicate.describe(
                "a port in ..domain.port.out..",
                iface -> iface.getPackageName().contains(".domain.port.out")
            ))
            .check(classes);
    }

    @Test
    void configuration_classes_should_not_implement_domain_ports() {
        noClasses()
            .that().haveSimpleName("NidoProperties")
            .should().implement(RefreshTokenConfigPort.class)
            .check(classes);
    }

    // -------------------------------------------------------------------------
    // BC isolation — domain + application layer
    // Each BC's domain and application must not depend on any other BC at all.
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0}: domain+application must not depend on other BCs")
    @MethodSource("bcIsolationSource")
    void each_bc_domain_and_application_should_not_depend_on_other_bcs(
            String bc, String[] otherBcPackages) {
        noClasses()
            .that().resideInAPackage(BASE + bc + ".domain..")
            .or().resideInAPackage(BASE + bc + ".application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(otherBcPackages)
            .allowEmptyShould(false)
            .check(classes);
    }

    static Stream<Arguments> bcIsolationSource() {
        return BCS.stream().map(bc -> {
            String[] others = BCS.stream()
                .filter(b -> !b.equals(bc))
                .map(b -> BASE + b + "..")
                .toArray(String[]::new);
            return Arguments.of(bc, others);
        });
    }

    // -------------------------------------------------------------------------
    // BC isolation — infrastructure layer
    // No BC's infra may depend on another BC's infra.
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0}: infrastructure must not depend on other BCs infrastructure")
    @MethodSource("bcInfraIsolationSource")
    void each_bc_infrastructure_should_not_depend_on_other_bc_infrastructure(
            String bc, String[] otherBcInfraPackages) {
        noClasses()
            .that().resideInAPackage(BASE + bc + ".infrastructure..")
            .and(excludeTests())
            .should().dependOnClassesThat()
            .resideInAnyPackage(otherBcInfraPackages)
            .allowEmptyShould(false)
            .check(classes);
    }

    static Stream<Arguments> bcInfraIsolationSource() {
        return BCS.stream().map(bc -> {
            String[] others = BCS.stream()
                .filter(b -> !b.equals(bc))
                .map(b -> BASE + b + ".infrastructure..")
                .toArray(String[]::new);
            return Arguments.of(bc, others);
        });
    }

    // -------------------------------------------------------------------------
    // Scoped module isolation
    //
    // None of these belong in BCS above: every one of them legitimately depends on
    // space.domain / space.application, because SpaceMembership is the caller type
    // threaded through nearly all of their handlers, and the generic rule would
    // forbid it. What still has to hold is that they do not reach for each other.
    //
    // They do not today, including where it would have been the easy thing to do:
    // a shopping list is computed from the menu by ComputeShoppingListHandler, which
    // lives in kitchen and uses kitchen's own repositories, and the resulting lines
    // are handed to shopping as a command. Neither module names the other. This rule
    // is what keeps that true — kitchen and shopping had no rule at all until now,
    // so nothing but habit was stopping the shortcut.
    // -------------------------------------------------------------------------

    private static final List<String> SCOPED_MODULES = List.of("finance", "tasks", "kitchen", "shopping");

    @ParameterizedTest(name = "{0}: must not depend on another scoped module")
    @MethodSource("scopedModuleIsolationSource")
    void each_scoped_module_should_not_depend_on_another_scoped_module(
            String module, String[] otherModulePackages) {
        noClasses()
            .that().resideInAPackage(BASE + module + "..")
            .and(excludeTests())
            .should().dependOnClassesThat()
            .resideInAnyPackage(otherModulePackages)
            // Fails if the module matched nothing, so a renamed or misspelt package cannot
            // turn this into a rule that passes by looking at an empty set.
            .allowEmptyShould(false)
            .check(classes);
    }

    static Stream<Arguments> scopedModuleIsolationSource() {
        return SCOPED_MODULES.stream().map(module -> {
            String[] others = SCOPED_MODULES.stream()
                .filter(other -> !other.equals(module))
                .map(other -> BASE + other + "..")
                .toArray(String[]::new);
            return Arguments.of(module, others);
        });
    }

    // -------------------------------------------------------------------------
    // The calendar reads the scoped modules — from its sources, and one way only
    //
    // Every source of the calendar lives in calendar.infrastructure.source and calls the inbound use
    // cases of the module it reads. None of those modules may learn the calendar exists; the rest of
    // the calendar may not name them; and even the sources may reach only what a module publishes —
    // its use cases and the domain types they return — never its persistence, web layer or handlers.
    // -------------------------------------------------------------------------

    private static final String CALENDAR = BASE + "calendar..";
    private static final String CALENDAR_SOURCES = BASE + "calendar.infrastructure.source..";

    private static String[] scopedModulePackages() {
        return SCOPED_MODULES.stream().map(module -> BASE + module + "..").toArray(String[]::new);
    }

    @Test
    void no_module_the_calendar_reads_depends_on_the_calendar() {
        noClasses()
            .that().resideInAnyPackage(scopedModulePackages())
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAPackage(CALENDAR)
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void the_calendar_names_the_modules_it_reads_only_from_its_sources() {
        noClasses()
            .that().resideInAPackage(CALENDAR)
            .and().resideOutsideOfPackage(CALENDAR_SOURCES)
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAnyPackage(scopedModulePackages())
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void the_calendar_sources_use_only_what_the_modules_they_read_publish() {
        DescribedPredicate<JavaClass> unpublished = DescribedPredicate.describe(
            "a scoped module's class outside its application.port.in and domain.model",
            c -> SCOPED_MODULES.stream().anyMatch(module -> c.getPackageName().startsWith(BASE + module + "."))
                && !c.getPackageName().contains(".application.port.in")
                && !c.getPackageName().contains(".domain.model"));
        noClasses()
            .that().resideInAPackage(CALENDAR_SOURCES)
            .and(excludeTests())
            .should().dependOnClassesThat(unpublished)
            .allowEmptyShould(false)
            .check(classes);
    }

    // -------------------------------------------------------------------------
    // The dashboard reads the scoped modules and the calendar — from its sources, one way only
    //
    // Same contract as the calendar's: none of the modules it reads may learn it exists, the rest of
    // the dashboard may not name them, and its sources reach only what a module publishes. It reads
    // members' names on the client, so it has no business with identity at all.
    // -------------------------------------------------------------------------

    private static final String DASHBOARD = BASE + "dashboard..";
    private static final String DASHBOARD_SOURCES = BASE + "dashboard.infrastructure.source..";
    private static final List<String> MODULES_THE_DASHBOARD_READS =
        Stream.concat(SCOPED_MODULES.stream(), Stream.of("calendar")).toList();

    private static String[] modulesTheDashboardReads() {
        return MODULES_THE_DASHBOARD_READS.stream().map(module -> BASE + module + "..").toArray(String[]::new);
    }

    @Test
    void no_module_the_dashboard_reads_depends_on_the_dashboard() {
        noClasses()
            .that().resideInAnyPackage(modulesTheDashboardReads())
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAPackage(DASHBOARD)
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void the_dashboard_names_the_modules_it_reads_only_from_its_sources() {
        noClasses()
            .that().resideInAPackage(DASHBOARD)
            .and().resideOutsideOfPackage(DASHBOARD_SOURCES)
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAnyPackage(modulesTheDashboardReads())
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void the_dashboard_sources_use_only_what_the_modules_they_read_publish() {
        DescribedPredicate<JavaClass> unpublished = DescribedPredicate.describe(
            "a read module's class outside its application.port.in and domain.model",
            c -> MODULES_THE_DASHBOARD_READS.stream().anyMatch(module -> c.getPackageName().startsWith(BASE + module + "."))
                && !c.getPackageName().contains(".application.port.in")
                && !c.getPackageName().contains(".domain.model"));
        noClasses()
            .that().resideInAPackage(DASHBOARD_SOURCES)
            .and(excludeTests())
            .should().dependOnClassesThat(unpublished)
            .allowEmptyShould(false)
            .check(classes);
    }

    // The space module is what every module stands on — the caller's membership, the space's today, the
    // invitations of an account — so the whole dashboard may use it, not only its sources. Only what it
    // publishes, though, and without it ever learning the dashboard exists.
    @Test
    void the_dashboard_uses_only_what_the_space_module_publishes() {
        DescribedPredicate<JavaClass> unpublished = DescribedPredicate.describe(
            "a space class outside its application.port.in and domain.model",
            c -> c.getPackageName().startsWith(BASE + "space.")
                && !c.getPackageName().startsWith(BASE + "space.application.port.in")
                && !c.getPackageName().startsWith(BASE + "space.domain.model"));
        noClasses()
            .that().resideInAPackage(DASHBOARD)
            .and(excludeTests())
            .should().dependOnClassesThat(unpublished)
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void the_space_module_does_not_depend_on_the_dashboard() {
        noClasses()
            .that().resideInAPackage(BASE + "space..")
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAPackage(DASHBOARD)
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void the_dashboard_depends_on_no_identity_class() {
        noClasses()
            .that().resideInAPackage(DASHBOARD)
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAPackage(BASE + "identity..")
            .allowEmptyShould(false)
            .check(classes);
    }

    // -------------------------------------------------------------------------
    // Mail — a channel any context may use, through what it publishes and nothing else
    //
    // A context sends a mail from an adapter in its own infrastructure, with a record of its own
    // implementing MailContent. Its domain and application never learn mail exists; and nobody but
    // mail's infrastructure touches an SMTP, template or HTML library.
    // -------------------------------------------------------------------------

    private static final String MAIL = BASE + "mail..";

    @Test
    void only_the_mail_infrastructure_touches_mail_libraries() {
        noClasses()
            .that().resideOutsideOfPackage(BASE + "mail.infrastructure..")
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAnyPackage(
                "jakarta.mail..", "org.springframework.mail..", "org.thymeleaf..", "org.jsoup..")
            .check(classes);
    }

    @Test
    void outside_mail_only_what_mail_publishes_is_used() {
        DescribedPredicate<JavaClass> unpublished = DescribedPredicate.describe(
            "a mail class outside its application.port.in and domain.model",
            c -> c.getPackageName().startsWith(BASE + "mail.")
                && !c.getPackageName().startsWith(BASE + "mail.application.port.in")
                && !c.getPackageName().startsWith(BASE + "mail.domain.model"));
        noClasses()
            .that().resideOutsideOfPackage(MAIL)
            .and(excludeTests())
            .should().dependOnClassesThat(unpublished)
            .check(classes);
    }

    @Test
    void outside_mail_only_an_infrastructure_adapter_names_mail() {
        noClasses()
            .that().resideOutsideOfPackage(MAIL)
            .and().resideOutsideOfPackage("..infrastructure..")
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAPackage(MAIL)
            .check(classes);
    }

    // -------------------------------------------------------------------------
    // Notifications — a context any other may use, through what it publishes
    //
    // A context notifies from an adapter in its own infrastructure, with a record of its own implementing
    // Notification. Its domain and application never learn notifications exist, and nobody reaches the
    // handlers, the catalogue scan or the channels.
    // -------------------------------------------------------------------------

    private static final String NOTIFICATIONS = BASE + "notifications..";

    @Test
    void outside_notifications_only_what_notifications_publishes_is_used() {
        DescribedPredicate<JavaClass> unpublished = DescribedPredicate.describe(
            "a notifications class outside its application.port.in and domain.model",
            c -> c.getPackageName().startsWith(BASE + "notifications.")
                && !c.getPackageName().startsWith(BASE + "notifications.application.port.in")
                && !c.getPackageName().startsWith(BASE + "notifications.domain.model"));
        noClasses()
            .that().resideOutsideOfPackage(NOTIFICATIONS)
            .and(excludeTests())
            .should().dependOnClassesThat(unpublished)
            .check(classes);
    }

    @Test
    void outside_notifications_only_an_infrastructure_adapter_names_notifications() {
        noClasses()
            .that().resideOutsideOfPackage(NOTIFICATIONS)
            .and().resideOutsideOfPackage("..infrastructure..")
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAPackage(NOTIFICATIONS)
            .check(classes);
    }

    @Test
    void a_notification_declares_its_kind_and_lives_in_the_infrastructure_of_its_context() {
        classes()
            .that().implement(Notification.class)
            .and(excludeTests())
            .should().beAnnotatedWith(NotificationKind.class)
            .andShould().resideInAPackage("..infrastructure..")
            .check(classes);
    }

    @Test
    void only_a_notification_declares_a_notification_kind() {
        classes()
            .that().areAnnotatedWith(NotificationKind.class)
            .should().implement(Notification.class)
            .check(classes);
    }

    // -------------------------------------------------------------------------
    // Instance — the installation's settings, setup and key; read by others through what it publishes
    // -------------------------------------------------------------------------

    private static final String INSTANCE = BASE + "instance..";

    @Test
    void outside_instance_only_what_instance_publishes_is_used() {
        DescribedPredicate<JavaClass> unpublished = DescribedPredicate.describe(
            "an instance class outside its application.port.in and domain.model",
            c -> c.getPackageName().startsWith(BASE + "instance.")
                && !c.getPackageName().startsWith(BASE + "instance.application.port.in")
                && !c.getPackageName().startsWith(BASE + "instance.domain.model"));
        noClasses()
            .that().resideOutsideOfPackage(INSTANCE)
            .and(excludeTests())
            .should().dependOnClassesThat(unpublished)
            .check(classes);
    }

    @Test
    void outside_instance_only_an_infrastructure_adapter_names_instance() {
        noClasses()
            .that().resideOutsideOfPackage(INSTANCE)
            .and().resideOutsideOfPackage("..infrastructure..")
            .and(excludeTests())
            .should().dependOnClassesThat().resideInAPackage(INSTANCE)
            .check(classes);
    }

    // -------------------------------------------------------------------------
    // Global infra isolation
    // -------------------------------------------------------------------------

    @Test
    void global_infrastructure_should_not_depend_on_bc_infrastructure() {
        noClasses()
            .that().resideInAPackage(BASE + "infrastructure..")
            .and(DescribedPredicate.describe("excluding tests and SecurityConfig",
                c -> !c.getSimpleName().endsWith("Test")
                  && !c.getSimpleName().endsWith("IT")
                  && !c.getSimpleName().equals("SecurityConfig")))
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                BASE + "authentication.infrastructure..",
                BASE + "identity.infrastructure..",
                BASE + "mfa.infrastructure..",
                BASE + "mail.infrastructure..",
                BASE + "notifications.infrastructure..",
                BASE + "instance.infrastructure..")
            .allowEmptyShould(false)
            .check(classes);
    }

    // -------------------------------------------------------------------------
    // Cross-BC application layer dependencies — whitelisted adapters only
    //
    // BC infra adapters may bridge to another BC's application layer ONLY when
    // explicitly listed here. Any unlisted class touching a foreign application
    // package will break the build.
    //
    // Uses getName() (not getSimpleName()) to also catch JVM-generated synthetic
    // inner classes (e.g. MfaTotpVerifierAdapter$1 from switch expressions).
    // -------------------------------------------------------------------------

    /**
     * Declares a permitted cross-BC infra → application dependency.
     *
     * @param sourceBc       the BC whose infrastructure contains the adapter
     * @param targetPackages the foreign application package(s) the adapter may use
     * @param allowedAdapters the adapter class names (and their synthetic inner classes) allowed
     */
    private record CrossBcAppDep(String sourceBc, String[] targetPackages, Set<String> allowedAdapters) {}

    private static final List<CrossBcAppDep> CROSS_BC_APP_DEPS = List.of(
        // authentication.infra → identity.application.port.in
        new CrossBcAppDep("authentication",
            new String[]{BASE + "identity.application.port.in.."},
            Set.of("UserProfileAdapter")),

        // authentication.infra → mfa.application (port.in + dto)
        new CrossBcAppDep("authentication",
            new String[]{BASE + "mfa.application.port.in..", BASE + "mfa.application.dto.."},
            Set.of("TotpStatusAdapter", "MfaTotpVerifierAdapter")),

        // authentication.infra → mail.application.port.in
        new CrossBcAppDep("authentication",
            new String[]{BASE + "mail.application.port.in.."},
            Set.of("AccountMailAdapter")),

        // identity.infra → authentication.application (port.in + dto)
        new CrossBcAppDep("identity",
            new String[]{BASE + "authentication.application.port.in..", BASE + "authentication.application.dto.."},
            Set.of("CredentialSetupAdapter", "CredentialChangeAdapter", "CredentialDeletionAdapter",
                   "TokenInvalidationAdapter", "PasswordCheckAdapter", "AccountRecoveryAdapter")),

        // identity.infra → mail.application.port.in
        new CrossBcAppDep("identity",
            new String[]{BASE + "mail.application.port.in.."},
            Set.of("ProfileMailAdapter", "PendingMailCancellationAdapter")),

        // identity.infra → mfa.application.port.in
        new CrossBcAppDep("identity",
            new String[]{BASE + "mfa.application.port.in.."},
            Set.of("TotpRecordInitAdapter", "MfaAdminResetTotpAdapter", "IdentityTotpStatusAdapter", "TotpDeletionAdapter")),

        // identity.infra → space.application.port.in
        new CrossBcAppDep("identity",
            new String[]{BASE + "space.application.port.in.."},
            Set.of("PersonalSpaceInitAdapter", "SpaceDataDeletionAdapter")),

        // identity.infra → notifications.application.port.in
        new CrossBcAppDep("identity",
            new String[]{BASE + "notifications.application.port.in.."},
            Set.of("NotificationDataDeletionAdapter")),

        // notifications.infra → mail.application.port.in
        new CrossBcAppDep("notifications",
            new String[]{BASE + "mail.application.port.in.."},
            Set.of("MailChannelAdapter")),

        // notifications.infra → identity.application.port.in
        new CrossBcAppDep("notifications",
            new String[]{BASE + "identity.application.port.in.."},
            Set.of("NotificationRecipientAdapter")),

        // space.infra → identity.application.port.in
        new CrossBcAppDep("space",
            new String[]{BASE + "identity.application.port.in.."},
            Set.of("MemberProfileAdapter")),

        // space.infra → notifications.application.port.in
        new CrossBcAppDep("space",
            new String[]{BASE + "notifications.application.port.in.."},
            Set.of("SpaceNotificationAdapter")),

        // mail.infra → instance.application.port.in
        new CrossBcAppDep("mail",
            new String[]{BASE + "instance.application.port.in.."},
            Set.of("MailConfigurationAdapter")),

        // authentication.infra → instance.application.port.in
        new CrossBcAppDep("authentication",
            new String[]{BASE + "instance.application.port.in.."},
            Set.of("SessionSettingsAdapter"))
    );

    @ParameterizedTest(name = "{0}.infra → {1}: only whitelisted adapters allowed")
    @MethodSource("crossBcAppDepSource")
    void only_whitelisted_adapters_may_depend_on_other_bc_application_layer(
            String sourceBc, String targetDesc, String[] targetPackages, Set<String> allowedAdapters) {
        DescribedPredicate<JavaClass> excludeAllowed = DescribedPredicate.describe(
            "excluding whitelisted adapters and their synthetic inner classes",
            c -> allowedAdapters.stream().noneMatch(name -> c.getName().contains(name)));
        noClasses()
            .that().resideInAPackage(BASE + sourceBc + ".infrastructure..")
            .and(excludeTests())
            .and(excludeAllowed)
            .should().dependOnClassesThat()
            .resideInAnyPackage(targetPackages)
            .allowEmptyShould(false)
            .check(classes);
    }

    static Stream<Arguments> crossBcAppDepSource() {
        return CROSS_BC_APP_DEPS.stream().map(dep -> Arguments.of(
            dep.sourceBc(),
            String.join(", ", dep.targetPackages()),
            dep.targetPackages(),
            dep.allowedAdapters()
        ));
    }

    // -------------------------------------------------------------------------
    // Specific guards — kept explicit because they address known past violations
    // or unique concerns not covered by the generic rules above.
    // -------------------------------------------------------------------------

    @Test
    void authentication_infrastructure_should_not_depend_on_mfa_concrete_services() {
        // Adapters may use mfa.application.port.in (interfaces) but never mfa.application.service
        // (concrete implementations) — doing so would bypass the port abstraction entirely.
        noClasses()
            .that().resideInAPackage(BASE + "authentication.infrastructure..")
            .and(excludeTests())
            .should().dependOnClassesThat()
            .resideInAPackage(BASE + "mfa.application.service..")
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void identity_infrastructure_should_not_depend_on_authentication_application_handlers() {
        noClasses()
            .that().resideInAPackage(BASE + "identity.infrastructure..")
            .should().dependOnClassesThat()
            .resideInAPackage(BASE + "authentication.application.handler..")
            .allowEmptyShould(false)
            .check(classes);
    }

    @Test
    void identity_infrastructure_should_not_depend_on_authentication_domain() {
        noClasses()
            .that().resideInAPackage(BASE + "identity.infrastructure..")
            .should().dependOnClassesThat()
            .resideInAPackage(BASE + "authentication.domain..")
            .allowEmptyShould(false)
            .check(classes);
    }
}