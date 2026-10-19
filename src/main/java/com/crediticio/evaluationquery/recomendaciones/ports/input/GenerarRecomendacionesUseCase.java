package com.crediticio.evaluationquery.recomendaciones.ports.input;

import com.crediticio.evaluationquery.recomendaciones.application.dto.ResultadoGeneracionResponse;

public interface GenerarRecomendacionesUseCase {

    ResultadoGeneracionResponse generar(long idEvaluacion);
}
