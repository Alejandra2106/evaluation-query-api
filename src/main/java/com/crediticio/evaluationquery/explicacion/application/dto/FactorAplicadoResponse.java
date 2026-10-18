package com.crediticio.evaluationquery.explicacion.application.dto;

/** Regla aplicada con su versión histórica, el cumplimiento de su condición y su contribución (HU13 CA03–CA05). */
public record FactorAplicadoResponse(
        long idRegla,
        String variable,
        String operadorAplicado,
        String valorCondicionAplicado,
        int puntajeReglaAplicado,
        boolean condicionCumplida,
        int contribucion) {
}
