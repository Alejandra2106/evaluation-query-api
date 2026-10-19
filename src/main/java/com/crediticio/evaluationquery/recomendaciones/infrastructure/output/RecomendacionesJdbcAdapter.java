package com.crediticio.evaluationquery.recomendaciones.infrastructure.output;

import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionConcurrenteException;
import com.crediticio.evaluationquery.recomendaciones.domain.GeneracionRecomendaciones;
import com.crediticio.evaluationquery.recomendaciones.domain.RecomendacionGenerada;
import com.crediticio.evaluationquery.recomendaciones.domain.TipoActivacion;
import com.crediticio.evaluationquery.recomendaciones.ports.output.RecomendacionesRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Tablas generacion_recomendaciones y recomendacion_evaluacion del esquema propio (Flyway de esta API, V1),
 * cualificadas con evaluationquery.consulta.esquema. Solo inserta y lee: los triggers impiden modificarlas.
 */
@Component
public class RecomendacionesJdbcAdapter implements RecomendacionesRepositoryPort {

    private static final Pattern IDENTIFICADOR = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String consultaCabecera;
    private final String consultaRecomendaciones;
    private final String insercionCabecera;
    private final String insercionRecomendacion;

    public RecomendacionesJdbcAdapter(NamedParameterJdbcTemplate jdbcTemplate,
            @Value("${evaluationquery.consulta.esquema:consulta}") String esquema) {
        if (esquema == null || !IDENTIFICADOR.matcher(esquema).matches()) {
            throw new IllegalStateException("evaluationquery.consulta.esquema debe ser un identificador en minúsculas");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.consultaCabecera = """
                SELECT version_catalogo, categoria_aplicada, fecha_generacion
                FROM %s.generacion_recomendaciones
                WHERE id_evaluacion = :idEvaluacion
                """.formatted(esquema);
        this.consultaRecomendaciones = """
                SELECT orden, codigo, texto, tipo_activacion, categoria, variable
                FROM %s.recomendacion_evaluacion
                WHERE id_evaluacion = :idEvaluacion
                ORDER BY orden
                """.formatted(esquema);
        this.insercionCabecera = """
                INSERT INTO %s.generacion_recomendaciones (id_evaluacion, version_catalogo, categoria_aplicada)
                VALUES (:idEvaluacion, :versionCatalogo, :categoriaAplicada)
                """.formatted(esquema);
        this.insercionRecomendacion = """
                INSERT INTO %s.recomendacion_evaluacion
                    (id_evaluacion, orden, codigo, texto, tipo_activacion, categoria, variable)
                VALUES (:idEvaluacion, :orden, :codigo, :texto, :tipoActivacion, :categoria, :variable)
                """.formatted(esquema);
    }

    @Override
    public Optional<GeneracionRecomendaciones> buscar(long idEvaluacion) {
        MapSqlParameterSource parametros = new MapSqlParameterSource("idEvaluacion", idEvaluacion);
        List<Cabecera> cabeceras = jdbcTemplate.query(consultaCabecera, parametros,
                (fila, numero) -> new Cabecera(
                        fila.getString("version_catalogo"),
                        fila.getString("categoria_aplicada"),
                        fila.getObject("fecha_generacion", OffsetDateTime.class)));
        if (cabeceras.isEmpty()) {
            return Optional.empty();
        }
        List<RecomendacionGenerada> recomendaciones = jdbcTemplate.query(consultaRecomendaciones, parametros,
                (fila, numero) -> new RecomendacionGenerada(
                        fila.getInt("orden"),
                        fila.getString("codigo"),
                        fila.getString("texto"),
                        TipoActivacion.valueOf(fila.getString("tipo_activacion")),
                        fila.getString("categoria"),
                        fila.getString("variable")));
        Cabecera cabecera = cabeceras.get(0);
        return Optional.of(new GeneracionRecomendaciones(idEvaluacion, cabecera.versionCatalogo(),
                cabecera.categoriaAplicada(), cabecera.fechaGeneracion(), recomendaciones));
    }

    /**
     * Primero la cabecera: su clave primaria serializa dos generaciones simultáneas de la misma evaluación (la
     * segunda espera a la primera y falla con clave duplicada, sin insertar ninguna recomendación).
     */
    @Override
    public GeneracionRecomendaciones guardar(GeneracionRecomendaciones generacion) {
        try {
            jdbcTemplate.update(insercionCabecera, new MapSqlParameterSource()
                    .addValue("idEvaluacion", generacion.idEvaluacion())
                    .addValue("versionCatalogo", generacion.versionCatalogo())
                    .addValue("categoriaAplicada", generacion.categoriaAplicada()));
        } catch (DuplicateKeyException ex) {
            throw new GeneracionConcurrenteException(generacion.idEvaluacion(), ex);
        }
        jdbcTemplate.batchUpdate(insercionRecomendacion, generacion.recomendaciones().stream()
                .map(recomendacion -> new MapSqlParameterSource()
                        .addValue("idEvaluacion", generacion.idEvaluacion())
                        .addValue("orden", recomendacion.orden())
                        .addValue("codigo", recomendacion.codigo())
                        .addValue("texto", recomendacion.texto())
                        .addValue("tipoActivacion", recomendacion.tipoActivacion().name())
                        .addValue("categoria", recomendacion.categoria())
                        .addValue("variable", recomendacion.variable()))
                .toArray(MapSqlParameterSource[]::new));
        return buscar(generacion.idEvaluacion()).orElseThrow();
    }

    private record Cabecera(String versionCatalogo, String categoriaAplicada, OffsetDateTime fechaGeneracion) {
    }
}
