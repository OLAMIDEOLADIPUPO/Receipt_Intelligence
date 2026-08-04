package com.olamide.receipthandler.configurations;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger metadata for the whole API.
 *
 * The document is served at {@code /v3/api-docs} and the interactive UI at
 * {@code /swagger-ui.html}. Both paths are opened up in {@link SecurityConfig}
 * so the docs are reachable without a token.
 *
 * <p>A single {@code bearer-jwt} security scheme is declared here and applied
 * to every operation via the top-level {@link SecurityRequirement}. The public
 * auth endpoints opt out individually with {@code @Operation(security = {})},
 * so Swagger's "Authorize" lock shows only where a token is actually required.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Receipt Handler API",
                version = "1.0.0",
                description = """
                        Backend for uploading receipt images/PDFs, extracting their contents with an
                        AI vision model, and reporting on spending. Receipts are processed
                        asynchronously: an upload returns immediately with status `PENDING`/`PROCESSING`,
                        and the receipt is fetched again once it reaches `COMPLETED` or `FAILED`.

                        **Authentication.** All endpoints except `/api/auth/**` require a Bearer JWT
                        access token obtained from `/api/auth/login` or `/api/auth/register`. Send it
                        as `Authorization: Bearer <token>`. A long-lived refresh token is issued as an
                        HttpOnly cookie and exchanged at `/api/auth/refresh`.
                        """,
                contact = @Contact(name = "Olamide Oladipupo")
        ),
        servers = @Server(url = "/", description = "Current host"),
        security = @SecurityRequirement(name = "bearer-jwt")
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT access token from /api/auth/login or /api/auth/register."
)
public class OpenApiConfig {
}
