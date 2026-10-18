package com.crediticio.evaluationquery.historial.infrastructure.input;

import com.crediticio.evaluationquery.historial.application.dto.EvaluacionHistorialResponse;
import com.crediticio.evaluationquery.historial.ports.input.ConsultarHistorialUseCase;
import com.crediticio.evaluationquery.shared.exception.IdentificadorInvalidoException;
import com.crediticio.evaluationquery.shared.response.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU15: historial de evaluaciones de un solicitante (ruta aprobada en P-HU15-02). Roles: SecurityConfig. */
@RestController
public class HistorialEvaluacionesController {

    private final ConsultarHistorialUseCase consultarHistorialUseCase;

    public HistorialEvaluacionesController(ConsultarHistorialUseCase consultarHistorialUseCase) {
        this.consultarHistorialUseCase = consultarHistorialUseCase;
    }

    @Operation(summary = "Consultar el historial de evaluaciones de un solicitante",
            description = "Evaluaciones de la más reciente a la más antigua (a igual fecha, por identificador "
                    + "descendente), con sus resultados persistidos y la ruta de su explicación. Lista vacía si el "
                    + "solicitante no tiene evaluaciones. Roles: ADMINISTRADOR_RIESGO y ANALISTA_CREDITO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial del solicitante (puede estar vacío)",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = EvaluacionHistorialResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Identificador no válido (VALIDATION_ERROR)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "El solicitante no existe (SOLICITANTE_NO_ENCONTRADO)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno (INTERNAL_ERROR)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/api/v1/solicitantes/{idSolicitante}/evaluaciones")
    public ResponseEntity<List<EvaluacionHistorialResponse>> consultar(@PathVariable long idSolicitante) {
        IdentificadorInvalidoException.exigirPositivo(idSolicitante, "idSolicitante");
        return ResponseEntity.ok(consultarHistorialUseCase.consultar(idSolicitante));
    }
}
