package com.crediticio.evaluationquery.explicacion.application;

import com.crediticio.evaluationquery.explicacion.application.dto.ExplicacionEvaluacionResponse;
import com.crediticio.evaluationquery.explicacion.domain.EvaluacionNoEncontradaException;
import com.crediticio.evaluationquery.explicacion.domain.ExplicacionEvaluacion;
import com.crediticio.evaluationquery.explicacion.ports.input.ConsultarExplicacionUseCase;
import com.crediticio.evaluationquery.explicacion.ports.output.ExplicacionEvaluacionConsultaPort;
import com.crediticio.evaluationquery.shared.exception.EvaluacionPendienteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU13: consulta de solo lectura; no modifica la evaluación ni recalcula su resultado. Una evaluación PENDIENTE de la
 * saga de HU14 no se explica como completa: 409 EVALUACION_PENDIENTE (P-HU13-02). */
@Service
public class ConsultarExplicacionService implements ConsultarExplicacionUseCase {

    private final ExplicacionEvaluacionConsultaPort consultaPort;
    private final ExplicacionMapper mapper;

    public ConsultarExplicacionService(ExplicacionEvaluacionConsultaPort consultaPort, ExplicacionMapper mapper) {
        this.consultaPort = consultaPort;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ExplicacionEvaluacionResponse consultar(long idEvaluacion) {
        ExplicacionEvaluacion explicacion = consultaPort.buscarPorId(idEvaluacion)
                .orElseThrow(EvaluacionNoEncontradaException::new);
        if (explicacion.isPendiente()) {
            throw new EvaluacionPendienteException();
        }
        return mapper.aResponse(explicacion);
    }
}
