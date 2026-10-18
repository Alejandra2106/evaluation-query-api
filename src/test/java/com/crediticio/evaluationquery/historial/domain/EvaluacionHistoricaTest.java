package com.crediticio.evaluationquery.historial.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU15 CA05, CA07 y P-HU15-01: resultados persistidos y puntaje derivado de las evaluaciones anteriores a HU12. */
class EvaluacionHistoricaTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 10, 10, 0);

    @Test
    void conPuntajeAlmacenadoDebeUsarloSinRecalcular() {
        // la suma de los detalles no se usa cuando hay puntaje persistido
        EvaluacionHistorica evaluacion = EvaluacionHistorica.reconstruir(5, FECHA, -20, 999, "MEDIO", 2);

        assertThat(evaluacion.getScoreTotal()).isEqualTo(-20);
        assertThat(evaluacion.getOrigenPuntaje()).isEqualTo(OrigenPuntaje.PERSISTIDO);
        assertThat(evaluacion.isClasificada()).isTrue();
        assertThat(evaluacion.getVersionPolitica()).isEqualTo(2);
    }

    @Test
    void unaEvaluacionAnteriorAHu12DebeMostrarLaSumaComoPuntajeDerivado() {
        EvaluacionHistorica evaluacion = EvaluacionHistorica.reconstruir(5, FECHA, null, 20, null, null);

        assertThat(evaluacion.getScoreTotal()).isEqualTo(20);
        assertThat(evaluacion.getOrigenPuntaje()).isEqualTo(OrigenPuntaje.DERIVADO_DE_DETALLES);
        assertThat(evaluacion.isClasificada()).isFalse();
        assertThat(evaluacion.getCategoria()).isNull();
    }

    @Test
    void unaClasificacionPersistidaIncompletaDebeRechazarse() {
        assertThatThrownBy(() -> EvaluacionHistorica.reconstruir(5, FECHA, 10, 10, null, null))
                .isInstanceOf(IllegalStateException.class);
    }
}
