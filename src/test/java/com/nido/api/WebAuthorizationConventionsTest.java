package com.nido.api;

import com.nido.api.infrastructure.web.CurrentMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The web-layer counterpart of {@link ArchRulesTest}: a convention that holds for routes that do
 * not exist yet.
 *
 * <p>{@code @CurrentMembership} can declare the role a route requires, and
 * {@code SpaceMembershipArgumentResolver} enforces it before the handler is ever entered. Until
 * this test existed, <b>not one of the 71 routes declared it</b>, so every route asked only for
 * VIEWER and the entire protection of a write rested on its handler remembering to call
 * {@code ensureCanWrite()}. They all did — that was checked one by one — which is exactly the
 * problem: nothing made the next one do it.
 *
 * <p>Annotating the routes without this test would have been decoration: a route added tomorrow
 * can forget {@code min} as easily as it can forget the handler guard. The rule is what makes the
 * declaration stick, so it is the actual fix.
 *
 * <p>The handler guards stay where they are. This floor is a filter at the edge, not a
 * replacement: it knows the caller's role and nothing else, while a handler also checks that the
 * membership matches the space it was handed and that the resource belongs to it. A route whose
 * floor is right and whose handler guard was deleted would still be wrong.
 */
class WebAuthorizationConventionsTest {

    private static final List<Class<? extends Annotation>> WRITE_MAPPINGS =
        List.of(PostMapping.class, PutMapping.class, PatchMapping.class, DeleteMapping.class);

    /**
     * Writes a VIEWER may legitimately reach, each for its own reason.
     *
     * <p>Leaving a space is the one write nobody needs permission for: a read-only member who wants
     * out would otherwise have to ask an admin to remove them.
     *
     * <p>Copying a recipe writes into the <b>destination</b> space, not into the {@code spaceId} of
     * the route — which it only reads. {@code CopyRecipeHandler} checks write access on the
     * destination membership instead, so a floor here would refuse a VIEWER copying a shared recipe
     * into their own space. This is the case that a floor derived from "does the handler call
     * ensureCanWrite" gets wrong: the guard is real, it just governs a different membership.
     */
    private static final Set<String> VIEWER_MAY_WRITE = Set.of(
        "SpaceController#leave",
        "RecipeController#copy");

    private record Route(String name, SpaceRole floor) {}

    private static List<Route> scopedWriteRoutes() {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return scanner.findCandidateComponents("com.nido.api").stream()
            .map(BeanDefinition::getBeanClassName)
            .map(WebAuthorizationConventionsTest::load)
            .flatMap(controller -> java.util.Arrays.stream(controller.getDeclaredMethods())
                .filter(WebAuthorizationConventionsTest::isWrite)
                .flatMap(method -> membershipFloor(method)
                    .map(floor -> new Route(controller.getSimpleName() + "#" + method.getName(), floor))
                    .stream()))
            .toList();
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(className, e);
        }
    }

    private static boolean isWrite(Method method) {
        return WRITE_MAPPINGS.stream().anyMatch(mapping -> method.isAnnotationPresent(mapping));
    }

    private static java.util.Optional<SpaceRole> membershipFloor(Method method) {
        for (Parameter parameter : method.getParameters()) {
            CurrentMembership annotation = parameter.getAnnotation(CurrentMembership.class);
            if (annotation != null) {
                return java.util.Optional.of(annotation.min());
            }
        }
        return java.util.Optional.empty();
    }

    @Test
    void every_route_that_writes_inside_a_space_declares_the_role_it_requires() {
        Set<String> viewerCanReach = new TreeSet<>();
        for (Route route : scopedWriteRoutes()) {
            if (!route.floor().atLeast(SpaceRole.MEMBER) && !VIEWER_MAY_WRITE.contains(route.name())) {
                viewerCanReach.add(route.name());
            }
        }

        assertThat(viewerCanReach)
            .as("these routes write inside a space but ask for no role, so nothing stops a VIEWER "
                + "before the handler — declare @CurrentMembership(min = ...), or list the route in "
                + "VIEWER_MAY_WRITE with its reason")
            .isEmpty();
    }

    @Test
    void the_exemption_list_names_routes_that_still_exist() {
        // Without this, a renamed or deleted route would leave a dead exemption behind, and the
        // rule above would keep passing for a route that no longer matches it.
        Set<String> existing = new TreeSet<>();
        scopedWriteRoutes().forEach(route -> existing.add(route.name()));

        assertThat(existing).containsAll(VIEWER_MAY_WRITE);
    }

    @Test
    void the_rule_actually_looked_at_something() {
        // A scanner that silently finds nothing would make both tests above pass forever.
        assertThat(scopedWriteRoutes())
            .as("no scoped write route found at all — the classpath scan is broken, not the code")
            .hasSizeGreaterThan(40);
    }
}
