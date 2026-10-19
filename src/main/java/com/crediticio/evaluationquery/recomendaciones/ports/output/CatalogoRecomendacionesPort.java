package com.crediticio.evaluationquery.recomendaciones.ports.output;

import com.crediticio.evaluationquery.recomendaciones.domain.CatalogoRecomendaciones;

/** Catálogo versionado vigente en este despliegue (HU14 CA03). */
public interface CatalogoRecomendacionesPort {

    CatalogoRecomendaciones catalogo();
}
