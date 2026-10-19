package com.crediticio.evaluationquery.recomendaciones.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Recomendaciones generadas para una evaluación con una versión del catálogo (HU14 CA04, CA05). Puede no
 * contener ninguna recomendación: la generación queda registrada igualmente. fechaGeneracion es null hasta que
 * la base de datos la asigna al guardarla.
 */
public record GeneracionRecomendaciones(long idEvaluacion, String versionCatalogo, String categoriaAplicada,
        OffsetDateTime fechaGeneracion, List<RecomendacionGenerada> recomendaciones) {

    public GeneracionRecomendaciones {
        Objects.requireNonNull(versionCatalogo, "versionCatalogo es obligatorio");
        Objects.requireNonNull(categoriaAplicada, "categoriaAplicada es obligatorio");
        recomendaciones = List.copyOf(Objects.requireNonNull(recomendaciones, "recomendaciones es obligatorio"));
    }
}
