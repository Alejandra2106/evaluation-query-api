package com.crediticio.evaluationquery;

import com.crediticio.evaluationquery.config.JwtConfig;
import com.crediticio.evaluationquery.support.ClavesRsaPrueba;
import com.crediticio.evaluationquery.support.DatosScoringPrueba;
import com.crediticio.evaluationquery.support.TokensPrueba;
import com.crediticio.evaluationquery.support.TokensServicioPrueba;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU14 dentro de evaluation-query-api, con la saga habilitada: generación interna con el token de servicio S1,
 * consulta con tokens de usuario, y efecto del estado de la saga en HU13 (P-HU13-02) y HU15 (CA08, CA09). Usa
 * un catálogo APROBADO solo de prueba (no es el del negocio) y claves RSA efímeras. H2 no tiene los triggers de
 * inmutabilidad; se verifican sobre PostgreSQL (EsquemaScoringContratoIT).
 */
@SpringBootTest(properties = {
        "evaluationquery.saga.habilitada=true",
        "evaluationquery.catalogo.ubicacion=classpath:catalogo/recomendaciones-prueba-aprobado.json",
        "evaluationquery.servicio-jwt.id-clave=" + RecomendacionesIntegracionTest.KID})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecomendacionesIntegracionTest {

    static final String KID = "scoring-prueba-1";
    private static final String EMISOR = "auth-api-prueba";
    private static final KeyPair CLAVES_USUARIOS = ClavesRsaPrueba.generar();
    private static final KeyPair CLAVES_SERVICIO = ClavesRsaPrueba.generar();
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 18, 9, 0);
    private static final String RUTA_INTERNA = "/internal/v1/evaluaciones/{id}/recomendaciones";
    private static final String RUTA_CONSULTA = "/api/v1/evaluaciones/{id}/recomendaciones";

    @TempDir
    static Path directorioClaves;

    @DynamicPropertySource
    static void configurarClavesPublicas(DynamicPropertyRegistry registro) {
        registro.add("evaluationquery.jwt.clave-publica",
                () -> "file:" + ClavesRsaPrueba.escribirClavePublica(CLAVES_USUARIOS, subdirectorio("usuarios")));
        registro.add("evaluationquery.servicio-jwt.clave-publica",
                () -> "file:" + ClavesRsaPrueba.escribirClavePublica(CLAVES_SERVICIO, subdirectorio("servicio")));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long idSolicitante;
    private long idPendiente;
    private long idPendienteSinFactores;
    private long idCompletada;
    private long idAnteriorALaSaga;

    @BeforeEach
    void prepararDatos() {
        DatosScoringPrueba datos = new DatosScoringPrueba(jdbcTemplate);
        datos.vaciar();
        idSolicitante = datos.solicitante("Solicitante Saga", "900000301");
        long moras = datos.riesgo("NUMERO_MORAS");
        long ingresos = datos.riesgo("INGRESOS_MENSUALES");
        long reglaMoras = datos.regla(moras, ">", "2", -20);
        long reglaIngresos = datos.regla(ingresos, ">=", "3000000", 20);
        long politica = datos.politica(1);

        idAnteriorALaSaga = datos.evaluacionClasificada(idSolicitante, FECHA.minusDays(5), 0, "MEDIO", politica);

        idPendiente = datos.evaluacionConEstado(idSolicitante, FECHA, -20, "MEDIO", politica, "PENDIENTE");
        datos.detalle(idPendiente, reglaMoras, ">", "2", -20, true, -20);
        datos.detalle(idPendiente, reglaIngresos, ">=", "3000000", 20, false, 0);

        idPendienteSinFactores = datos.evaluacionConEstado(idSolicitante, FECHA.minusDays(1), 20, "ALTO", politica,
                "PENDIENTE");
        datos.detalle(idPendienteSinFactores, reglaIngresos, ">=", "3000000", 20, true, 20);

        idCompletada = datos.evaluacionConEstado(idSolicitante, FECHA.minusDays(2), 0, "MEDIO", politica,
                "COMPLETADA");
        jdbcTemplate.update("INSERT INTO consulta.generacion_recomendaciones (id_evaluacion, version_catalogo, "
                + "categoria_aplicada) VALUES (?, 'ANTERIOR-1', 'MEDIO')", idCompletada);
        jdbcTemplate.update("INSERT INTO consulta.recomendacion_evaluacion (id_evaluacion, orden, codigo, texto, "
                + "tipo_activacion, categoria) VALUES (?, 1, 'REC-HIST-01', 'Texto de la versión anterior', "
                + "'CATEGORIA', 'MEDIO')", idCompletada);
    }

    private static Path subdirectorio(String nombre) {
        try {
            return Files.createDirectories(directorioClaves.resolve(nombre));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String usuario(String rol) {
        return "Bearer " + TokensPrueba.firmar(CLAVES_USUARIOS, TokensPrueba.claimsValidos(EMISOR, rol).build());
    }

    private static String servicio() {
        return "Bearer " + TokensServicioPrueba.firmar(CLAVES_SERVICIO, KID, TokensServicioPrueba.claimsValidos().build());
    }

    private int recomendacionesGuardadas(long idEvaluacion) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consulta.recomendacion_evaluacion "
                + "WHERE id_evaluacion = ?", Integer.class, idEvaluacion);
    }

    // ---- Generación interna ----

    @Test
    void laSagaGeneraLasRecomendacionesConLaClasificacionYLosFactoresPersistidos() throws Exception {
        mockMvc.perform(put(RUTA_INTERNA, idPendiente).header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEvaluacion").value(idPendiente))
                .andExpect(jsonPath("$.versionCatalogo").value("PRUEBA-1"))
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.creada").doesNotExist());

        assertThat(jdbcTemplate.queryForList("SELECT codigo FROM consulta.recomendacion_evaluacion "
                + "WHERE id_evaluacion = ? ORDER BY orden", String.class, idPendiente))
                .containsExactly("PRUEBA-CAT-MEDIO", "PRUEBA-MORAS");
        assertThat(jdbcTemplate.queryForObject("SELECT score_total FROM evaluacion WHERE id_evaluacion = ?",
                Integer.class, idPendiente)).isEqualTo(-20);
    }

    @Test
    void unReintentoDevuelveLaGeneracionGuardadaSinDuplicarla() throws Exception {
        mockMvc.perform(put(RUTA_INTERNA, idPendiente).header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isCreated());

        mockMvc.perform(put(RUTA_INTERNA, idPendiente).header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionCatalogo").value("PRUEBA-1"))
                .andExpect(jsonPath("$.cantidad").value(2));

        assertThat(recomendacionesGuardadas(idPendiente)).isEqualTo(2);
    }

    @Test
    void elReintentoDeUnaEvaluacionYaCompletadaDevuelveLaVersionConQueSeGenero() throws Exception {
        mockMvc.perform(put(RUTA_INTERNA, idCompletada).header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionCatalogo").value("ANTERIOR-1"))
                .andExpect(jsonPath("$.cantidad").value(1));
    }

    @Test
    void sinRecomendacionesAplicablesQuedaUnaGeneracionVacia() throws Exception {
        mockMvc.perform(put(RUTA_INTERNA, idPendienteSinFactores).header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(0));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consulta.generacion_recomendaciones "
                + "WHERE id_evaluacion = ?", Integer.class, idPendienteSinFactores)).isEqualTo(1);
    }

    @Test
    void laGeneracionRechazaEvaluacionesInexistentesOAnterioresALaSaga() throws Exception {
        mockMvc.perform(put(RUTA_INTERNA, 999999).header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EVALUACION_NO_ENCONTRADA"));
        mockMvc.perform(put(RUTA_INTERNA, idAnteriorALaSaga).header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EVALUACION_NO_PENDIENTE"));

        assertThat(recomendacionesGuardadas(idAnteriorALaSaga)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {JwtConfig.ROL_ADMINISTRADOR, JwtConfig.ROL_ANALISTA})
    void ningunTokenDeUsuarioAutorizaLaGeneracion(String rol) throws Exception {
        mockMvc.perform(put(RUTA_INTERNA, idPendiente).header(HttpHeaders.AUTHORIZATION, usuario(rol)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"));
        mockMvc.perform(put(RUTA_INTERNA, idPendiente))
                .andExpect(status().isUnauthorized());

        assertThat(recomendacionesGuardadas(idPendiente)).isZero();
    }

    @Test
    void unTokenDeServicioSinElPermisoOConOtroKidSeRechaza() throws Exception {
        String sinPermiso = TokensServicioPrueba.firmar(CLAVES_SERVICIO, KID,
                TokensServicioPrueba.claimsValidos().claim("scope", "otro").build());
        String otroKid = TokensServicioPrueba.firmar(CLAVES_SERVICIO, "kid-desconocido",
                TokensServicioPrueba.claimsValidos().build());

        mockMvc.perform(put(RUTA_INTERNA, idPendiente).header(HttpHeaders.AUTHORIZATION, "Bearer " + sinPermiso))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(RUTA_INTERNA, idPendiente).header(HttpHeaders.AUTHORIZATION, "Bearer " + otroKid))
                .andExpect(status().isUnauthorized());
        assertThat(recomendacionesGuardadas(idPendiente)).isZero();
    }

    @Test
    void elTokenDeServicioSoloSirveParaLaOperacionInterna() throws Exception {
        mockMvc.perform(get(RUTA_CONSULTA, idCompletada).header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/internal/v1/evaluaciones/{id}/recomendaciones", idCompletada)
                        .header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isForbidden());
    }

    // ---- Consulta ----

    @ParameterizedTest
    @ValueSource(strings = {JwtConfig.ROL_ADMINISTRADOR, JwtConfig.ROL_ANALISTA})
    void ambosRolesConsultanElTextoHistoricoSinDecisionDeCredito(String rol) throws Exception {
        MvcResult resultado = mockMvc.perform(get(RUTA_CONSULTA, idCompletada)
                        .header(HttpHeaders.AUTHORIZATION, usuario(rol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEvaluacion").value(idCompletada))
                .andExpect(jsonPath("$.generadas").value(true))
                .andExpect(jsonPath("$.versionCatalogo").value("ANTERIOR-1"))
                .andExpect(jsonPath("$.fechaGeneracion").exists())
                .andExpect(jsonPath("$.recomendaciones.length()").value(1))
                .andExpect(jsonPath("$.recomendaciones[0].orden").value(1))
                .andExpect(jsonPath("$.recomendaciones[0].codigo").value("REC-HIST-01"))
                .andExpect(jsonPath("$.recomendaciones[0].texto").value("Texto de la versión anterior"))
                .andExpect(jsonPath("$.recomendaciones[0].tipoActivacion").value("CATEGORIA"))
                .andExpect(jsonPath("$.recomendaciones[0].categoria").value("MEDIO"))
                .andExpect(jsonPath("$.recomendaciones[0].variable").value(nullValue()))
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString()).doesNotContainIgnoringCase("aprob")
                .doesNotContainIgnoringCase("rechaz");
    }

    @Test
    void consultarNoGeneraRecomendacionesYUnaPendienteResponde409() throws Exception {
        mockMvc.perform(get(RUTA_CONSULTA, idPendiente).header(HttpHeaders.AUTHORIZATION, usuario(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EVALUACION_PENDIENTE"));

        assertThat(recomendacionesGuardadas(idPendiente)).isZero();
    }

    @Test
    void unaEvaluacionAnteriorALaSagaRespondeUnaListaVaciaNoGenerada() throws Exception {
        mockMvc.perform(get(RUTA_CONSULTA, idAnteriorALaSaga)
                        .header(HttpHeaders.AUTHORIZATION, usuario(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generadas").value(false))
                .andExpect(jsonPath("$.versionCatalogo").value(nullValue()))
                .andExpect(jsonPath("$.recomendaciones.length()").value(0));
    }

    @Test
    void laConsultaValidaLaEvaluacionYElAcceso() throws Exception {
        mockMvc.perform(get(RUTA_CONSULTA, 999999).header(HttpHeaders.AUTHORIZATION, usuario(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EVALUACION_NO_ENCONTRADA"));
        mockMvc.perform(get(RUTA_CONSULTA, 0).header(HttpHeaders.AUTHORIZATION, usuario(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(get(RUTA_CONSULTA, idCompletada))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NO_AUTENTICADO"));
        mockMvc.perform(get(RUTA_CONSULTA, idCompletada).header(HttpHeaders.AUTHORIZATION, usuario("AUDITOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESO_DENEGADO"));
    }

    @Test
    void trasLaGeneracionYLaConfirmacionDeScoringLaConsultaDevuelveLoGenerado() throws Exception {
        mockMvc.perform(put(RUTA_INTERNA, idPendiente).header(HttpHeaders.AUTHORIZATION, servicio()))
                .andExpect(status().isCreated());
        // scoring-api confirma la saga (en PostgreSQL lo protegen los triggers de V11)
        jdbcTemplate.update("UPDATE evaluacion SET estado = 'COMPLETADA' WHERE id_evaluacion = ?", idPendiente);

        mockMvc.perform(get(RUTA_CONSULTA, idPendiente).header(HttpHeaders.AUTHORIZATION, usuario(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionCatalogo").value("PRUEBA-1"))
                .andExpect(jsonPath("$.recomendaciones[1].tipoActivacion").value("FACTOR_DESFAVORABLE"))
                .andExpect(jsonPath("$.recomendaciones[1].variable").value("NUMERO_MORAS"));
    }

    // ---- Estado de la saga en HU13 y HU15 ----

    @Test
    void laExplicacionDeUnaEvaluacionPendienteResponde409() throws Exception {
        mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", idPendiente)
                        .header(HttpHeaders.AUTHORIZATION, usuario(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EVALUACION_PENDIENTE"));
        mockMvc.perform(get("/api/v1/evaluaciones/{id}/explicacion", idCompletada)
                        .header(HttpHeaders.AUTHORIZATION, usuario(JwtConfig.ROL_ANALISTA)))
                .andExpect(status().isOk());
    }

    @Test
    void elHistorialExcluyeLasPendientesEIncluyeEstadoYEnlaces() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/{id}/evaluaciones", idSolicitante)
                        .header(HttpHeaders.AUTHORIZATION, usuario(JwtConfig.ROL_ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].idEvaluacion").value(idCompletada))
                .andExpect(jsonPath("$[0].estado").value("COMPLETADA"))
                .andExpect(jsonPath("$[0].recomendaciones").value("/api/v1/evaluaciones/" + idCompletada
                        + "/recomendaciones"))
                .andExpect(jsonPath("$[1].idEvaluacion").value(idAnteriorALaSaga))
                .andExpect(jsonPath("$[1].estado").value(nullValue()));
    }
}
