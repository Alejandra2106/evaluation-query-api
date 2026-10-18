package com.crediticio.evaluationquery.explicacion.domain;

/** HU13 CA08: la evaluación consultada no existe. */
public class EvaluacionNoEncontradaException extends RuntimeException {

    public EvaluacionNoEncontradaException() {
        super("No existe una evaluación con el identificador indicado");
    }
}
