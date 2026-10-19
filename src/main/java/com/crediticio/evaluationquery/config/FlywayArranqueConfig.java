package com.crediticio.evaluationquery.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Protección contra migraciones accidentales del esquema propio al arrancar (HU14). Por defecto Flyway solo
 * valida: con migraciones pendientes la aplicación no arranca y no se aplica nada. La migración se ejecuta solo
 * de forma controlada, con EVALUATION_QUERY_FLYWAY_MIGRAR_AL_ARRANCAR=true (docs/operacion/despliegue.md).
 */
@Configuration
public class FlywayArranqueConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayArranqueConfig.class);

    @Bean
    public FlywayMigrationStrategy estrategiaMigracion(
            @Value("${evaluationquery.flyway.migrar-al-arrancar:false}") boolean migrarAlArrancar) {
        return flyway -> ejecutar(flyway, migrarAlArrancar);
    }

    static void ejecutar(Flyway flyway, boolean migrarAlArrancar) {
        if (migrarAlArrancar) {
            log.warn("Flyway aplicará las migraciones pendientes (evaluationquery.flyway.migrar-al-arrancar=true)");
            flyway.migrate();
            return;
        }
        try {
            flyway.validate();
        } catch (RuntimeException ex) {
            log.error("El esquema propio no coincide con las migraciones de esta versión (puede haber migraciones "
                    + "pendientes). No se aplicó ninguna. Siga el procedimiento de despliegue controlado.");
            throw ex;
        }
    }
}
