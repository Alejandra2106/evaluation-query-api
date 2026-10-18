package com.crediticio.evaluationquery;

import com.crediticio.evaluationquery.support.ClavesRsaPrueba;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

/** Arranque completo (perfil test) con una clave pública generada en tiempo de ejecución. */
@SpringBootTest
@ActiveProfiles("test")
class EvaluationQueryApiApplicationTests {

    @TempDir
    static Path claves;

    @DynamicPropertySource
    static void configurarClavePublica(DynamicPropertyRegistry registro) {
        registro.add("evaluationquery.jwt.clave-publica",
                () -> "file:" + ClavesRsaPrueba.escribirClavePublica(ClavesRsaPrueba.generar(), claves));
    }

    @Test
    void elContextoDebeArrancar() {
    }
}
