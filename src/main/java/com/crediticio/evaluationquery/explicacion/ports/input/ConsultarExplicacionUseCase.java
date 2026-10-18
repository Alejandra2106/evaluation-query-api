package com.crediticio.evaluationquery.explicacion.ports.input;

import com.crediticio.evaluationquery.explicacion.application.dto.ExplicacionEvaluacionResponse;

public interface ConsultarExplicacionUseCase {

    ExplicacionEvaluacionResponse consultar(long idEvaluacion);
}
