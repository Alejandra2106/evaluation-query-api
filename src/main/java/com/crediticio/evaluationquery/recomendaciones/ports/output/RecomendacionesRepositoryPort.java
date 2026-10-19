package com.crediticio.evaluationquery.recomendaciones.ports.output;

import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionConcurrenteException;
import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionRecomendaciones;

import java.util.Optional;

/** Recomendaciones generadas, en el esquema propio de evaluation-query-api (HU14 CA04–CA06). */
public interface RecomendacionesRepositoryPort {

    Optional<GeneracionRecomendaciones> buscar(long idEvaluacion);

    /**
     * Guarda la cabecera y sus recomendaciones en la transacción actual.
     *
     * @throws GeneracionConcurrenteException si ya existe una generación para esa evaluación
     */
    GeneracionRecomendaciones guardar(GeneracionRecomendaciones generacion);
}
