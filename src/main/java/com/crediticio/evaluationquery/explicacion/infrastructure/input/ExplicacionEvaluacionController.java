package com.crediticio.evaluationquery.explicacion.infrastructure.input;

import com.crediticio.evaluationquery.explicacion.application.dto.ExplicacionEvaluacionResponse;
import com.crediticio.evaluationquery.explicacion.ports.input.ConsultarExplicacionUseCase;
import com.crediticio.evaluationquery.shared.exception.IdentificadorInvalidoException;
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

    @GetMapping("/api/v1/evaluaciones/{idEvaluacion}/explicacion")
    public ResponseEntity<ExplicacionEvaluacionResponse> consultar(@PathVariable long idEvaluacion) {
        IdentificadorInvalidoException.exigirPositivo(idEvaluacion, "idEvaluacion");
        return ResponseEntity.ok(consultarExplicacionUseCase.consultar(idEvaluacion));
    }
}
