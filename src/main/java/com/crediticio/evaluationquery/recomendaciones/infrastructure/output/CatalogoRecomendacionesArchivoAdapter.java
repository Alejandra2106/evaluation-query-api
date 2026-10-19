package com.crediticio.evaluationquery.recomendaciones.infrastructure.output;

import com.crediticio.evaluationquery.recomendaciones.domain.CatalogoRecomendaciones;
import com.crediticio.evaluationquery.recomendaciones.domain.EntradaCatalogo;
import com.crediticio.evaluationquery.recomendaciones.domain.TipoActivacion;
import com.crediticio.evaluationquery.recomendaciones.ports.output.CatalogoRecomendacionesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Catálogo versionado en un archivo JSON (decisión aprobada de HU14, CA03), leído una vez al arrancar. Un archivo
 * ausente, ilegible o con entradas inválidas impide el arranque. El estado debe ser BORRADOR o APROBADO; solo
 * con APROBADO se generan recomendaciones.
 */
@Component
public class CatalogoRecomendacionesArchivoAdapter implements CatalogoRecomendacionesPort {

    private static final Logger log = LoggerFactory.getLogger(CatalogoRecomendacionesArchivoAdapter.class);
    static final String APROBADO = "APROBADO";
    static final String BORRADOR = "BORRADOR";

    private final CatalogoRecomendaciones catalogo;

    public CatalogoRecomendacionesArchivoAdapter(JsonMapper jsonMapper,
            @Value("${evaluationquery.catalogo.ubicacion:classpath:catalogo/recomendaciones.json}") Resource ubicacion) {
        this.catalogo = leer(jsonMapper, ubicacion);
        log.info("Catálogo de recomendaciones {} cargado (aprobado: {})", catalogo.getVersion(), catalogo.isAprobado());
    }

    static CatalogoRecomendaciones leer(JsonMapper jsonMapper, Resource ubicacion) {
        if (ubicacion == null || !ubicacion.exists()) {
            throw new IllegalStateException("No se encontró el catálogo de recomendaciones configurado");
        }
        ArchivoCatalogo archivo;
        try (InputStream entrada = ubicacion.getInputStream()) {
            archivo = jsonMapper.readValue(entrada, ArchivoCatalogo.class);
        } catch (IOException | JacksonException ex) {
            throw new IllegalStateException("No fue posible leer el catálogo de recomendaciones", ex);
        }
        if (!APROBADO.equals(archivo.estado()) && !BORRADOR.equals(archivo.estado())) {
            throw new IllegalStateException("El estado del catálogo debe ser BORRADOR o APROBADO");
        }
        if (archivo.entradas() == null) {
            throw new IllegalStateException("El catálogo de recomendaciones no tiene entradas");
        }
        return new CatalogoRecomendaciones(archivo.version(), APROBADO.equals(archivo.estado()),
                archivo.entradas().stream()
                        .map(entrada -> new EntradaCatalogo(entrada.codigo(), entrada.texto(),
                                entrada.tipoActivacion(), entrada.categoria(), entrada.variable()))
                        .toList());
    }

    @Override
    public CatalogoRecomendaciones catalogo() {
        return catalogo;
    }

    record ArchivoCatalogo(String version, String estado, String nota, List<ArchivoEntrada> entradas) {
    }

    record ArchivoEntrada(String codigo, TipoActivacion tipoActivacion, String categoria, String variable,
            String texto) {
    }
}
