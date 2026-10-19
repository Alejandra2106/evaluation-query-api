package com.crediticio.evaluationquery.recomendaciones.application.dto;

/** Recomendación tal como se generó: código, texto y criterio de activación (HU14 CA05). */
public record RecomendacionResponse(int orden, String codigo, String texto, String tipoActivacion, String categoria,
        String variable) {
}
