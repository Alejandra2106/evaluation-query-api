package com.crediticio.evaluationquery.explicacion.application;

import com.crediticio.evaluationquery.explicacion.application.dto.ExplicacionEvaluacionResponse;
import com.crediticio.evaluationquery.explicacion.domain.EvaluacionNoEncontradaException;
import com.crediticio.evaluationquery.explicacion.ports.input.ConsultarExplicacionUseCase;
import com.crediticio.evaluationquery.explicacion.ports.output.ExplicacionEvaluacionConsultaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU13: consulta de solo lectura; no modifica la evaluación ni recalcula su resultado. */
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
        return mapper.aResponse(consultaPort.buscarPorId(idEvaluacion)
                .orElseThrow(EvaluacionNoEncontradaException::new));
    }
}
