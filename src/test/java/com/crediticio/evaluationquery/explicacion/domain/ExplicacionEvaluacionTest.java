package com.crediticio.evaluationquery.explicacion.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU13 CA02, CA05, CA06 y P-HU13-04/05: puntaje, contribuciones y coherencia con lo persistido. */
class ExplicacionEvaluacionTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 10, 10, 0);
    private static final FactorAplicado CUMPLIDA_NEGATIVA =
            new FactorAplicado(1, "NUMERO_MORAS", ">", "2", -20, true, -20);
    private static final FactorAplicado NO_CUMPLIDA =
            new FactorAplicado(2, "INGRESOS_MENSUALES", ">=", "3000000", 20, false, 0);

    @Test
    void conPuntajeAlmacenadoCoincidenteDebeUsarloComoPersistido() {
        ExplicacionEvaluacion explicacion = ExplicacionEvaluacion.reconstruir(7, FECHA, -20, "MEDIO", 1, null,
                List.of(CUMPLIDA_NEGATIVA, NO_CUMPLIDA));

        assertThat(explicacion.getScoreTotal()).isEqualTo(-20);
        assertThat(explicacion.getOrigenPuntaje()).isEqualTo(OrigenPuntaje.PERSISTIDO);
        assertThat(explicacion.isClasificada()).isTrue();
        assertThat(explicacion.getFactores()).extracting(FactorAplicado::contribucion).containsExactly(-20, 0);
    }

    @Test
    void unaEvaluacionSinReglasAplicadasDebeTenerPuntajeCero() {
        ExplicacionEvaluacion explicacion = ExplicacionEvaluacion.reconstruir(7, FECHA, 0, "BAJO", 1, null, List.of());

        assertThat(explicacion.getScoreTotal()).isZero();
        assertThat(explicacion.getFactores()).isEmpty();
    }

    @Test
    void siLaSumaNoCoincideConElPuntajeAlmacenadoDebeSerInconsistente() {
        assertThatThrownBy(() -> ExplicacionEvaluacion.reconstruir(7, FECHA, 15, "BAJO", 1, null, List.of(CUMPLIDA_NEGATIVA)))
                .isInstanceOf(ExplicacionInconsistenteException.class)
                .hasMessageContaining("evaluación 7")
                .hasMessageContaining("(-20)")
                .hasMessageContaining("(15)");
    }

    @Test
    void unaClasificacionPersistidaIncompletaDebeSerInconsistente() {
        assertThatThrownBy(() -> ExplicacionEvaluacion.reconstruir(7, FECHA, -20, null, null, null, List.of(CUMPLIDA_NEGATIVA)))
                .isInstanceOf(ExplicacionInconsistenteException.class);
        assertThatThrownBy(() -> ExplicacionEvaluacion.reconstruir(7, FECHA, -20, "MEDIO", null, null, List.of(CUMPLIDA_NEGATIVA)))
                .isInstanceOf(ExplicacionInconsistenteException.class);
    }

    @Test
    void unaEvaluacionAnteriorAHu12DebeMostrarElPuntajeDerivadoYSinClasificacion() {
        ExplicacionEvaluacion explicacion = ExplicacionEvaluacion.reconstruir(3, FECHA, null, null, null, null,
                List.of(CUMPLIDA_NEGATIVA, NO_CUMPLIDA));

        assertThat(explicacion.getScoreTotal()).isEqualTo(-20);
        assertThat(explicacion.getOrigenPuntaje()).isEqualTo(OrigenPuntaje.DERIVADO_DE_DETALLES);
        assertThat(explicacion.isClasificada()).isFalse();
        assertThat(explicacion.getCategoria()).isNull();
        assertThat(explicacion.getVersionPolitica()).isNull();
    }
}
