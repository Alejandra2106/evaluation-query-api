package com.crediticio.evaluationquery.recomendaciones.ports.input;

import com.crediticio.evaluationquery.recomendaciones.application.dto.RecomendacionesResponse;

public interface ConsultarRecomendacionesUseCase {

    RecomendacionesResponse consultar(long idEvaluacion);
}
