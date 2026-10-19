package com.crediticio.evaluationquery.recomendaciones.domain;

import java.util.Objects;
import java.util.Set;

/** Entrada del catálogo de recomendaciones: código estable, texto y criterio de activación (HU14 CA03). */
public record EntradaCatalogo(String codigo, String texto, TipoActivacion tipoActivacion, String categoria,
        String variable) {

    static final Set<String> CATEGORIAS = Set.of("BAJO", "MEDIO", "ALTO");
    static final Set<String> VARIABLES = Set.of("INGRESOS_MENSUALES", "NIVEL_ENDEUDAMIENTO", "NUMERO_MORAS",
            "HISTORIAL_CREDITICIO", "ANTIGUEDAD_LABORAL");

    public EntradaCatalogo {
        requerirTexto(codigo, "codigo", 50);
        requerirTexto(texto, "texto", 500);
        Objects.requireNonNull(tipoActivacion, "tipoActivacion es obligatorio");
        if (tipoActivacion == TipoActivacion.CATEGORIA && (!CATEGORIAS.contains(categoria) || variable != null)) {
            throw new IllegalArgumentException("La entrada " + codigo + " debe indicar solo una categoría válida");
        }
        if (tipoActivacion == TipoActivacion.FACTOR_DESFAVORABLE && (!VARIABLES.contains(variable) || categoria != null)) {
            throw new IllegalArgumentException("La entrada " + codigo + " debe indicar solo una variable válida");
        }
    }

    private static void requerirTexto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank() || valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " es obligatorio y admite hasta " + maximo + " caracteres");
        }
    }
}
