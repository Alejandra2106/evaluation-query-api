package com.crediticio.evaluationquery.explicacion.domain;

/**
 * Los datos persistidos de una evaluación no son coherentes (P-HU13-05): no se corrigen ni se ocultan; la
 * consulta responde 500 INTERNAL_ERROR y el detalle se registra en el log. El mensaje solo contiene
 * identificadores y puntajes, nunca datos del solicitante.
 */
public class ExplicacionInconsistenteException extends RuntimeException {

    public ExplicacionInconsistenteException(long idEvaluacion, String motivo) {
        super("Datos inconsistentes en la evaluación " + idEvaluacion + ": " + motivo);
    }
}
