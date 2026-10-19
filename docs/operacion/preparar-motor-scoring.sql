-- Preparación de motor_scoring para evaluation-query-api (HU13, HU14, HU15). Lo ejecuta el administrador de
-- la base, una sola vez, ANTES del primer arranque de evaluation-query-api. NO lo ejecuta la aplicación y no
-- se ha ejecutado sobre motor_scoring: requiere autorización expresa.
--
-- Qué hace:
--   1. Crea el rol de la aplicación (LOGIN, sin contraseña en este archivo).
--   2. Le da SELECT únicamente sobre las seis tablas de scoring-api que lee (esquema public).
--   3. Crea el esquema propio (por defecto "consulta"), cuyo propietario es el rol que ejecutará Flyway de
--      evaluation-query-api; ese rol crea las tablas de recomendaciones con la migración V1 de esta API.
--   4. Concede al rol de la aplicación SELECT e INSERT sobre las tablas que cree ese migrador en el esquema
--      propio (privilegios por defecto). Nunca UPDATE ni DELETE: las recomendaciones son inmutables.
--
-- Requisitos: las migraciones V1–V9 de scoring-api ya aplicadas (las tablas deben existir). V10 y V11 las
-- aplica scoring-api al arrancar con su versión con HU14; añaden columnas, que el SELECT por tabla ya cubre.
--
-- Uso (psql, conectado a motor_scoring con un rol administrador):
--   psql -h <host> -U <administrador> -d motor_scoring -v rol_app=<rol de la aplicación> \
--        -v rol_migrador=<rol de Flyway de evaluation-query-api> -v esquema=consulta \
--        -f preparar-motor-scoring.sql
--   y después, de forma interactiva (la contraseña nunca va en archivos ni en argumentos):
--   \password <rol de la aplicación>
--
-- La aplicación se configura entonces con EVALUATION_QUERY_DB_USERNAME=<rol de la aplicación>,
-- EVALUATION_QUERY_DB_SCHEMA=<esquema>, y SPRING_FLYWAY_USER / SPRING_FLYWAY_PASSWORD del migrador.
-- El rol migrador debe existir previamente (puede ser el mismo que usa Flyway de scoring-api).

\set ON_ERROR_STOP on

\if :{?rol_app}
\else
    \echo 'Falta -v rol_app=<rol de la aplicación>'
    \quit
\endif
\if :{?rol_migrador}
\else
    \echo 'Falta -v rol_migrador=<rol de Flyway de evaluation-query-api>'
    \quit
\endif
\if :{?esquema}
\else
    \set esquema consulta
\endif

SELECT current_database() = 'motor_scoring' AS es_motor_scoring \gset
\if :es_motor_scoring
\else
    \echo 'Este script solo se ejecuta conectado a motor_scoring'
    \quit
\endif

BEGIN;

-- 1. Rol de la aplicación: sin contraseña aquí; se asigna con \password
CREATE ROLE :"rol_app" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION;
COMMENT ON ROLE :"rol_app" IS 'evaluation-query-api: lectura de scoring-api y recomendaciones de HU14';
GRANT CONNECT ON DATABASE motor_scoring TO :"rol_app";

-- 2. Solo lectura sobre las tablas de scoring-api que consulta (HU13, HU14, HU15)
GRANT USAGE ON SCHEMA public TO :"rol_app";
GRANT SELECT ON public.solicitante, public.riesgo, public.regla_scoring, public.politica_clasificacion,
    public.evaluacion, public.detalle_evaluacion TO :"rol_app";

-- 3. Esquema propio de evaluation-query-api; Flyway de esta API no lo crea (create-schemas=false)
CREATE SCHEMA :"esquema" AUTHORIZATION :"rol_migrador";
COMMENT ON SCHEMA :"esquema" IS 'evaluation-query-api: recomendaciones generadas (HU14)';
GRANT USAGE ON SCHEMA :"esquema" TO :"rol_app";

-- 4. Inserción y lectura de recomendaciones en las tablas que cree el migrador
ALTER DEFAULT PRIVILEGES FOR ROLE :"rol_migrador" IN SCHEMA :"esquema"
    GRANT SELECT, INSERT ON TABLES TO :"rol_app";
ALTER DEFAULT PRIVILEGES FOR ROLE :"rol_migrador" IN SCHEMA :"esquema"
    GRANT USAGE ON SEQUENCES TO :"rol_app";

COMMIT;

-- Comprobación (sin modificar nada): el rol no debe tener INSERT, UPDATE ni DELETE en public
SELECT table_name, string_agg(privilege_type, ', ' ORDER BY privilege_type) AS privilegios
FROM information_schema.role_table_grants
WHERE grantee = :'rol_app' AND table_schema = 'public'
GROUP BY table_name
ORDER BY table_name;
