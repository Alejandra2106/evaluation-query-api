package com.crediticio.evaluationquery.shared.exception;

/**
 * La evaluación sigue PENDIENTE en la saga de HU14: no se presenta como una evaluación completa (HU14 CA13).
 * La comparten la explicación (P-HU13-02) y las recomendaciones (P-HU14-03): 409 EVALUACION_PENDIENTE.
 */
public class EvaluacionPendienteException extends RuntimeException {

    public EvaluacionPendienteException() {
        super("La evaluación está pendiente: todavía no está completa");
    }
}
