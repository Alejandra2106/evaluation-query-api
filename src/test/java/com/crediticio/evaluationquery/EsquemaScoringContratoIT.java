package com.crediticio.evaluationquery;

import com.crediticio.evaluationquery.explicacion.domain.ExplicacionEvaluacion;
import com.crediticio.evaluationquery.explicacion.domain.FactorAplicado;
import com.crediticio.evaluationquery.explicacion.domain.OrigenPuntaje;
import com.crediticio.evaluationquery.explicacion.infrastructure.output.ExplicacionEvaluacionJdbcAdapter;
import com.crediticio.evaluationquery.historial.domain.EvaluacionHistorica;
import com.crediticio.evaluationquery.historial.infrastructure.output.HistorialEvaluacionesJdbcAdapter;
import com.crediticio.evaluationquery.support.DatosScoringPrueba;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de lectura de HU13 y HU15 contra el esquema real de scoring-api en PostgreSQL. En un esquema
 * temporal y aislado aplica las migraciones V1–V9 de scoring-api (leídas de
 * ../scoring-api/src/main/resources/db/migration, sin modificarlas, sustituyendo el placeholder
 * ${flyway:defaultSchema}); inserta una evaluación anterior a HU12 antes de V8 y evaluaciones clasificadas
 * después (respetando los triggers de V8), y ejecuta los adaptadores JDBC de esta API dentro de transacciones
 * de solo lectura. No toca el esquema public ni sus datos.
 *
 * Antes de abrir cualquier conexión exige, por igualdad exacta de texto, -Dit.base-dedicada=motor_scoring_it y
 * -Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it (nunca usa variables de la
 * aplicación). Exige que el esquema no exista. Limpieza: solo elimina el esquema si conserva el OID y el
 * comentario-marca registrados al crearlo en esta ejecución; si no, no elimina nada y hace fallar la prueba.
 *
 * Al terminar en "IT", Surefire no la ejecuta con `./mvnw test`. Ejecución manual (requiere autorización
 * expresa para usar motor_scoring_it), desde la raíz de evaluation-query-api, con scoring-api en la carpeta
 * hermana y DB_USERNAME y DB_PASSWORD en el entorno:
 *   mvnw.cmd -B test "-Dtest=EsquemaScoringContratoIT" "-Dit.base-dedicada=motor_scoring_it"
 *     "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it"
 */
class EsquemaScoringContratoIT {

