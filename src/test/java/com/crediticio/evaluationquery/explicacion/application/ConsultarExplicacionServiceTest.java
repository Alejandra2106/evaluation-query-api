package com.crediticio.evaluationquery.explicacion.application;

import com.crediticio.evaluationquery.explicacion.application.dto.ExplicacionEvaluacionResponse;
import com.crediticio.evaluationquery.explicacion.domain.EvaluacionNoEncontradaException;
import com.crediticio.evaluationquery.explicacion.domain.ExplicacionEvaluacion;
import com.crediticio.evaluationquery.explicacion.domain.FactorAplicado;
import com.crediticio.evaluationquery.shared.exception.EvaluacionPendienteException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU13 CA01–CA05, CA08 y P-HU13-02 con un puerto simulado. */
class ConsultarExplicacionServiceTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 10, 10, 0);

    @Test
    void debeDevolverLaExplicacionConSusFactores() {
        ExplicacionEvaluacion explicacion = ExplicacionEvaluacion.reconstruir(7, FECHA, -20, "MEDIO", 1, "COMPLETADA", List.of(
                new FactorAplicado(1, "NUMERO_MORAS", ">", "2", -20, true, -20),
                new FactorAplicado(2, "INGRESOS_MENSUALES", ">=", "3000000", 20, false, 0)));
        ConsultarExplicacionService servicio = new ConsultarExplicacionService(
                id -> id == 7 ? Optional.of(explicacion) : Optional.empty(), new ExplicacionMapper());

        ExplicacionEvaluacionResponse respuesta = servicio.consultar(7);

        assertThat(respuesta.idEvaluacion()).isEqualTo(7);
        assertThat(respuesta.fechaEvaluacion()).isEqualTo(FECHA);
        assertThat(respuesta.scoreTotal()).isEqualTo(-20);
        assertThat(respuesta.origenPuntaje()).isEqualTo("PERSISTIDO");
        assertThat(respuesta.clasificada()).isTrue();
        assertThat(respuesta.categoria()).isEqualTo("MEDIO");
        assertThat(respuesta.versionPolitica()).isEqualTo(1);
        assertThat(respuesta.factores()).hasSize(2);
        assertThat(respuesta.factores().get(1).condicionCumplida()).isFalse();
        assertThat(respuesta.factores().get(1).contribucion()).isZero();
    }

    @Test
    void siLaEvaluacionNoExisteDebeLanzarNoEncontrada() {
        ConsultarExplicacionService servicio = new ConsultarExplicacionService(id -> Optional.empty(),
                new ExplicacionMapper());

        assertThatThrownBy(() -> servicio.consultar(99)).isInstanceOf(EvaluacionNoEncontradaException.class);
    }

    @Test
    void unaEvaluacionPendienteNoDebeExplicarseComoCompleta() {
        ExplicacionEvaluacion pendiente = ExplicacionEvaluacion.reconstruir(8, FECHA, 0, "BAJO", 1, "PENDIENTE",
                List.of());
        ConsultarExplicacionService servicio = new ConsultarExplicacionService(id -> Optional.of(pendiente),
                new ExplicacionMapper());

        assertThatThrownBy(() -> servicio.consultar(8)).isInstanceOf(EvaluacionPendienteException.class);
    }

    @Test
    void unaEvaluacionAnteriorALaSagaDebeExplicarse() {
        ExplicacionEvaluacion historica = ExplicacionEvaluacion.reconstruir(3, FECHA, 0, "BAJO", 1, null, List.of());
        ConsultarExplicacionService servicio = new ConsultarExplicacionService(id -> Optional.of(historica),
                new ExplicacionMapper());

        assertThat(servicio.consultar(3).idEvaluacion()).isEqualTo(3);
    }
}
