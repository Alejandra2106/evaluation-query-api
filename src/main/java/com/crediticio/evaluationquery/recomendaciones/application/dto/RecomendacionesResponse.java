package com.crediticio.evaluationquery.recomendaciones.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Recomendaciones de una evaluación (HU14 CA07). generadas es false en las evaluaciones anteriores a la saga, que
 * no tienen recomendaciones (P-HU14-04). Las recomendaciones no son una aprobación ni un rechazo del crédito
 * (CA10).
 */
public record RecomendacionesResponse(long idEvaluacion, boolean generadas, String versionCatalogo,
        OffsetDateTime fechaGeneracion, List<RecomendacionResponse> recomendaciones) {
}
