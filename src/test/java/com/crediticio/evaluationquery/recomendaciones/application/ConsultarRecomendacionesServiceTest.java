package com.crediticio.evaluationquery.recomendaciones.application;

import com.crediticio.evaluationquery.recomendaciones.application.dto.RecomendacionResponse;
import com.crediticio.evaluationquery.recomendaciones.application.dto.RecomendacionesResponse;
import com.crediticio.evaluationquery.recomendaciones.domain.EvaluacionInexistenteException;
import com.crediticio.evaluationquery.recomendaciones.domain.EvaluacionParaRecomendar;
import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionRecomendaciones;
import com.crediticio.evaluationquery.recomendaciones.domain.RecomendacionGenerada;
import com.crediticio.evaluationquery.recomendaciones.domain.TipoActivacion;
import com.crediticio.evaluationquery.shared.exception.EvaluacionPendienteException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU14 CA01, CA05, CA07, CA08, CA13, P-HU14-03 y P-HU14-04 con puertos simulados. */
class ConsultarRecomendacionesServiceTest {

    private final Map<Long, EvaluacionParaRecomendar> evaluaciones = new HashMap<>();
    private final RecomendacionesEnMemoria repositorio = new RecomendacionesEnMemoria();
    private final ConsultarRecomendacionesService servicio = new ConsultarRecomendacionesService(
            id -> Optional.ofNullable(evaluaciones.get(id)), repositorio, new RecomendacionesMapper());

    private void evaluacion(long id, String estado) {
        evaluaciones.put(id, new EvaluacionParaRecomendar(id, estado, "ALTO", List.of()));
    }

    @Test
    void debeDevolverLasRecomendacionesPersistidasSinGenerarNinguna() {
        evaluacion(7, "COMPLETADA");
        repositorio.guardadas.put(7L, new GeneracionRecomendaciones(7, "V1", "ALTO", RecomendacionesEnMemoria.FECHA,
                List.of(new RecomendacionGenerada(1, "REC-CAT-ALTO-01", "Texto histórico", TipoActivacion.CATEGORIA,
                        "ALTO", null))));

        RecomendacionesResponse respuesta = servicio.consultar(7);

        assertThat(respuesta.generadas()).isTrue();
        assertThat(respuesta.versionCatalogo()).isEqualTo("V1");
        assertThat(respuesta.fechaGeneracion()).isEqualTo(RecomendacionesEnMemoria.FECHA);
        assertThat(respuesta.recomendaciones()).containsExactly(new RecomendacionResponse(1, "REC-CAT-ALTO-01",
                "Texto histórico", "CATEGORIA", "ALTO", null));
        assertThat(repositorio.inserciones).isZero();
    }

    @Test
    void unaEvaluacionInexistenteDebeLanzarNoEncontrada() {
        assertThatThrownBy(() -> servicio.consultar(99)).isInstanceOf(EvaluacionInexistenteException.class);
    }

    @Test
    void unaEvaluacionPendienteDebeRechazarseAunqueYaTengaRecomendaciones() {
        evaluacion(7, "PENDIENTE");
        repositorio.guardadas.put(7L, new GeneracionRecomendaciones(7, "V1", "ALTO", RecomendacionesEnMemoria.FECHA,
                List.of()));

        assertThatThrownBy(() -> servicio.consultar(7)).isInstanceOf(EvaluacionPendienteException.class);
    }

    @Test
    void unaEvaluacionAnteriorALaSagaDebeDevolverUnaListaVaciaNoGenerada() {
        evaluacion(3, null);

        RecomendacionesResponse respuesta = servicio.consultar(3);

        assertThat(respuesta).isEqualTo(new RecomendacionesResponse(3, false, null, null, List.of()));
    }

    @Test
    void unaEvaluacionCompletadaSinGeneracionEsUnaInconsistenciaQueNoSeOculta() {
        evaluacion(5, "COMPLETADA");

        assertThatThrownBy(() -> servicio.consultar(5)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("5");
    }
}
