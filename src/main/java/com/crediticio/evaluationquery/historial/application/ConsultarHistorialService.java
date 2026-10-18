package com.crediticio.evaluationquery.historial.application;

import com.crediticio.evaluationquery.historial.application.dto.EvaluacionHistorialResponse;
import com.crediticio.evaluationquery.historial.domain.SolicitanteNoEncontradoException;
import com.crediticio.evaluationquery.historial.ports.input.ConsultarHistorialUseCase;
import com.crediticio.evaluationquery.historial.ports.output.HistorialEvaluacionesPort;
import com.crediticio.evaluationquery.historial.ports.output.SolicitanteExistenciaPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HU15: comprueba primero que el solicitante existe (CA02, 404 si no) y después lista su historial (200,
 * vacío si no tiene evaluaciones). Solo lectura (CA11).
 */
@Service
public class ConsultarHistorialService implements ConsultarHistorialUseCase {

    private final SolicitanteExistenciaPort solicitanteExistenciaPort;
    private final HistorialEvaluacionesPort historialEvaluacionesPort;
    private final HistorialMapper mapper;

    public ConsultarHistorialService(SolicitanteExistenciaPort solicitanteExistenciaPort,
            HistorialEvaluacionesPort historialEvaluacionesPort, HistorialMapper mapper) {
        this.solicitanteExistenciaPort = solicitanteExistenciaPort;
        this.historialEvaluacionesPort = historialEvaluacionesPort;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluacionHistorialResponse> consultar(long idSolicitante) {
        if (!solicitanteExistenciaPort.existe(idSolicitante)) {
            throw new SolicitanteNoEncontradoException();
        }
        return historialEvaluacionesPort.listarPorSolicitante(idSolicitante).stream()
                .map(mapper::aResponse)
                .toList();
    }
}