    private static final String ESQUEMA = "eq_contrato_it";
    private static final String BASE_AUTORIZADA = "motor_scoring_it";
    private static final String URL_AUTORIZADA = "jdbc:postgresql://localhost:5432/" + BASE_AUTORIZADA;
    private static final Path MIGRACIONES_SCORING = Path.of("..", "scoring-api", "src", "main", "resources", "db",
            "migration");
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 10, 10, 0);

    private static String url;
    private static String usuario;
    private static String clave;
    private static String marca;
    private static Long oidEsquemaCreado;

    private static JdbcTemplate jdbcTemplate;
    private static TransactionTemplate transaccion;
    private static TransactionTemplate soloLectura;
    private static ExplicacionEvaluacionJdbcAdapter explicacionAdapter;
    private static HistorialEvaluacionesJdbcAdapter historialAdapter;

    private static long idSolicitante;
    private static long idSolicitanteSinEvaluaciones;
    private static long idAnteriorAHu12;
    private static long idClasificada;
    private static long idMismaFecha;

    @BeforeAll
    static void prepararEsquemaConMigracionesDeScoring() throws Exception {
        String base = System.getProperty("it.base-dedicada");
        url = System.getProperty("spring.datasource.url");
        usuario = System.getenv("DB_USERNAME");
        clave = System.getenv("DB_PASSWORD");
        if (!BASE_AUTORIZADA.equals(base)) {
            throw new IllegalStateException("Debe indicarse -Dit.base-dedicada=" + BASE_AUTORIZADA);
        }
        if (!URL_AUTORIZADA.equals(url)) {
            throw new IllegalStateException("spring.datasource.url debe indicarse con -D y ser exactamente "
                    + URL_AUTORIZADA);
        }
        assertThat(usuario).as("DB_USERNAME").isNotBlank();
        assertThat(clave).as("DB_PASSWORD").isNotBlank();
        assertThat(MIGRACIONES_SCORING.resolve("V9__proteger_inmutabilidad_politica_clasificacion.sql"))
                .as("migraciones de scoring-api en la carpeta hermana").exists();

        crearEsquemaPropio(base);

        DriverManagerDataSource dataSource = new DriverManagerDataSource(url + "?currentSchema=" + ESQUEMA, usuario,
                clave);
        jdbcTemplate = new JdbcTemplate(dataSource);
        DataSourceTransactionManager gestor = new DataSourceTransactionManager(dataSource);
        transaccion = new TransactionTemplate(gestor);
        soloLectura = new TransactionTemplate(gestor);
        soloLectura.setReadOnly(true);
        NamedParameterJdbcTemplate consultas = new NamedParameterJdbcTemplate(dataSource);
        explicacionAdapter = new ExplicacionEvaluacionJdbcAdapter(consultas);
        historialAdapter = new HistorialEvaluacionesJdbcAdapter(consultas);
        DatosScoringPrueba datos = new DatosScoringPrueba(jdbcTemplate);

        aplicarMigraciones(1, 7);
        long[] ids = new long[4];
        transaccion.executeWithoutResult(estado -> {
            idSolicitante = datos.solicitante("Solicitante Contrato", "900000201");
            idSolicitanteSinEvaluaciones = datos.solicitante("Sin Evaluaciones", "900000202");
            ids[0] = datos.riesgo("NUMERO_MORAS");
            ids[1] = datos.riesgo("INGRESOS_MENSUALES");
            ids[2] = datos.regla(ids[0], ">", "2", -20);
            ids[3] = datos.regla(ids[1], ">=", "3000000", 20);
            idAnteriorAHu12 = datos.evaluacionAnteriorAHu12(idSolicitante, FECHA.minusDays(9));
            datos.detalle(idAnteriorAHu12, ids[3], ">=", "3000000", 20, true, 20);
        });

        aplicarMigraciones(8, 9);
        transaccion.executeWithoutResult(estado -> {
            long politica = datos.politica(1);
            jdbcTemplate.update("INSERT INTO rango_clasificacion (id_politica, categoria, limite_inferior, "
                    + "limite_superior) VALUES (?, 'BAJO', -100, -30), (?, 'MEDIO', -29, 29), (?, 'ALTO', 30, 130)",
                    politica, politica, politica);
            // puntaje -20 = suma de detalles (trigger A); -20 pertenece a MEDIO en esta política (trigger B)
            idClasificada = datos.evaluacionClasificada(idSolicitante, FECHA, -20, "MEDIO", politica);
            datos.detalle(idClasificada, ids[2], ">", "2", -20, true, -20);
            datos.detalle(idClasificada, ids[3], ">=", "3000000", 20, false, 0);
            idMismaFecha = datos.evaluacionClasificada(idSolicitante, FECHA, 0, "MEDIO", politica);
        });
        // La regla se edita después de evaluar; la explicación debe mostrar la versión aplicada
        jdbcTemplate.update("UPDATE regla_scoring SET puntaje = -30, valor_condicion = '4' WHERE id_regla = ?", ids[2]);
    }

    @AfterAll
    static void eliminarSoloElEsquemaCreadoPorLaPrueba() {
        if (marca == null) {
            return; // la prueba abortó antes de crear nada
        }
        String motivo;
        try (Connection connection = DriverManager.getConnection(url, usuario, clave)) {
            connection.setAutoCommit(false);
            try {
                motivo = motivoParaNoEliminarElEsquema(connection);
                if (motivo == null) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("DROP SCHEMA " + ESQUEMA + " CASCADE");
                    }
                    connection.commit();
                    return;
                }
                connection.rollback();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            motivo = "falló la comprobación o el DROP; transacción revertida (SQLState " + ex.getSQLState() + ")";
        }
        throw new AssertionError("No se eliminó el esquema " + ESQUEMA + ": " + motivo
                + ". Revíselo manualmente antes de volver a ejecutar la prueba.");
    }

    @Test
    void lasMigracionesDeScoringQuedanAplicadasEnElEsquemaTemporal() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema = ? "
                + "AND table_name = 'evaluacion' AND column_name IN ('score_total', 'categoria', 'id_politica')",
                Long.class, ESQUEMA)).isEqualTo(3L);
    }

    @Test
    void laExplicacionUsaLasInstantaneasPersistidasYCoincideConElPuntaje() {
        ExplicacionEvaluacion explicacion = enSoloLectura(() -> explicacionAdapter.buscarPorId(idClasificada)).orElseThrow();

        assertThat(explicacion.getScoreTotal()).isEqualTo(-20);
        assertThat(explicacion.getOrigenPuntaje()).isEqualTo(OrigenPuntaje.PERSISTIDO);
        assertThat(explicacion.getCategoria()).isEqualTo("MEDIO");
        assertThat(explicacion.getVersionPolitica()).isEqualTo(1);
        assertThat(explicacion.getFechaEvaluacion()).isEqualTo(FECHA);
        assertThat(explicacion.getFactores()).containsExactly(
                new FactorAplicado(explicacion.getFactores().get(0).idRegla(), "NUMERO_MORAS", ">", "2", -20, true, -20),
                new FactorAplicado(explicacion.getFactores().get(1).idRegla(), "INGRESOS_MENSUALES", ">=", "3000000",
                        20, false, 0));
    }

    @Test
    void unaEvaluacionAnteriorAHu12SeExplicaConPuntajeDerivado() {
        ExplicacionEvaluacion explicacion = enSoloLectura(() -> explicacionAdapter.buscarPorId(idAnteriorAHu12))
                .orElseThrow();

        assertThat(explicacion.getScoreTotal()).isEqualTo(20);
        assertThat(explicacion.getOrigenPuntaje()).isEqualTo(OrigenPuntaje.DERIVADO_DE_DETALLES);
        assertThat(explicacion.isClasificada()).isFalse();
    }

    @Test
    void unaEvaluacionInexistenteNoDevuelveExplicacion() {
        assertThat(enSoloLectura(() -> explicacionAdapter.buscarPorId(Long.MAX_VALUE))).isEmpty();
    }

    @Test
    void elHistorialSeOrdenaPorFechaYDesempataPorIdentificador() {
        List<EvaluacionHistorica> historial = enSoloLectura(() -> historialAdapter.listarPorSolicitante(idSolicitante));

        assertThat(historial).extracting(EvaluacionHistorica::getIdEvaluacion)
                .containsExactly(idMismaFecha, idClasificada, idAnteriorAHu12);
        assertThat(historial.get(1).getScoreTotal()).isEqualTo(-20);
        assertThat(historial.get(2).getScoreTotal()).isEqualTo(20);
        assertThat(historial.get(2).isClasificada()).isFalse();
    }

    @Test
    void laExistenciaDelSolicitanteSeDistingueDelHistorialVacio() {
        assertThat(enSoloLectura(() -> historialAdapter.existe(idSolicitanteSinEvaluaciones))).isTrue();
        assertThat(enSoloLectura(() -> historialAdapter.listarPorSolicitante(idSolicitanteSinEvaluaciones))).isEmpty();
        assertThat(enSoloLectura(() -> historialAdapter.existe(Long.MAX_VALUE))).isFalse();
    }

    // ---------------------------------------------------------------------------------------

    private static <T> T enSoloLectura(Supplier<T> consulta) {
        return soloLectura.execute(estado -> consulta.get());
    }

    private static void crearEsquemaPropio(String base) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, usuario, clave);
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT current_database(), EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = ?)")) {
            statement.setString(1, ESQUEMA);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertThat(resultSet.getString(1)).as("base de la conexión").isEqualTo(base);
                assertThat(resultSet.getBoolean(2))
                        .as("el esquema " + ESQUEMA + " ya existe; no se reutiliza ni se elimina").isFalse();
            }
            String nuevaMarca = "creado por EsquemaScoringContratoIT " + UUID.randomUUID();
            try (Statement creacion = connection.createStatement()) {
                creacion.execute("CREATE SCHEMA " + ESQUEMA);
                creacion.execute("COMMENT ON SCHEMA " + ESQUEMA + " IS '" + nuevaMarca + "'");
            }
            marca = nuevaMarca;
            try (PreparedStatement oid = connection.prepareStatement("SELECT oid FROM pg_namespace WHERE nspname = ?")) {
                oid.setString(1, ESQUEMA);
                try (ResultSet resultSet = oid.executeQuery()) {
                    resultSet.next();
                    oidEsquemaCreado = resultSet.getLong(1);
                }
            }
        }
    }

    /** Ejecuta en orden las migraciones Vdesde..Vhasta de scoring-api, sin modificarlas en disco. */
    private static void aplicarMigraciones(int desde, int hasta) throws IOException {
        List<Path> archivos;
        try (Stream<Path> listado = Files.list(MIGRACIONES_SCORING)) {
            archivos = listado.filter(archivo -> {
                int version = version(archivo);
                return version >= desde && version <= hasta;
            }).sorted((a, b) -> Integer.compare(version(a), version(b))).toList();
        }
        assertThat(archivos).as("migraciones V" + desde + "–V" + hasta).hasSize(hasta - desde + 1);
        for (Path archivo : archivos) {
            String sql = Files.readString(archivo, StandardCharsets.UTF_8).replace("${flyway:defaultSchema}", ESQUEMA);
            transaccion.executeWithoutResult(estado -> jdbcTemplate.execute(sql));
        }
    }

    private static int version(Path archivo) {
        String nombre = archivo.getFileName().toString();
        return Integer.parseInt(nombre.substring(1, nombre.indexOf("__")));
    }

    private static String motivoParaNoEliminarElEsquema(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT n.oid, obj_description(n.oid, 'pg_namespace') FROM pg_namespace n WHERE n.nspname = ?")) {
            statement.setString(1, ESQUEMA);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return "el esquema ya no existe";
                }
                if (oidEsquemaCreado == null || resultSet.getLong(1) != oidEsquemaCreado) {
                    return "el esquema existente no es el que creó esta ejecución (OID distinto)";
                }
                if (!marca.equals(resultSet.getString(2))) {
                    return "el esquema no conserva la marca de creación de esta ejecución";
                }
            }
        }
        return null;
    }
}
