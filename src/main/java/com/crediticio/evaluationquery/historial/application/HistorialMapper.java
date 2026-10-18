package com.crediticio.evaluationquery.historial.application;

import com.crediticio.evaluationquery.historial.application.dto.EvaluacionHistorialResponse;
import com.crediticio.evaluationquery.historial.domain.EvaluacionHistorica;
import org.springframework.stereotype.Component;

@Component
public class HistorialMapper {

    /** Ruta de la explicación (HU13, P-HU13-01). */
    static final String RUTA_EXPLICACION = "/api/v1/evaluaciones/%d/explicacion";

    public EvaluacionHistorialResponse aResponse(EvaluacionHistorica evaluacion) {
        return new EvaluacionHistorialResponse(
                evaluacion.getIdEvaluacion(),
                evaluacion.getFechaEvaluacion(),
                evaluacion.getScoreTotal(),
                evaluacion.getOrigenPuntaje().name(),
                evaluacion.isClasificada(),
                evaluacion.getCategoria(),
                evaluacion.getVersionPolitica(),
                RUTA_EXPLICACION.formatted(evaluacion.getIdEvaluacion()));
    }
}
