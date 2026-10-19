package com.crediticio.evaluationquery.recomendaciones.application;

import com.crediticio.evaluationquery.recomendaciones.application.dto.RecomendacionResponse;
import com.crediticio.evaluationquery.recomendaciones.application.dto.RecomendacionesResponse;
import com.crediticio.evaluationquery.recomendaciones.application.dto.ResultadoGeneracionResponse;
import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionRecomendaciones;
import com.crediticio.evaluationquery.recomendaciones.domain.RecomendacionGenerada;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecomendacionesMapper {

    public ResultadoGeneracionResponse aResultado(GeneracionRecomendaciones generacion, boolean creada) {
        return new ResultadoGeneracionResponse(generacion.idEvaluacion(), generacion.versionCatalogo(),
                generacion.recomendaciones().size(), creada);
    }

    public RecomendacionesResponse aRespuesta(GeneracionRecomendaciones generacion) {
        return new RecomendacionesResponse(generacion.idEvaluacion(), true, generacion.versionCatalogo(),
                generacion.fechaGeneracion(), generacion.recomendaciones().stream().map(this::aRespuesta).toList());
    }

    public RecomendacionesResponse sinGenerar(long idEvaluacion) {
        return new RecomendacionesResponse(idEvaluacion, false, null, null, List.of());
    }

    private RecomendacionResponse aRespuesta(RecomendacionGenerada recomendacion) {
        return new RecomendacionResponse(recomendacion.orden(), recomendacion.codigo(), recomendacion.texto(),
                recomendacion.tipoActivacion().name(), recomendacion.categoria(), recomendacion.variable());
    }
}
