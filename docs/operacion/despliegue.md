# Despliegue de evaluation-query-api (HU13–HU15)

Estado: **ejecutado el 2026-10-19 sobre `motor_scoring`** (roles `consulta_app` y `consulta_propietario`,
esquema `consulta` en V1). El registro completo, con el respaldo previo, el ensayo y los pasos pendientes
(contraseñas y saga), está en `scoring-api/docs/operacion/despliegue-v10-v11.md`.

1. Requisito: `scoring-api` ya migrada a V11 (necesita la columna `evaluacion.estado`).
2. `01-preparar-antes-de-migrar.sql`: rol de la aplicación (solo SELECT sobre seis tablas de `public`) y
   esquema `consulta`, propiedad del migrador. Contraseña con `\password`.
3. Migración controlada de la V1 propia: un arranque con `EVALUATION_QUERY_FLYWAY_MIGRAR_AL_ARRANCAR=true`,
   `SPRING_FLYWAY_USER` y `SPRING_FLYWAY_PASSWORD` del migrador. Sin esa variable, un arranque con migraciones
   pendientes falla y no aplica nada (`config/FlywayArranqueConfig`).
4. `02-permisos-despues-de-migrar.sql`: SELECT e INSERT sobre las tablas de recomendaciones y SELECT sobre el
   historial de Flyway.
5. Arranque normal con el rol de la aplicación y sin variables de migración. La saga
   (`EVALUATION_QUERY_SAGA_HABILITADA=true`) solo con un catálogo APROBADO por el negocio
   (`EVALUATION_QUERY_CATALOGO_LOCATION`).
