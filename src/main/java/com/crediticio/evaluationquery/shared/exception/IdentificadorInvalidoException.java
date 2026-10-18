package com.crediticio.evaluationquery.shared.exception;

/** Identificador de ruta no positivo (respuesta 400 VALIDATION_ERROR). */
public class IdentificadorInvalidoException extends RuntimeException {

    public IdentificadorInvalidoException(String parametro) {
        super("El parámetro '" + parametro + "' debe ser un entero positivo");
    }

    /** Devuelve el identificador si es positivo; si no, rechaza la solicitud. */
    public static long exigirPositivo(long identificador, String parametro) {
        if (identificador <= 0) {
            throw new IdentificadorInvalidoException(parametro);
        }
        return identificador;
    }
}
