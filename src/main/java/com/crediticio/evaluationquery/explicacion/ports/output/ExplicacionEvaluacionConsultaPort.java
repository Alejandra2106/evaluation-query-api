package com.crediticio.evaluationquery.explicacion.ports.output;

import com.crediticio.evaluationquery.explicacion.domain.ExplicacionEvaluacion;

import java.util.Optional;

/** Lectura de la evaluación y de sus detalles persistidos por scoring-api (HU13). */
public interface ExplicacionEvaluacionConsultaPort {

    Optional<ExplicacionEvaluacion> buscarPorId(long idEvaluacion);
}
