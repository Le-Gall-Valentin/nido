package com.nido.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two records describe a user to the browser, one in authentication and one in identity, and they
 * are field for field the same.
 *
 * <p>That is not an oversight to merge away. Neither bounded context may reach into the other's
 * infrastructure — {@link ArchRulesTest} fails the build over it — so a shared response type would
 * have to live somewhere that belongs to neither, and coupling the two contexts' wire formats is
 * exactly what that rule exists to prevent. Each owning its own is the design.
 *
 * <p>What the design does not give is any reason for them to stay in step, and they have to: the
 * frontend has a single {@code User} interface and points it at both
 * {@code GET /api/users/me} and {@code POST /api/auth/2fa/verify}. Adding a field on one side is
 * silent on this side of the wire and breaks the other route in the browser. This test is the
 * missing reason.
 *
 * <p>It compares names and types rather than asserting a list, so the failure names what diverged
 * and adding a field to both sides needs no edit here.
 */
class UserInfoResponseContractTest {

    private static List<String> shapeOf(Class<?> record) {
        return Arrays.stream(record.getRecordComponents())
            .map(component -> component.getName() + ": " + component.getType().getSimpleName())
            .toList();
    }

    @Test
    void both_descriptions_of_a_user_have_the_same_shape() {
        List<String> fromAuthentication =
            shapeOf(com.nido.api.authentication.infrastructure.web.dto.UserInfoResponse.class);
        List<String> fromIdentity =
            shapeOf(com.nido.api.identity.infrastructure.web.dto.UserInfoResponse.class);

        assertThat(fromAuthentication)
            .as("the frontend reads both routes into one type — a field on one side only breaks the other")
            .containsExactlyElementsOf(fromIdentity);
    }

    @Test
    void the_comparison_is_actually_looking_at_fields() {
        // A record with no components would make the rule above pass on two empty lists.
        assertThat(shapeOf(com.nido.api.identity.infrastructure.web.dto.UserInfoResponse.class))
            .hasSizeGreaterThan(3)
            .anySatisfy(component -> assertThat(component).startsWith("id:"));
    }

    @Test
    void neither_record_reaches_into_the_other_context() {
        // The duplication is the price of the isolation; this checks the price is actually being
        // paid, rather than one record quietly referencing the other's types.
        for (Class<?> record : List.of(com.nido.api.authentication.infrastructure.web.dto.UserInfoResponse.class,
                                       com.nido.api.identity.infrastructure.web.dto.UserInfoResponse.class)) {
            String ownContext = record.getName().startsWith("com.nido.api.authentication")
                ? "identity" : "authentication";
            for (RecordComponent component : record.getRecordComponents()) {
                assertThat(component.getType().getName())
                    .as("%s.%s", record.getSimpleName(), component.getName())
                    .doesNotContain("com.nido.api." + ownContext);
            }
        }
    }
}
