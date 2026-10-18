package com.crediticio.evaluationquery.historial.ports.output;

/** HU15 CA02: comprobación de existencia del solicitante, sin leer sus datos personales. */
public interface SolicitanteExistenciaPort {

    boolean existe(long idSolicitante);
}
