package com.crediticio.evaluationquery.explicacion.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU13. No incluye datos personales ni financieros del solicitante, ni su identificador (CA11, P-HU13-06).
 * categoria y versionPolitica son null en evaluaciones anteriores a HU12 (clasificada = false), cuyo puntaje
 * es la suma de sus detalles (origenPuntaje = DERIVADO_DE_DETALLES).
 */
public record ExplicacionEvaluacionResponse(
        long idEvaluacion,
        LocalDateTime fechaEvaluacion,
        int scoreTotal,
        String origenPuntaje,
        boolean clasificada,
        String categoria,
        Integer versionPolitica,
        List<FactorAplicadoResponse> factores) {
}
