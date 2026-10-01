package com.nido.api.space.application.service;

import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberNamesTest {

    private final MemberProfilePort profiles = mock(MemberProfilePort.class);
    private final MemberNames names = new MemberNames(profiles);

    private final UUID carol = UUID.randomUUID();
    private final UUID dave = UUID.randomUUID();

    @Test
    void names_every_account_in_one_call_asking_each_once() {
        when(profiles.findByIds(List.of(carol, dave))).thenReturn(List.of(
            new MemberProfile(carol, "carol", "carol@test.com"), new MemberProfile(dave, "dave", "dave@test.com")));

        assertThat(names.byId(List.of(carol, dave, carol))).containsEntry(carol, "carol").containsEntry(dave, "dave").hasSize(2);
    }

    @Test
    void an_account_no_longer_named_and_a_missing_id_are_left_out() {
        when(profiles.findByIds(List.of(carol, dave))).thenReturn(List.of(
            new MemberProfile(carol, null, null), new MemberProfile(dave, "dave", "dave@test.com")));

        assertThat(names.byId(Arrays.asList(carol, null, dave))).containsOnlyKeys(dave);
    }

    @Test
    void nobody_to_name_asks_nothing() {
        assertThat(names.byId(Arrays.asList((UUID) null))).isEmpty();
        verify(profiles, never()).findByIds(any());
    }
}
