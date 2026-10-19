package com.crediticio.evaluationquery.recomendaciones.infrastructure.output;

import com.crediticio.evaluationquery.recomendaciones.domain.CatalogoRecomendaciones;
import com.crediticio.evaluationquery.recomendaciones.domain.EntradaCatalogo;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** HU14 CA03: lectura y validación del catálogo versionado. */
class CatalogoRecomendacionesArchivoAdapterTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void elCatalogoIncluidoEsLaVersion1Aprobada() {
        CatalogoRecomendaciones catalogo = CatalogoRecomendacionesArchivoAdapter.leer(JSON,
                new ClassPathResource("catalogo/recomendaciones.json"));

        assertThat(catalogo.getVersion()).isEqualTo("1");
        assertThat(catalogo.isAprobado()).isTrue();
        assertThat(catalogo.getEntradas()).extracting(EntradaCatalogo::codigo).containsExactly("REC-CAT-ALTO-01",
                "REC-CAT-MEDIO-01", "REC-MORAS-01", "REC-ENDEUD-01", "REC-INGRESOS-01", "REC-HISTORIAL-01",
                "REC-ANTIG-01");
    }

    @Test
    void unCatalogoAprobadoGeneraRecomendaciones() {
        CatalogoRecomendaciones catalogo = CatalogoRecomendacionesArchivoAdapter.leer(JSON, json("""
                {"version": "V7", "estado": "APROBADO", "entradas": [
                  {"codigo": "C1", "tipoActivacion": "CATEGORIA", "categoria": "ALTO", "texto": "t"}]}
                """));

        assertThat(catalogo.isAprobado()).isTrue();
        assertThat(catalogo.getVersion()).isEqualTo("V7");
    }

    @Test
    void unCatalogoBorradorNoGeneraRecomendaciones() {
        assertThat(CatalogoRecomendacionesArchivoAdapter.leer(JSON,
                new ClassPathResource("catalogo/recomendaciones-prueba-borrador.json")).isAprobado()).isFalse();
    }

    @Test
    void unEstadoDesconocidoImpideElArranque() {
        assertThatThrownBy(() -> CatalogoRecomendacionesArchivoAdapter.leer(JSON, json("""
                {"version": "V7", "estado": "aprobado", "entradas": []}
                """))).isInstanceOf(IllegalStateException.class).hasMessageContaining("BORRADOR o APROBADO");
    }

    @Test
    void unaEntradaInvalidaImpideElArranque() {
        assertThatThrownBy(() -> CatalogoRecomendacionesArchivoAdapter.leer(JSON, json("""
                {"version": "V7", "estado": "APROBADO", "entradas": [
                  {"codigo": "C1", "tipoActivacion": "CATEGORIA", "categoria": "CRITICO", "texto": "t"}]}
                """))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unArchivoAusenteOIlegibleImpideElArranque() {
        assertThatThrownBy(() -> CatalogoRecomendacionesArchivoAdapter.leer(JSON,
                new ClassPathResource("catalogo/no-existe.json"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CatalogoRecomendacionesArchivoAdapter.leer(JSON, json("{no es json")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CatalogoRecomendacionesArchivoAdapter.leer(JSON, json("""
                {"version": "V7", "estado": "APROBADO"}
                """))).isInstanceOf(IllegalStateException.class);
    }

    private static Resource json(String contenido) {
        return new ByteArrayResource(contenido.getBytes(StandardCharsets.UTF_8));
    }
}
