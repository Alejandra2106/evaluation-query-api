package com.crediticio.evaluationquery.historial.application;

import com.crediticio.evaluationquery.historial.application.dto.EvaluacionHistorialResponse;
import com.crediticio.evaluationquery.historial.domain.EvaluacionHistorica;
import com.crediticio.evaluationquery.historial.domain.SolicitanteNoEncontradoException;
import com.crediticio.evaluationquery.historial.ports.output.HistorialEvaluacionesPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU15 CA02–CA05 y CA09 con puertos simulados. */
class ConsultarHistorialServiceTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 10, 10, 0);

    @Test
    void siElSolicitanteNoExisteNoDebeConsultarSuHistorial() {
        AtomicBoolean consultado = new AtomicBoolean();
        HistorialEvaluacionesPort historial = id -> {
            consultado.set(true);
            return List.of();
        };
        ConsultarHistorialService servicio = new ConsultarHistorialService(id -> false, historial,
                new HistorialMapper());

        assertThatThrownBy(() -> servicio.consultar(4)).isInstanceOf(SolicitanteNoEncontradoException.class);
        assertThat(consultado).isFalse();
    }

    @Test
    void unSolicitanteSinEvaluacionesDebeDevolverUnaListaVacia() {
        ConsultarHistorialService servicio = new ConsultarHistorialService(id -> true, id -> List.of(),
                new HistorialMapper());

        assertThat(servicio.consultar(4)).isEmpty();
    }

    @Test
    void debeConservarElOrdenDelPuertoYEnlazarLaExplicacionYLasRecomendaciones() {
        ConsultarHistorialService servicio = new ConsultarHistorialService(id -> true, id -> List.of(
                EvaluacionHistorica.reconstruir(9, FECHA, 0, 0, "BAJO", 1, "COMPLETADA"),
                EvaluacionHistorica.reconstruir(2, FECHA.minusDays(3), null, 20, null, null, null)),
                new HistorialMapper());

        List<EvaluacionHistorialResponse> respuesta = servicio.consultar(4);

        assertThat(respuesta).extracting(EvaluacionHistorialResponse::idEvaluacion).containsExactly(9L, 2L);
        assertThat(respuesta.get(0).explicacion()).isEqualTo("/api/v1/evaluaciones/9/explicacion");
        assertThat(respuesta.get(0).recomendaciones()).isEqualTo("/api/v1/evaluaciones/9/recomendaciones");
        assertThat(respuesta.get(0).estado()).isEqualTo("COMPLETADA");
        assertThat(respuesta.get(1).estado()).isNull();
        assertThat(respuesta.get(1).origenPuntaje()).isEqualTo("DERIVADO_DE_DETALLES");
        assertThat(respuesta.get(1).clasificada()).isFalse();
    }
}
