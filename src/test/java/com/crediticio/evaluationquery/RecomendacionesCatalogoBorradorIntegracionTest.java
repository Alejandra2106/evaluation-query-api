package com.crediticio.evaluationquery;

import com.crediticio.evaluationquery.support.ClavesRsaPrueba;
import com.crediticio.evaluationquery.support.DatosScoringPrueba;
import com.crediticio.evaluationquery.support.TokensServicioPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU14 con el catálogo incluido, que es un BORRADOR no aprobado (P-HU14-01): la saga habilitada no genera
 * ninguna recomendación y recibe 503 CATALOGO_NO_DISPONIBLE, que trata como transitorio.
 */
@SpringBootTest(properties = {
        "evaluationquery.saga.habilitada=true",
        "evaluationquery.servicio-jwt.id-clave=" + RecomendacionesCatalogoBorradorIntegracionTest.KID})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecomendacionesCatalogoBorradorIntegracionTest {

    static final String KID = "scoring-prueba-1";
    private static final KeyPair CLAVES_USUARIOS = ClavesRsaPrueba.generar();
    private static final KeyPair CLAVES_SERVICIO = ClavesRsaPrueba.generar();

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

    private static Path subdirectorio(String nombre) {
        try {
            return Files.createDirectories(directorioClaves.resolve(nombre));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Test
    void conElCatalogoBorradorNoSeGeneraNingunaRecomendacion() throws Exception {
        DatosScoringPrueba datos = new DatosScoringPrueba(jdbcTemplate);
        datos.vaciar();
        long solicitante = datos.solicitante("Solicitante Borrador", "900000401");
        long idPendiente = datos.evaluacionConEstado(solicitante, LocalDateTime.of(2026, 10, 18, 9, 0), 0, "ALTO",
                datos.politica(1), "PENDIENTE");
        String token = TokensServicioPrueba.firmar(CLAVES_SERVICIO, KID, TokensServicioPrueba.claimsValidos().build());

        mockMvc.perform(put("/internal/v1/evaluaciones/{id}/recomendaciones", idPendiente)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("CATALOGO_NO_DISPONIBLE"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM consulta.generacion_recomendaciones",
                Integer.class)).isZero();
    }
}
