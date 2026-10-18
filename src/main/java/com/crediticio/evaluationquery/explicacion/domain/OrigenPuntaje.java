package com.crediticio.evaluationquery.explicacion.domain;

/**
 * Origen del puntaje total mostrado (P-HU13-04): almacenado por scoring-api, o derivado de la suma de los
 * detalles históricos en las evaluaciones registradas antes de HU12, que no tienen puntaje persistido.
 */
public enum OrigenPuntaje {
    PERSISTIDO,
    DERIVADO_DE_DETALLES
}
