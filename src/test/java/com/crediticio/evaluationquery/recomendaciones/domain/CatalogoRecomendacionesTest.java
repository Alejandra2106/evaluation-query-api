package com.crediticio.evaluationquery.recomendaciones.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU14 CA02, CA03 y CA09: selección determinista a partir de la clasificación y los factores persistidos. */
class CatalogoRecomendacionesTest {

    private static final EntradaCatalogo ALTO = new EntradaCatalogo("REC-CAT-ALTO-01", "Revisión ampliada",
            TipoActivacion.CATEGORIA, "ALTO", null);
    private static final EntradaCatalogo MORAS = new EntradaCatalogo("REC-MORAS-01", "Revisar moras",
            TipoActivacion.FACTOR_DESFAVORABLE, null, "NUMERO_MORAS");
    private static final EntradaCatalogo MORAS_ALTERNATIVA = new EntradaCatalogo("REC-MORAS-00", "Otra sobre moras",
            TipoActivacion.FACTOR_DESFAVORABLE, null, "NUMERO_MORAS");
    private static final EntradaCatalogo ENDEUDAMIENTO = new EntradaCatalogo("REC-ENDEUD-01", "Revisar deudas",
            TipoActivacion.FACTOR_DESFAVORABLE, null, "NIVEL_ENDEUDAMIENTO");
    private static final EntradaCatalogo INGRESOS = new EntradaCatalogo("REC-INGRESOS-01", "Revisar ingresos",
            TipoActivacion.FACTOR_DESFAVORABLE, null, "INGRESOS_MENSUALES");

    private static final CatalogoRecomendaciones CATALOGO = new CatalogoRecomendaciones("V1", true,
            List.of(MORAS, INGRESOS, ALTO, ENDEUDAMIENTO));

    @Test
    void debeOrdenarPrimeroLaCategoriaYDespuesLasVariablesAlfabeticamente() {
        List<RecomendacionGenerada> recomendaciones = CATALOGO.seleccionar("ALTO", List.of(
                new FactorEvaluado("NUMERO_MORAS", true, -20),
                new FactorEvaluado("NIVEL_ENDEUDAMIENTO", true, -15)));

        assertThat(recomendaciones).extracting(RecomendacionGenerada::codigo)
                .containsExactly("REC-CAT-ALTO-01", "REC-ENDEUD-01", "REC-MORAS-01");
        assertThat(recomendaciones).extracting(RecomendacionGenerada::orden).containsExactly(1, 2, 3);
        assertThat(recomendaciones.get(0).tipoActivacion()).isEqualTo(TipoActivacion.CATEGORIA);
        assertThat(recomendaciones.get(0).categoria()).isEqualTo("ALTO");
        assertThat(recomendaciones.get(1).variable()).isEqualTo("NIVEL_ENDEUDAMIENTO");
    }

    @Test
    void soloLosFactoresCumplidosConContribucionNegativaSonDesfavorables() {
        List<RecomendacionGenerada> recomendaciones = CATALOGO.seleccionar("BAJO", List.of(
                new FactorEvaluado("NUMERO_MORAS", false, 0),
                new FactorEvaluado("INGRESOS_MENSUALES", true, 20),
                new FactorEvaluado("NIVEL_ENDEUDAMIENTO", true, 0)));

        assertThat(recomendaciones).isEmpty();
    }

    @Test
    void variosFactoresSobreLaMismaVariableGeneranUnaSolaRecomendacion() {
        List<RecomendacionGenerada> recomendaciones = CATALOGO.seleccionar("MEDIO", List.of(
                new FactorEvaluado("NUMERO_MORAS", true, -20),
                new FactorEvaluado("NUMERO_MORAS", true, -10)));

        assertThat(recomendaciones).extracting(RecomendacionGenerada::codigo).containsExactly("REC-MORAS-01");
    }

    @Test
    void anteVariasEntradasDelMismoCriterioDebeElegirLaDeCodigoMenor() {
        CatalogoRecomendaciones catalogo = new CatalogoRecomendaciones("V2", true, List.of(MORAS, MORAS_ALTERNATIVA));

        assertThat(catalogo.seleccionar("BAJO", List.of(new FactorEvaluado("NUMERO_MORAS", true, -20))))
                .extracting(RecomendacionGenerada::codigo).containsExactly("REC-MORAS-00");
    }

    @Test
    void sinEntradaParaLaCategoriaNoGeneraRecomendacionDeCategoria() {
        assertThat(CATALOGO.seleccionar("MEDIO", List.of())).isEmpty();
    }

    @Test
    void debeConservarElTextoDeLaEntrada() {
        RecomendacionGenerada recomendacion = CATALOGO.seleccionar("ALTO", List.of()).get(0);

        assertThat(recomendacion.texto()).isEqualTo("Revisión ampliada");
    }

    @Test
    void debeRechazarCodigosRepetidosYVersionesInvalidas() {
        assertThatThrownBy(() -> new CatalogoRecomendaciones("V1", true, List.of(MORAS, MORAS)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("REC-MORAS-01");
        assertThatThrownBy(() -> new CatalogoRecomendaciones(" ", true, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CatalogoRecomendaciones("x".repeat(31), true, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaEntradaDebeTenerSoloElCriterioDeSuTipo() {
        assertThatThrownBy(() -> new EntradaCatalogo("C1", "t", TipoActivacion.CATEGORIA, "CRITICO", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaCatalogo("C1", "t", TipoActivacion.CATEGORIA, "ALTO", "NUMERO_MORAS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaCatalogo("F1", "t", TipoActivacion.FACTOR_DESFAVORABLE, null, "EDAD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaCatalogo("F1", "t", TipoActivacion.FACTOR_DESFAVORABLE, "ALTO",
                "NUMERO_MORAS")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EntradaCatalogo("F1", " ", TipoActivacion.FACTOR_DESFAVORABLE, null,
                "NUMERO_MORAS")).isInstanceOf(IllegalArgumentException.class);
    }
}
