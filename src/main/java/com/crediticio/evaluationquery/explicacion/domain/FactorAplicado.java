package com.crediticio.evaluationquery.explicacion.domain;

/**
 * Regla aplicada en una evaluación, tal como quedó persistida al evaluar (HU13 CA03–CA05): la instantánea de
 * la regla (operador, valor de la condición y puntaje vigentes en ese momento, P-HU13-03), si se cumplió la
 * condición y su contribución (el puntaje obtenido, 0 si la condición no se cumplió). La variable es la de
 * la regla, que no cambia al editarla.
 */
public record FactorAplicado(
        long idRegla,
        String variable,
        String operadorAplicado,
        String valorCondicionAplicado,
        int puntajeReglaAplicado,
        boolean condicionCumplida,
        int contribucion) {
}
