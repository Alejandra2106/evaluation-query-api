package com.crediticio.evaluationquery.historial.ports.output;

import com.crediticio.evaluationquery.historial.domain.EvaluacionHistorica;

import java.util.List;

public interface HistorialEvaluacionesPort {

    /** Evaluaciones del solicitante, de la más reciente a la más antigua y, a igual fecha, por id descendente. */
    List<EvaluacionHistorica> listarPorSolicitante(long idSolicitante);
}
