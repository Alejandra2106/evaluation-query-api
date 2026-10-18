package com.crediticio.evaluationquery.historial.domain;

/**
 * Origen del puntaje total mostrado en el historial (P-HU15-01): almacenado por scoring-api, o derivado de la
 * suma de los detalles históricos en las evaluaciones registradas antes de HU12.
 */
public enum OrigenPuntaje {
    PERSISTIDO,
    DERIVADO_DE_DETALLES
}
