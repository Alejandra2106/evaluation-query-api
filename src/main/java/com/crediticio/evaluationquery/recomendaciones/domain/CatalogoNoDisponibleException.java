package com.crediticio.evaluationquery.recomendaciones.domain;

/**
 * El catálogo no está APROBADO (P-HU14-01): no se genera ninguna recomendación. Respuesta 503
 * CATALOGO_NO_DISPONIBLE, que scoring-api trata como transitoria.
 */
public class CatalogoNoDisponibleException extends RuntimeException {

    public CatalogoNoDisponibleException(String version) {
        super("El catálogo de recomendaciones " + version + " no está aprobado");
    }
}
