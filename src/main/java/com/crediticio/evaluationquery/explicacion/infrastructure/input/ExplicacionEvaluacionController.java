package com.crediticio.evaluationquery.explicacion.infrastructure.input;

import com.crediticio.evaluationquery.explicacion.application.dto.ExplicacionEvaluacionResponse;
import com.crediticio.evaluationquery.explicacion.ports.input.ConsultarExplicacionUseCase;
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

/** HU13: explicación de una evaluación (ruta aprobada en P-HU13-01). Roles: SecurityConfig (HU10 CA04). */
@RestController
public class ExplicacionEvaluacionController {

    private final ConsultarExplicacionUseCase consultarExplicacionUseCase;

    public ExplicacionEvaluacionController(ConsultarExplicacionUseCase consultarExplicacionUseCase) {
        this.consultarExplicacionUseCase = consultarExplicacionUseCase;
    }

    @Operation(summary = "Consultar la explicación de una evaluación",
            description = "Devuelve los datos generales de la evaluación y cada regla aplicada con su versión "
                    + "histórica, si se cumplió su condición y su contribución (0 si no se cumplió). Usa solo datos "
                    + "persistidos, sin recalcular. Roles: ADMINISTRADOR_RIESGO y ANALISTA_CREDITO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Explicación de la evaluación",
                    content = @Content(schema = @Schema(implementation = ExplicacionEvaluacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identificador no válido (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "La evaluación no existe (EVALUACION_NO_ENCONTRADA)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500",
                    description = "Datos persistidos incoherentes o error interno (INTERNAL_ERROR)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/api/v1/evaluaciones/{idEvaluacion}/explicacion")
    public ResponseEntity<ExplicacionEvaluacionResponse> consultar(@PathVariable long idEvaluacion) {
        IdentificadorInvalidoException.exigirPositivo(idEvaluacion, "idEvaluacion");
        return ResponseEntity.ok(consultarExplicacionUseCase.consultar(idEvaluacion));
    }
}
