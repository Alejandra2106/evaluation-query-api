package com.crediticio.evaluationquery.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI de evaluation-query-api (mismo patrón que scoring-api): los endpoints de negocio exigen un
 * JWT de acceso emitido por auth-api (esquema bearerAuth) y pueden responder 401/403 con el formato ApiError.
 * Solo se publica con el perfil dev.
 */
@Configuration
public class OpenApiConfig {

    static final String ESQUEMA_SEGURIDAD = "bearerAuth";
    private static final String REFERENCIA_API_ERROR = "#/components/schemas/ApiError";

    @Bean
    public OpenAPI evaluationQueryApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("evaluation-query-api - Motor de Scoring de Riesgo Crediticio")
                        .version("v1")
                        .description("Explicación de una evaluación (HU13) e historial de evaluaciones de un "
                                + "solicitante (HU15). Requiere un JWT de acceso emitido por auth-api."))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token de acceso RS256 emitido por auth-api")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD));
    }

    @Bean
    public OpenApiCustomizer respuestasDeSeguridad() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(ruta -> ruta.readOperations().forEach(operacion -> {
                if (operacion.getResponses() == null) {
                    operacion.setResponses(new ApiResponses());
                }
                ApiResponses respuestas = operacion.getResponses();
                if (!respuestas.containsKey("401")) {
                    respuestas.addApiResponse("401",
                            respuestaError("Token de acceso ausente, inválido o expirado"));
                }
                if (!respuestas.containsKey("403")) {
                    respuestas.addApiResponse("403",
                            respuestaError("El usuario no tiene un rol autorizado para esta operación"));
                }
            }));
        };
    }

    private static ApiResponse respuestaError(String descripcion) {
        return new ApiResponse()
                .description(descripcion)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref(REFERENCIA_API_ERROR))));
    }
}
