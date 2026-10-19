package com.crediticio.evaluationquery.recomendaciones.application;

import com.crediticio.evaluationquery.recomendaciones.application.dto.RecomendacionesResponse;
import com.crediticio.evaluationquery.recomendaciones.domain.EvaluacionInexistenteException;
import com.crediticio.evaluationquery.recomendaciones.domain.EvaluacionParaRecomendar;
import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionRecomendaciones;
import com.crediticio.evaluationquery.recomendaciones.ports.input.ConsultarRecomendacionesUseCase;
import com.crediticio.evaluationquery.recomendaciones.ports.output.EvaluacionParaRecomendarPort;
import com.crediticio.evaluationquery.recomendaciones.ports.output.RecomendacionesRepositoryPort;
import com.crediticio.evaluationquery.shared.exception.EvaluacionPendienteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * HU14 CA07: consulta las recomendaciones persistidas; nunca las genera (CA01). 404 si la evaluación no existe
 * (CA08), 409 si está PENDIENTE (CA13, P-HU14-03) y lista vacía con generadas = false en las evaluaciones
 * anteriores a la saga (P-HU14-04).
 */
@Service
public class ConsultarRecomendacionesService implements ConsultarRecomendacionesUseCase {

    private final EvaluacionParaRecomendarPort evaluacionPort;
    private final RecomendacionesRepositoryPort repositorio;
    private final RecomendacionesMapper mapper;

    public ConsultarRecomendacionesService(EvaluacionParaRecomendarPort evaluacionPort,
            RecomendacionesRepositoryPort repositorio, RecomendacionesMapper mapper) {
        this.evaluacionPort = evaluacionPort;
        this.repositorio = repositorio;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RecomendacionesResponse consultar(long idEvaluacion) {
        EvaluacionParaRecomendar evaluacion = evaluacionPort.buscar(idEvaluacion)
                .orElseThrow(EvaluacionInexistenteException::new);
        if (evaluacion.esPendiente()) {
            throw new EvaluacionPendienteException();
        }
        Optional<GeneracionRecomendaciones> generacion = repositorio.buscar(idEvaluacion);
        if (generacion.isPresent()) {
            return mapper.aRespuesta(generacion.get());
        }
        if (evaluacion.esAnteriorALaSaga()) {
            return mapper.sinGenerar(idEvaluacion);
        }
        // COMPLETADA sin generación: la saga garantiza que no ocurre; no se oculta
        throw new IllegalStateException("La evaluación " + idEvaluacion + " está COMPLETADA sin recomendaciones");
    }
}
