package com.crediticio.evaluationquery.recomendaciones.domain;

/** Copia histórica de una recomendación asociada a una evaluación (HU14 CA04, CA05). */
public record RecomendacionGenerada(int orden, String codigo, String texto, TipoActivacion tipoActivacion,
        String categoria, String variable) {

    static RecomendacionGenerada desde(EntradaCatalogo entrada, int orden) {
        return new RecomendacionGenerada(orden, entrada.codigo(), entrada.texto(), entrada.tipoActivacion(),
                entrada.categoria(), entrada.variable());
    }
}
