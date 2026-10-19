package com.crediticio.evaluationquery.recomendaciones.ports.output;

import com.crediticio.evaluationquery.recomendaciones.domain.EvaluacionParaRecomendar;

import java.util.Optional;

/** Lectura (solo SELECT) del estado, la clasificación y los factores persistidos por scoring-api. */
public interface EvaluacionParaRecomendarPort {

    Optional<EvaluacionParaRecomendar> buscar(long idEvaluacion);
}
