package com.crediticio.evaluationquery.recomendaciones.domain;

import java.util.List;

/**
 * Datos persistidos de una evaluación que necesita HU14: estado de la saga (null antes de la saga),
 * clasificación persistida (null antes de HU12) y factores aplicados.
 */
public record EvaluacionParaRecomendar(long idEvaluacion, String estado, String categoria,
        List<FactorEvaluado> factores) {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String COMPLETADA = "COMPLETADA";

    public EvaluacionParaRecomendar {
        factores = List.copyOf(factores);
    }

    public boolean esPendiente() {
        return PENDIENTE.equals(estado);
    }

    public boolean esAnteriorALaSaga() {
        return estado == null;
    }
}
