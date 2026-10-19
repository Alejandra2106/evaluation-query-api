package com.crediticio.evaluationquery.recomendaciones.domain;

/** Otra petición guardó primero la generación de la misma evaluación (reintento concurrente). */
public class GeneracionConcurrenteException extends RuntimeException {

    public GeneracionConcurrenteException(long idEvaluacion, Throwable causa) {
        super("La generación de la evaluación " + idEvaluacion + " ya fue guardada por otra petición", causa);
    }
}
