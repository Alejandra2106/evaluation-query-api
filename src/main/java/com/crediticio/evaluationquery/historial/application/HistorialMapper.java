package com.crediticio.evaluationquery.historial.application;

import com.crediticio.evaluationquery.historial.application.dto.EvaluacionHistorialResponse;
import com.crediticio.evaluationquery.historial.domain.EvaluacionHistorica;
import org.springframework.stereotype.Component;

@Component
public class HistorialMapper {

    /** Ruta de la explicación (HU13, P-HU13-01). */
    static final String RUTA_EXPLICACION = "/api/v1/evaluaciones/%d/explicacion";

    /** Ruta de las recomendaciones (HU14, P-HU14-05). */
    static final String RUTA_RECOMENDACIONES = "/api/v1/evaluaciones/%d/recomendaciones";

    public EvaluacionHistorialResponse aResponse(EvaluacionHistorica evaluacion) {
        return new EvaluacionHistorialResponse(
                evaluacion.getIdEvaluacion(),
                evaluacion.getFechaEvaluacion(),
                evaluacion.getScoreTotal(),
                evaluacion.getOrigenPuntaje().name(),
                evaluacion.isClasificada(),
                evaluacion.getCategoria(),
                evaluacion.getVersionPolitica(),
                evaluacion.getEstado(),
                RUTA_EXPLICACION.formatted(evaluacion.getIdEvaluacion()),
                RUTA_RECOMENDACIONES.formatted(evaluacion.getIdEvaluacion()));
    }
}
