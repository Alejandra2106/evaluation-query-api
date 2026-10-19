package com.crediticio.evaluationquery.recomendaciones.domain;

/** HU14 CA08: la evaluación consultada no existe (respuesta 404 EVALUACION_NO_ENCONTRADA). */
public class EvaluacionInexistenteException extends RuntimeException {

    public EvaluacionInexistenteException() {
        super("No existe una evaluación con el identificador indicado");
    }
}
