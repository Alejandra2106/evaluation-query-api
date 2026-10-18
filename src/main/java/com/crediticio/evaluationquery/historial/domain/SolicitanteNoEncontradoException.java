package com.crediticio.evaluationquery.historial.domain;

/** HU15 CA03: el solicitante consultado no existe. */
public class SolicitanteNoEncontradoException extends RuntimeException {

    public SolicitanteNoEncontradoException() {
        super("No existe un solicitante con el identificador indicado");
    }
}
