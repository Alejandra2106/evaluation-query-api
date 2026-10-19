package com.crediticio.evaluationquery.recomendaciones.domain;

/** Criterio que activa una entrada del catálogo (HU14 CA02, CA03). */
public enum TipoActivacion {
    /** La clasificación persistida de la evaluación coincide con la categoría de la entrada. */
    CATEGORIA,
    /** La evaluación tiene un factor desfavorable sobre la variable de la entrada. */
    FACTOR_DESFAVORABLE
}
