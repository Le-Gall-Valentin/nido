package com.nido.api.instance.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.infrastructure.web.MailLanguage;
import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.application.port.in.SendSettingsTestMailUseCase;
import com.nido.api.instance.application.port.in.UpdateSettingsUseCase;
import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.SettingGroup;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.infrastructure.web.dto.SettingsResponse;
import com.nido.api.instance.infrastructure.web.dto.SettingsValuesRequest;
import com.nido.api.shared.security.AuthenticatedUser;
import com.nido.api.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@Tag(name = "Instance")
@RestController
@RequestMapping("/api/admin/settings")
public class InstanceSettingsController {

    private final GetEffectiveSettingsQuery settings;
    private final UpdateSettingsUseCase update;
    private final SendSettingsTestMailUseCase testMail;

    public InstanceSettingsController(GetEffectiveSettingsQuery settings, UpdateSettingsUseCase update,
                                      SendSettingsTestMailUseCase testMail) {
        this.settings = settings;
        this.update = update;
        this.testMail = testMail;
    }

    @Operation(summary = "Réglages de l'instance",
        description = "Chaque réglage avec sa source (`ENVIRONMENT`, `DATABASE`, `DEFAULT`). Un secret n'est jamais renvoyé : seulement s'il est défini. Réservé au SUPER_ADMIN. Rate limit : 60 req/fenêtre.")
    @GetMapping
    @RateLimiting(max = 60)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SettingsResponse get() {
        return SettingsResponse.of(settings.current());
    }

    @Operation(summary = "Enregistrer un bloc de réglages",
        description = "Une valeur vide revient au défaut ; un mot de passe vide garde celui enregistré. `400 SETTINGS_INVALID` avec les erreurs par réglage, `409 SETTING_LOCKED_BY_ENVIRONMENT` pour un réglage défini par l'environnement. Rate limit : 20 req/fenêtre.")
    @PutMapping("/{group}")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SettingsResponse update(@PathVariable String group, @Valid @RequestBody SettingsValuesRequest body,
                                   @CurrentUser AuthenticatedUser user) {
        return SettingsResponse.of(update.update(group(group), SettingCodes.keys(body.values()), user.userId()));
    }

    @Operation(summary = "Rétablir un réglage", description = "Revient à la valeur par défaut ; efface un secret. Rate limit : 20 req/fenêtre.")
    @DeleteMapping("/{group}/{key}")
    @RateLimiting(max = 20)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SettingsResponse reset(@PathVariable String group, @PathVariable String key, @CurrentUser AuthenticatedUser user) {
        SettingGroup settingGroup = group(group);
        SettingKey settingKey = SettingKey.fromCode(key).filter(k -> k.group() == settingGroup)
            .orElseThrow(() -> new InstanceException.UnknownSetting(key));
        return SettingsResponse.of(update.reset(settingKey, user.userId()));
    }

    @Operation(summary = "Envoyer un mail de test",
        description = "Avec les valeurs du formulaire, sans rien enregistrer, à l'adresse de l'administrateur connecté. `422 MAIL_TEST_FAILED` avec la réponse du serveur. Rate limit : 5 req/fenêtre.")
    @PostMapping("/mail/test")
    @RateLimiting(max = 5)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> testMail(@Valid @RequestBody SettingsValuesRequest body, @CurrentUser AuthenticatedUser user) {
        testMail.sendTest(SettingCodes.keys(body.values()), user.email(), locale());
        return ResponseEntity.noContent().build();
    }

    private static SettingGroup group(String code) {
        return SettingGroup.fromCode(code).orElseThrow(() -> new InstanceException.UnknownSetting(code));
    }

    private static Locale locale() {
        return MailLanguage.requested().map(language -> Locale.of(language.code())).orElse(Locale.ENGLISH);
    }
}
