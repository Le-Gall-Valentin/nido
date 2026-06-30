package com.synapsys.api;

import com.synapsys.api.shared.annotation.ApplicationService;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchRulesTest {

    private static final String BASE = "com.synapsys.api.";
    private static final List<String> BCS = List.of("authentication", "identity", "mfa");

    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.synapsys.api");

    private static DescribedPredicate<JavaClass> excludeTests() {
        return DescribedPredicate.describe("excluding tests",
            c -> !c.getSimpleName().endsWith("Test") && !c.getSimpleName().endsWith("IT"));
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
            .that().haveSimpleName("SynapsysProperties")
            .should().implement(com.synapsys.api.authentication.domain.port.out.RefreshTokenConfigPort.class)
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
                BASE + "mfa.infrastructure..")
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

        // identity.infra → authentication.application (port.in + dto)
        new CrossBcAppDep("identity",
            new String[]{BASE + "authentication.application.port.in..", BASE + "authentication.application.dto.."},
            Set.of("CredentialSetupAdapter", "CredentialChangeAdapter", "CredentialDeletionAdapter")),

        // identity.infra → mfa.application.port.in
        new CrossBcAppDep("identity",
            new String[]{BASE + "mfa.application.port.in.."},
            Set.of("TotpRecordInitAdapter", "MfaAdminResetTotpAdapter", "IdentityTotpStatusAdapter", "TotpDeletionAdapter"))
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