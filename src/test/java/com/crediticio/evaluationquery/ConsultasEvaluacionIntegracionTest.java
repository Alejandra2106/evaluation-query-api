package com.crediticio.evaluationquery;

import com.crediticio.evaluationquery.config.JwtConfig;
import com.crediticio.evaluationquery.support.ClavesRsaPrueba;
import com.crediticio.evaluationquery.support.DatosScoringPrueba;
import com.crediticio.evaluationquery.support.TokensPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU13 y HU15 de extremo a extremo dentro de evaluation-query-api: cadena de seguridad real con tokens RS256
 * efímeros, controladores, servicios y adaptadores JDBC sobre H2 (esquema-scoring-h2.sql). La compatibilidad de
 * las consultas con el esquema real de PostgreSQL la comprueba EsquemaScoringContratoIT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsultasEvaluacionIntegracionTest {

    private static final String EMISOR = "auth-api-prueba";
    private static final KeyPair CLAVES = ClavesRsaPrueba.generar();
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 10, 10, 0);
    private static final String NOMBRE = "Solicitante Integracion";
    private static final String DOCUMENTO = "900000101";

    @TempDir
    static Path directorioClaves;

    @DynamicPropertySource
    static void configurarClavePublica(DynamicPropertyRegistry registro) {
        registro.add("evaluationquery.jwt.clave-publica",
                () -> "file:" + ClavesRsaPrueba.escribirClavePublica(CLAVES, directorioClaves));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long idSolicitante;
    private long idSolicitanteSinEvaluaciones;
    private long idClasificada;
    private long idAnteriorAHu12;
    private long idMismaFecha;
    private long idInconsistente;

    @BeforeEach
    void prepararDatos() {
        DatosScoringPrueba datos = new DatosScoringPrueba(jdbcTemplate);
        datos.vaciar();
        idSolicitante = datos.solicitante(NOMBRE, DOCUMENTO);
        idSolicitanteSinEvaluaciones = datos.solicitante("Sin Evaluaciones", "900000102");
        long otroSolicitante = datos.solicitante("Otro", "900000103");
        long moras = datos.riesgo("NUMERO_MORAS");
        long ingresos = datos.riesgo("INGRESOS_MENSUALES");
        long reglaMoras = datos.regla(moras, ">", "2", -20);
        long reglaIngresos = datos.regla(ingresos, ">=", "3000000", 20);
        long politica = datos.politica(1);

        idAnteriorAHu12 = datos.evaluacionAnteriorAHu12(idSolicitante, FECHA.minusDays(9));
        datos.detalle(idAnteriorAHu12, reglaIngresos, ">=", "3000000", 20, true, 20);

        idClasificada = datos.evaluacionClasificada(idSolicitante, FECHA, -20, "MEDIO", politica);
        datos.detalle(idClasificada, reglaMoras, ">", "2", -20, true, -20);
        datos.detalle(idClasificada, reglaIngresos, ">=", "3000000", 20, false, 0);

        idMismaFecha = datos.evaluacionClasificada(idSolicitante, FECHA, 0, "BAJO", politica);

        idInconsistente = datos.evaluacionClasificada(otroSolicitante, FECHA, 15, "BAJO", politica);
        datos.detalle(idInconsistente, reglaIngresos, ">=", "3000000", 20, true, 10);

        // La regla se edita después de evaluar: la explicación debe mostrar la versión aplicada (-20)
        jdbcTemplate.update("UPDATE regla_scoring SET puntaje = -30, valor_condicion = '4' WHERE id_regla = ?",
                reglaMoras);
    }

    private static String bearer(String rol) {
        return "Bearer " + TokensPrueba.firmar(CLAVES, TokensPrueba.claimsValidos(EMISOR, rol).build());
    }

    private static void sinDatosPersonales(MvcResult resultado) throws Exception {
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain(NOMBRE, DOCUMENTO, "idSolicitante", "ingresos", "deudas");
    }

    // ---- HU13 ----

    @Test
    void laExplicacionDebeMostrarLosDatosGeneralesYLasReglasHistoricas() throws Exception {
        String traceId = UUID.randomUUID().toString();
        MvcResult resultado = mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", idClasificada)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA))
                        .header("X-Trace-Id", traceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.idEvaluacion").value(idClasificada))
                .andExpect(jsonPath("$.fechaEvaluacion").value("2026-10-10T10:00:00"))
                .andExpect(jsonPath("$.scoreTotal").value(-20))
                .andExpect(jsonPath("$.origenPuntaje").value("PERSISTIDO"))
                .andExpect(jsonPath("$.clasificada").value(true))
                .andExpect(jsonPath("$.categoria").value("MEDIO"))
                .andExpect(jsonPath("$.versionPolitica").value(1))
                .andExpect(jsonPath("$.factores.length()").value(2))
                .andExpect(jsonPath("$.factores[0].variable").value("NUMERO_MORAS"))
                .andExpect(jsonPath("$.factores[0].operadorAplicado").value(">"))
                .andExpect(jsonPath("$.factores[0].valorCondicionAplicado").value("2"))
                .andExpect(jsonPath("$.factores[0].puntajeReglaAplicado").value(-20))
                .andExpect(jsonPath("$.factores[0].condicionCumplida").value(true))
                .andExpect(jsonPath("$.factores[0].contribucion").value(-20))
                .andExpect(jsonPath("$.factores[1].condicionCumplida").value(false))
                .andExpect(jsonPath("$.factores[1].contribucion").value(0))
                .andReturn();

        sinDatosPersonales(resultado);
    }

    @Test
    void elAdministradorTambienPuedeConsultarLaExplicacion() throws Exception {
        mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", idClasificada)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ADMINISTRADOR)))
                .andExpect(status().isOk());
    }

    @Test
    void unaEvaluacionAnteriorAHu12DebeMostrarElPuntajeDerivadoSinClasificacion() throws Exception {
        mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", idAnteriorAHu12)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreTotal").value(20))
                .andExpect(jsonPath("$.origenPuntaje").value("DERIVADO_DE_DETALLES"))
                .andExpect(jsonPath("$.clasificada").value(false))
                .andExpect(jsonPath("$.categoria").value(nullValue()))
                .andExpect(jsonPath("$.versionPolitica").value(nullValue()));
    }

    @Test
    void unaEvaluacionInexistenteDebeResponder404ConTraceId() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EVALUACION_NO_ENCONTRADA"))
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString())
                .contains("\"traceId\":\"" + resultado.getResponse().getHeader("X-Trace-Id") + "\"");
    }

    @Test
    void unaSumaInconsistenteDebeResponder500SinCorregirse() throws Exception {
        mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", idInconsistente)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-3"})
    void unIdentificadorDeEvaluacionInvalidoDebeResponder400(String identificador) throws Exception {
        mockMvc.perform(get("/api/v1/evaluaciones/" + identificador + "/explicacion")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void laExplicacionExigeUnTokenYUnRolDelModelo() throws Exception {
        mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", idClasificada))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"));
        mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", idClasificada)
                        .header(HttpHeaders.AUTHORIZATION, bearer("AUDITOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
    }

    // ---- HU15 ----

    @Test
    void elHistorialDebeOrdenarseDeLaMasRecienteALaMasAntiguaConDesempatePorId() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/solicitantes/{id}/evaluaciones", idSolicitante)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].idEvaluacion").value(idMismaFecha))
                .andExpect(jsonPath("$[1].idEvaluacion").value(idClasificada))
                .andExpect(jsonPath("$[2].idEvaluacion").value(idAnteriorAHu12))
                .andExpect(jsonPath("$[1].fechaEvaluacion").value("2026-10-10T10:00:00"))
                .andExpect(jsonPath("$[1].scoreTotal").value(-20))
                .andExpect(jsonPath("$[1].origenPuntaje").value("PERSISTIDO"))
                .andExpect(jsonPath("$[1].categoria").value("MEDIO"))
                .andExpect(jsonPath("$[1].versionPolitica").value(1))
                .andExpect(jsonPath("$[1].explicacion").value("/api/v1/evaluaciones/" + idClasificada + "/explicacion"))
                .andExpect(jsonPath("$[2].scoreTotal").value(20))
                .andExpect(jsonPath("$[2].origenPuntaje").value("DERIVADO_DE_DETALLES"))
                .andExpect(jsonPath("$[2].clasificada").value(false))
                .andExpect(jsonPath("$[2].categoria").value(nullValue()))
                .andReturn();

        sinDatosPersonales(resultado);
    }

    @Test
    void unSolicitanteSinEvaluacionesDebeDevolverUnaListaVacia() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/{id}/evaluaciones", idSolicitanteSinEvaluaciones)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unSolicitanteInexistenteDebeResponder404() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/{id}/evaluaciones", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SOLICITANTE_NO_ENCONTRADO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x1", "0", "-5"})
    void unIdentificadorDeSolicitanteInvalidoDebeResponder400(String identificador) throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/" + identificador + "/evaluaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void elHistorialExigeUnTokenYUnRolDelModelo() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/{id}/evaluaciones", idSolicitante))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/solicitantes/{id}/evaluaciones", idSolicitante)
                        .header(HttpHeaders.AUTHORIZATION, bearer("AUDITOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasConsultasNoDebenModificarDatos() throws Exception {
        String antes = huella();

        mockMvc.perform(get("/api/v1/solicitantes/{id}/evaluaciones", idSolicitante)
                .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA))).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", idClasificada)
                .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA))).andExpect(status().isOk());

        assertThat(huella()).isEqualTo(antes);
    }

    @Test
    void otrasRutasYMetodosSiguenDenegados() throws Exception {
        mockMvc.perform(post("/api/v1/evaluaciones/{id}/explicacion", idClasificada)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/evaluaciones/{id}", idClasificada)
                        .header(HttpHeaders.AUTHORIZATION, bearer(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isForbidden());
    }

    private String huella() {
        return jdbcTemplate.queryForObject("SELECT CONCAT((SELECT COUNT(*) FROM evaluacion), '|', "
                + "(SELECT COUNT(*) FROM detalle_evaluacion), '|', "
                + "(SELECT COALESCE(SUM(score_total), 0) FROM evaluacion))", String.class);
    }
}
