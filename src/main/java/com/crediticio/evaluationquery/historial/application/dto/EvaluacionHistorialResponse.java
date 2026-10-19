package com.crediticio.evaluationquery.historial.application.dto;

import java.time.LocalDateTime;

/**
 * Elemento del historial (HU15 CA05). No incluye datos personales ni financieros del solicitante (CA13).
 * explicacion y recomendaciones son las rutas de HU13 y HU14 para el detalle de la evaluación y sus
 * recomendaciones (CA09). categoria y versionPolitica son null en evaluaciones anteriores a HU12
 * (clasificada = false), cuyo puntaje es DERIVADO_DE_DETALLES. estado es COMPLETADA, o null en las
 * evaluaciones anteriores a la saga de HU14 (P-HU15-04); las PENDIENTE no se incluyen (CA08).
 */
public record EvaluacionHistorialResponse(
        long idEvaluacion,
        LocalDateTime fechaEvaluacion,
        int scoreTotal,
        String origenPuntaje,
        boolean clasificada,
        String categoria,
        Integer versionPolitica,
        String estado,
        String explicacion,
        String recomendaciones) {
}
