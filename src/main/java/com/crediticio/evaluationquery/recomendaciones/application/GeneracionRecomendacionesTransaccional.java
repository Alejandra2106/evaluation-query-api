package com.crediticio.evaluationquery.recomendaciones.application;

import com.crediticio.evaluationquery.recomendaciones.domain.CatalogoNoDisponibleException;
import com.crediticio.evaluationquery.recomendaciones.domain.CatalogoRecomendaciones;
import com.crediticio.evaluationquery.recomendaciones.domain.EvaluacionInexistenteException;
import com.crediticio.evaluationquery.recomendaciones.domain.EvaluacionNoPendienteException;
import com.crediticio.evaluationquery.recomendaciones.domain.EvaluacionParaRecomendar;
import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionRecomendaciones;
import com.crediticio.evaluationquery.recomendaciones.ports.output.CatalogoRecomendacionesPort;
import com.crediticio.evaluationquery.recomendaciones.ports.output.EvaluacionParaRecomendarPort;
import com.crediticio.evaluationquery.recomendaciones.ports.output.RecomendacionesRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Transacciones locales de la generación (HU14). La saga de scoring-api la invoca; esta API no confirma nada en
 * scoring-api: cada API tiene su propia transacción y no existe una transacción distribuida.
 */
@Service
public class GeneracionRecomendacionesTransaccional {

    private final EvaluacionParaRecomendarPort evaluacionPort;
    private final RecomendacionesRepositoryPort repositorio;
    private final CatalogoRecomendacionesPort catalogoPort;

    public GeneracionRecomendacionesTransaccional(EvaluacionParaRecomendarPort evaluacionPort,
            RecomendacionesRepositoryPort repositorio, CatalogoRecomendacionesPort catalogoPort) {
        this.evaluacionPort = evaluacionPort;
        this.repositorio = repositorio;
        this.catalogoPort = catalogoPort;
    }

    /**
     * Si ya existe una generación la devuelve (reintento, CA06, CA15); si no, exige que la evaluación exista y esté
     * PENDIENTE, aplica el catálogo APROBADO a su clasificación y factores persistidos (CA02, CA09) y la guarda.
     *
     * @return la generación y si se creó en esta llamada
     */
    @Transactional
    public Resultado generar(long idEvaluacion) {
        Optional<GeneracionRecomendaciones> existente = repositorio.buscar(idEvaluacion);
        if (existente.isPresent()) {
            return new Resultado(existente.get(), false);
        }
        EvaluacionParaRecomendar evaluacion = evaluacionPort.buscar(idEvaluacion)
                .orElseThrow(EvaluacionInexistenteException::new);
        if (!evaluacion.esPendiente() || evaluacion.categoria() == null) {
            throw new EvaluacionNoPendienteException();
        }
        CatalogoRecomendaciones catalogo = catalogoPort.catalogo();
        if (!catalogo.isAprobado()) {
            throw new CatalogoNoDisponibleException(catalogo.getVersion());
        }
        GeneracionRecomendaciones generacion = new GeneracionRecomendaciones(idEvaluacion, catalogo.getVersion(),
                evaluacion.categoria(), null, catalogo.seleccionar(evaluacion.categoria(), evaluacion.factores()));
        return new Resultado(repositorio.guardar(generacion), true);
    }

    @Transactional(readOnly = true)
    public Optional<GeneracionRecomendaciones> existente(long idEvaluacion) {
        return repositorio.buscar(idEvaluacion);
    }

    public record Resultado(GeneracionRecomendaciones generacion, boolean creada) {
    }
}
