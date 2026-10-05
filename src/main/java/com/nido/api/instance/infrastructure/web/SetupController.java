package com.nido.api.instance.infrastructure.web;

import com.nido.api.infrastructure.ratelimit.RateLimiting;
import com.nido.api.instance.application.port.in.GetSetupStatusQuery;
import com.nido.api.instance.application.port.in.SetupUseCase;
import com.nido.api.instance.domain.model.CompleteSetupCommand;
import com.nido.api.instance.domain.model.InitialAdmin;
import com.nido.api.instance.domain.model.SetupEncryptionKey;
import com.nido.api.instance.domain.model.SetupStatus;
import com.nido.api.instance.infrastructure.web.dto.CompleteSetupRequest;
import com.nido.api.instance.infrastructure.web.dto.SetupCodeRequest;
import com.nido.api.instance.infrastructure.web.dto.SetupKeyResponse;
import com.nido.api.instance.infrastructure.web.dto.SetupMailTestRequest;
import com.nido.api.instance.infrastructure.web.dto.SetupStatusResponse;
import com.nido.api.shared.model.Language;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/**
 * The first-run setup. Public — there is no account yet — and guarded instead by the code printed in
 * the logs, sent in the body of every call, never in a URL that would end up in an access log.
 */
@Tag(name = "Instance")
@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final GetSetupStatusQuery status;
    private final SetupUseCase setup;

    public SetupController(GetSetupStatusQuery status, SetupUseCase setup) {
        this.status = status;
        this.setup = setup;
    }

    @Operation(summary = "L'installation est-elle à faire ?",
        description = "Public. Une fois l'installation faite, ne dit rien d'autre que `required: false`. Rate limit : 60 req/fenêtre.")
    @GetMapping("/status")
    @RateLimiting(max = 60)
    public SetupStatusResponse status() {
        SetupStatus current = status.status();
        return new SetupStatusResponse(current.required(), current.lockedPublicUrl().orElse(null), current.mailLocked());
    }

    @Operation(summary = "Vérifier le code d'installation", description = "`403 SETUP_CODE_INVALID` ; `404` une fois l'installation faite. Rate limit : 5 req/fenêtre.")
    @PostMapping("/verify-code")
    @RateLimiting(max = 5)
    public ResponseEntity<Void> verifyCode(@Valid @RequestBody SetupCodeRequest body) {
        setup.verifyCode(body.code());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "La clé de chiffrement, si Nido l'a générée", description = "Rate limit : 5 req/fenêtre.")
    @PostMapping("/encryption-key")
    @RateLimiting(max = 5)
    public SetupKeyResponse encryptionKey(@Valid @RequestBody SetupCodeRequest body) {
        SetupEncryptionKey key = setup.encryptionKey(body.code());
        return key.generated() ? new SetupKeyResponse("GENERATED", key.key()) : new SetupKeyResponse("PROVIDED", null);
    }

    @Operation(summary = "Envoyer un mail de test pendant l'installation", description = "`422 MAIL_TEST_FAILED` avec la réponse du serveur. Rate limit : 5 req/fenêtre.")
    @PostMapping("/mail-test")
    @RateLimiting(max = 5)
    public ResponseEntity<Void> mailTest(@Valid @RequestBody SetupMailTestRequest body) {
        setup.sendTestMail(body.code(), InstanceSettingsController.keys(body.mail()), body.publicUrl(), body.recipient(),
            body.language() == null ? Locale.ENGLISH : Locale.of(body.language()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Terminer l'installation",
        description = "Tout ou rien : compte SUPER_ADMIN, adresse publique, mail, fin de l'installation. Ne connecte pas. Rate limit : 5 req/fenêtre.")
    @PostMapping("/complete")
    @RateLimiting(max = 5)
    public ResponseEntity<Void> complete(@Valid @RequestBody CompleteSetupRequest body) {
        Language language = body.admin().language() == null ? null : Language.fromCode(body.admin().language()).orElse(null);
        setup.complete(new CompleteSetupCommand(body.code(),
            new InitialAdmin(body.admin().username().strip(), body.admin().email(), body.admin().password(), language),
            body.publicUrl(), body.mail() == null ? null : InstanceSettingsController.keys(body.mail()),
            body.encryptionKeySaved()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
