package com.crediticio.evaluationquery.recomendaciones.infrastructure.input;

import com.crediticio.evaluationquery.recomendaciones.application.dto.RecomendacionesResponse;
import com.crediticio.evaluationquery.recomendaciones.ports.input.ConsultarRecomendacionesUseCase;
import com.crediticio.evaluationquery.shared.exception.IdentificadorInvalidoException;
import com.crediticio.evaluationquery.shared.response.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** HU14: consulta de las recomendaciones persistidas (P-HU14-05). Roles: SecurityConfig (HU10 CA04, CA11). */
@RestController
public class RecomendacionesController {

    private final ConsultarRecomendacionesUseCase consultarRecomendacionesUseCase;

    public RecomendacionesController(ConsultarRecomendacionesUseCase consultarRecomendacionesUseCase) {
        this.consultarRecomendacionesUseCase = consultarRecomendacionesUseCase;
    }

    @Operation(summary = "Consultar las recomendaciones de una evaluación",
            description = "Devuelve las recomendaciones generadas durante la evaluación, con el código, el texto y "
                    + "la versión del catálogo con que se generaron. No las genera ni recalcula nada. No son una "
                    + "aprobación ni un rechazo del crédito. En las evaluaciones anteriores a la saga devuelve una "
                    + "lista vacía con generadas = false. Roles: ADMINISTRADOR_RIESGO y ANALISTA_CREDITO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recomendaciones de la evaluación",
                    content = @Content(schema = @Schema(implementation = RecomendacionesResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identificador no válido (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "La evaluación no existe (EVALUACION_NO_ENCONTRADA)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409",
                    description = "La evaluación está pendiente; aún no tiene recomendaciones (EVALUACION_PENDIENTE)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno (INTERNAL_ERROR)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/api/v1/evaluaciones/{idEvaluacion}/recomendaciones")
    public ResponseEntity<RecomendacionesResponse> consultar(@PathVariable long idEvaluacion) {
        IdentificadorInvalidoException.exigirPositivo(idEvaluacion, "idEvaluacion");
        return ResponseEntity.ok(consultarRecomendacionesUseCase.consultar(idEvaluacion));
    }
}
