package com.crediticio.evaluationquery.recomendaciones.domain;

/**
 * Se pidió generar recomendaciones para una evaluación que no está PENDIENTE y no tiene una generación previa
 * (por ejemplo, una evaluación anterior a la saga). Respuesta 409 EVALUACION_NO_PENDIENTE.
 */
public class EvaluacionNoPendienteException extends RuntimeException {

    public EvaluacionNoPendienteException() {
        super("Solo se generan recomendaciones para evaluaciones pendientes");
    }
}
