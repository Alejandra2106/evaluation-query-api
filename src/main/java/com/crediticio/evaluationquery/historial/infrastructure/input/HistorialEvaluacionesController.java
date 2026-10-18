package com.crediticio.evaluationquery.historial.infrastructure.input;

import com.crediticio.evaluationquery.historial.application.dto.EvaluacionHistorialResponse;
import com.crediticio.evaluationquery.historial.ports.input.ConsultarHistorialUseCase;
import com.crediticio.evaluationquery.shared.exception.IdentificadorInvalidoException;
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

    @GetMapping("/api/v1/solicitantes/{idSolicitante}/evaluaciones")
    public ResponseEntity<List<EvaluacionHistorialResponse>> consultar(@PathVariable long idSolicitante) {
        IdentificadorInvalidoException.exigirPositivo(idSolicitante, "idSolicitante");
        return ResponseEntity.ok(consultarHistorialUseCase.consultar(idSolicitante));
    }
}
