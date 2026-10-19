package com.crediticio.evaluationquery.recomendaciones.domain;

/** Regla aplicada en la evaluación, tal como quedó persistida: variable, cumplimiento y contribución. */
public record FactorEvaluado(String variable, boolean condicionCumplida, int contribucion) {

    /**
     * Definición BORRADOR de factor desfavorable (P-HU14-02, pendiente de aprobación del negocio): condición
     * cumplida con contribución negativa. Una condición incumplida no es por sí misma un factor desfavorable.
     */
    public boolean esDesfavorable() {
        return condicionCumplida && contribucion < 0;
    }
}
