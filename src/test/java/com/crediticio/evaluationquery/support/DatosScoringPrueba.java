package com.crediticio.evaluationquery.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Inserta datos de prueba en las tablas de scoring-api que lee esta API (H2 con esquema-scoring-h2.sql o un
 * esquema temporal de PostgreSQL con las migraciones de scoring-api). Solo para pruebas.
 */
public final class DatosScoringPrueba {

    private final JdbcTemplate jdbcTemplate;

    public DatosScoringPrueba(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long solicitante(String nombre, String documento) {
        return insertar("INSERT INTO solicitante (nombre_completo, numero_documento, ingresos_mensuales, "
                + "deudas_mensuales, numero_moras, historial_crediticio, antiguedad_laboral) "
                + "VALUES (?, ?, 4000000, 1200000, 3, 'BUENO', 2)", "id_solicitante", nombre, documento);
    }

    public long riesgo(String variable) {
        return insertar("INSERT INTO riesgo (variable, descripcion) VALUES (?, ?)", "id_riesgo", variable,
                "Variable " + variable);
    }

    public long regla(long idRiesgo, String operador, String valorCondicion, int puntaje) {
        return insertar("INSERT INTO regla_scoring (id_riesgo, operador, valor_condicion, puntaje) VALUES (?, ?, ?, ?)",
                "id_regla", idRiesgo, operador, valorCondicion, puntaje);
    }

    public long politica(int version) {
        return insertar("INSERT INTO politica_clasificacion (version, publicado_por) VALUES (?, ?)", "id_politica",
                version, UUID.randomUUID());
    }

    /** Evaluación registrada antes de HU12: sin puntaje, categoría ni política (columnas anteriores a V8). */
    public long evaluacionAnteriorAHu12(long idSolicitante, LocalDateTime fecha) {
        return insertar("INSERT INTO evaluacion (id_solicitante, fecha_evaluacion) VALUES (?, ?)", "id_evaluacion",
                idSolicitante, Timestamp.valueOf(fecha));
    }

    public long evaluacionClasificada(long idSolicitante, LocalDateTime fecha, int scoreTotal, String categoria,
            long idPolitica) {
        return insertar("INSERT INTO evaluacion (id_solicitante, fecha_evaluacion, score_total, categoria, id_politica) "
                + "VALUES (?, ?, ?, ?, ?)", "id_evaluacion", idSolicitante, Timestamp.valueOf(fecha), scoreTotal,
                categoria, idPolitica);
    }

    public long detalle(long idEvaluacion, long idRegla, String operador, String valorCondicion, int puntajeRegla,
            boolean condicionCumplida, int puntajeObtenido) {
        return insertar("INSERT INTO detalle_evaluacion (id_evaluacion, id_regla, operador_aplicado, "
                + "valor_condicion_aplicado, puntaje_regla_aplicado, condicion_cumplida, puntaje_obtenido) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", "id_detalle_evaluacion", idEvaluacion, idRegla, operador,
                valorCondicion, puntajeRegla, condicionCumplida, puntajeObtenido);
    }

    /** Solo H2: vacía las tablas en orden de dependencias. */
    public void vaciar() {
        for (String tabla : new String[] {"detalle_evaluacion", "evaluacion", "politica_clasificacion",
                "regla_scoring", "riesgo", "solicitante"}) {
            jdbcTemplate.update("DELETE FROM " + tabla);
        }
    }

    private long insertar(String sql, String columnaId, Object... valores) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbcTemplate.update(conexion -> {
            PreparedStatement sentencia = conexion.prepareStatement(sql, new String[] {columnaId});
            for (int i = 0; i < valores.length; i++) {
                if (valores[i] == null) {
                    sentencia.setNull(i + 1, Types.NULL);
                } else {
                    sentencia.setObject(i + 1, valores[i]);
                }
            }
            return sentencia;
        }, claves);
        return Objects.requireNonNull(claves.getKey(), "clave generada").longValue();
    }
}
