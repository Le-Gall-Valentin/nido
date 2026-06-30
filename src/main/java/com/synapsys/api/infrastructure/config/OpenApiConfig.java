package com.synapsys.api.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI synapsysOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Synapsys API")
                        .version("1.0.0")
                        .description("""
                                API REST de Synapsys.

                                ## Authentification

                                L'API utilise des cookies HttpOnly pour transporter les tokens JWT.
                                Après un login réussi, deux cookies sont posés automatiquement :
                                - `access_token` : token d'accès à courte durée de vie (utilisé sur chaque requête protégée)
                                - `refresh_token` : token de renouvellement à longue durée de vie (utilisé sur `POST /api/auth/refresh`)

                                Ces cookies sont transmis automatiquement par le navigateur. Il n'y a pas de header `Authorization` à gérer.

                                ## Rate limiting

                                Tous les endpoints sont soumis à un rate limit. En cas de dépassement, l'API répond avec `429 Too Many Requests`
                                et les headers suivants :
                                - `X-RateLimit-Limit` : nombre de requêtes autorisées par fenêtre
                                - `X-RateLimit-Remaining` : nombre de requêtes restantes (0 si dépassé)
                                - `X-RateLimit-Reset` : timestamp Unix de réinitialisation de la fenêtre
                                - `Retry-After` : délai en secondes avant de pouvoir réessayer

                                ## Format des erreurs

                                Toutes les erreurs suivent le format RFC 7807 (`application/problem+json`) :
                                ```json
                                {
                                  "type": "about:blank",
                                  "title": "NomDeLErreur",
                                  "status": 404,
                                  "detail": "Description lisible de l'erreur.",
                                  "instance": "/api/users/abc"
                                }
                                ```
                                Certaines erreurs incluent un champ `error_code` supplémentaire (ex : `totp_challenge_expired`).
                                """))
                .tags(List.of(
                        new Tag().name("Authentication").description("Login, logout et renouvellement de token"),
                        new Tag().name("2FA - Challenge").description("Vérification du code TOTP lors du login à deux facteurs"),
                        new Tag().name("2FA - Management").description("Configuration et gestion du TOTP pour l'utilisateur connecté"),
                        new Tag().name("Users").description("Gestion des utilisateurs (profil personnel et administration)")))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("access_token")
                                .description("Cookie HttpOnly contenant le JWT d'accès. Posé automatiquement par `POST /api/auth/login`."))
                        .addSecuritySchemes("refreshCookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("refresh_token")
                                .description("Cookie HttpOnly contenant le token de renouvellement. Requis uniquement sur `POST /api/auth/refresh`.")));
    }
}
