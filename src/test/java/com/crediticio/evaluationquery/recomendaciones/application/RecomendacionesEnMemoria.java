package com.crediticio.evaluationquery.recomendaciones.application;

import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionConcurrenteException;
import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionRecomendaciones;
import com.crediticio.evaluationquery.recomendaciones.ports.output.RecomendacionesRepositoryPort;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Repositorio simulado; antesDeGuardar permite simular que otra petición guardó primero. */
class RecomendacionesEnMemoria implements RecomendacionesRepositoryPort {

    static final OffsetDateTime FECHA = OffsetDateTime.parse("2026-10-18T10:00:00Z");

    final Map<Long, GeneracionRecomendaciones> guardadas = new HashMap<>();
    Runnable antesDeGuardar = () -> { };
    int inserciones;

    @Override
    public Optional<GeneracionRecomendaciones> buscar(long idEvaluacion) {
        return Optional.ofNullable(guardadas.get(idEvaluacion));
    }

    @Override
    public GeneracionRecomendaciones guardar(GeneracionRecomendaciones generacion) {
        antesDeGuardar.run();
        if (guardadas.containsKey(generacion.idEvaluacion())) {
            throw new GeneracionConcurrenteException(generacion.idEvaluacion(), null);
        }
        inserciones++;
        GeneracionRecomendaciones persistida = new GeneracionRecomendaciones(generacion.idEvaluacion(),
                generacion.versionCatalogo(), generacion.categoriaAplicada(), FECHA, generacion.recomendaciones());
        guardadas.put(generacion.idEvaluacion(), persistida);
        return persistida;
    }
}
