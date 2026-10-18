package com.crediticio.evaluationquery.explicacion.application;

import com.crediticio.evaluationquery.explicacion.application.dto.ExplicacionEvaluacionResponse;
import com.crediticio.evaluationquery.explicacion.application.dto.FactorAplicadoResponse;
import com.crediticio.evaluationquery.explicacion.domain.ExplicacionEvaluacion;
import com.crediticio.evaluationquery.explicacion.domain.FactorAplicado;
import org.springframework.stereotype.Component;

@Component
public class ExplicacionMapper {

    public ExplicacionEvaluacionResponse aResponse(ExplicacionEvaluacion explicacion) {
        return new ExplicacionEvaluacionResponse(
                explicacion.getIdEvaluacion(),
                explicacion.getFechaEvaluacion(),
                explicacion.getScoreTotal(),
                explicacion.getOrigenPuntaje().name(),
                explicacion.isClasificada(),
                explicacion.getCategoria(),
                explicacion.getVersionPolitica(),
                explicacion.getFactores().stream().map(ExplicacionMapper::aResponse).toList());
    }

    private static FactorAplicadoResponse aResponse(FactorAplicado factor) {
        return new FactorAplicadoResponse(
                factor.idRegla(),
                factor.variable(),
                factor.operadorAplicado(),
                factor.valorCondicionAplicado(),
                factor.puntajeReglaAplicado(),
                factor.condicionCumplida(),
                factor.contribucion());
    }
}
