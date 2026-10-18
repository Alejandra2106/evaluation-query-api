package com.crediticio.evaluationquery.historial.ports.input;

import com.crediticio.evaluationquery.historial.application.dto.EvaluacionHistorialResponse;

import java.util.List;

public interface ConsultarHistorialUseCase {

    List<EvaluacionHistorialResponse> consultar(long idSolicitante);
}
