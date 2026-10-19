package com.crediticio.evaluationquery.explicacion.infrastructure.output;

import com.crediticio.evaluationquery.explicacion.domain.ExplicacionEvaluacion;
import com.crediticio.evaluationquery.explicacion.domain.FactorAplicado;
import com.crediticio.evaluationquery.explicacion.ports.output.ExplicacionEvaluacionConsultaPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lee con SELECT las tablas de scoring-api (evaluacion, detalle_evaluacion, regla_scoring, riesgo y
 * politica_clasificacion). Los datos de cada regla salen de la instantánea del detalle; de regla_scoring y
 * riesgo solo se toma la variable, que no cambia al editar una regla (HU13 CA03, CA07).
 */
@Component
public class ExplicacionEvaluacionJdbcAdapter implements ExplicacionEvaluacionConsultaPort {

    private static final String CONSULTA_EVALUACION = """
            SELECT e.id_evaluacion, e.fecha_evaluacion, e.score_total, e.categoria, e.estado,
                   p.version AS version_politica
            FROM evaluacion e
            LEFT JOIN politica_clasificacion p ON p.id_politica = e.id_politica
            WHERE e.id_evaluacion = :idEvaluacion
            """;

    private static final String CONSULTA_FACTORES = """
            SELECT d.id_regla, r.variable, d.operador_aplicado, d.valor_condicion_aplicado,
                   d.puntaje_regla_aplicado, d.condicion_cumplida, d.puntaje_obtenido
            FROM detalle_evaluacion d
            JOIN regla_scoring rs ON rs.id_regla = d.id_regla
            JOIN riesgo r ON r.id_riesgo = rs.id_riesgo
            WHERE d.id_evaluacion = :idEvaluacion
            ORDER BY d.id_detalle_evaluacion
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ExplicacionEvaluacionJdbcAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ExplicacionEvaluacion> buscarPorId(long idEvaluacion) {
        MapSqlParameterSource parametros = new MapSqlParameterSource("idEvaluacion", idEvaluacion);
        List<DatosEvaluacion> evaluaciones = jdbcTemplate.query(CONSULTA_EVALUACION, parametros,
                (fila, numero) -> new DatosEvaluacion(
                        fila.getLong("id_evaluacion"),
                        fila.getObject("fecha_evaluacion", LocalDateTime.class),
                        fila.getObject("score_total", Integer.class),
                        fila.getString("categoria"),
                        fila.getObject("version_politica", Integer.class),
                        fila.getString("estado")));
        if (evaluaciones.isEmpty()) {
            return Optional.empty();
        }
        List<FactorAplicado> factores = jdbcTemplate.query(CONSULTA_FACTORES, parametros,
                (fila, numero) -> new FactorAplicado(
                        fila.getLong("id_regla"),
                        fila.getString("variable"),
                        fila.getString("operador_aplicado"),
                        fila.getString("valor_condicion_aplicado"),
                        fila.getInt("puntaje_regla_aplicado"),
                        fila.getBoolean("condicion_cumplida"),
                        fila.getInt("puntaje_obtenido")));
        DatosEvaluacion datos = evaluaciones.get(0);
        return Optional.of(ExplicacionEvaluacion.reconstruir(datos.idEvaluacion(), datos.fechaEvaluacion(),
                datos.scoreTotal(), datos.categoria(), datos.versionPolitica(), datos.estado(), factores));
    }

    private record DatosEvaluacion(long idEvaluacion, LocalDateTime fechaEvaluacion, Integer scoreTotal,
            String categoria, Integer versionPolitica, String estado) {
    }
}
