package com.crediticio.evaluationquery.historial.application.dto;

import java.time.LocalDateTime;

/**
 * Elemento del historial (HU15 CA05). No incluye datos personales ni financieros del solicitante (CA13).
 * explicacion es la ruta de HU13 para el detalle de la evaluación (CA09); la referencia a las
 * recomendaciones (HU14) se añadirá cuando HU14 esté implementada. categoria y versionPolitica son null en
 * evaluaciones anteriores a HU12 (clasificada = false), cuyo puntaje es DERIVADO_DE_DETALLES.
 */
public record EvaluacionHistorialResponse(
        long idEvaluacion,
        LocalDateTime fechaEvaluacion,
        int scoreTotal,
        String origenPuntaje,
        boolean clasificada,
        String categoria,
        Integer versionPolitica,
        String explicacion) {
}
