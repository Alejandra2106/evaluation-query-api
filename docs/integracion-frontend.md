# Integración del frontend con las tres API

Guía elaborada el 2026-10-18 a partir del código de `auth-api`, `scoring-api` y `evaluation-query-api`.
Todo lo que aparece aquí está tomado de la configuración y los controladores reales, salvo lo marcado como
**no verificado**. No se ha ejecutado todavía un flujo de extremo a extremo con las tres API levantadas a la
vez ni con tokens reales de `auth-api` (ver "Limitaciones").

## 1. Servicios locales

| API | Puerto por defecto | Variable del puerto | Base de datos | Swagger |
|---|---|---|---|---|
| `auth-api` | 8081 | `AUTH_API_PORT` | Esquema `auth` (Flyway propio; el esquema debe existir) | Solo con el perfil `dev` |
| `scoring-api` | 8080 (Spring Boot por defecto) | — | Esquema `public` (Flyway V1–V9) | Solo con el perfil `dev` (`/swagger-ui.html`) |
| `evaluation-query-api` | 8082 | `EVALUATION_QUERY_API_PORT` | Lee `public` (solo SELECT, sin Flyway) | Solo con el perfil `dev` (`/swagger-ui.html`) |

Las tres comparten la misma instancia y base PostgreSQL (`motor_scoring` en el entorno local actual).

## 2. Autenticación

1. `POST http://localhost:8081/api/v1/auth/login` con `{"correo": "...", "contrasena": "..."}` →
   200 `{"accessToken": "...", "tokenType": "Bearer", "expiresIn": 900}` y la cookie de renovación
   `refresh_token` (HttpOnly, ruta `/api/v1/auth`). 401 `CREDENCIALES_INVALIDAS`.
2. Cada llamada a `scoring-api` y `evaluation-query-api` envía `Authorization: Bearer <accessToken>`.
3. El token dura 15 minutos. Para renovarlo: `POST /api/v1/auth/refresh` con la cookie y un `Origin`
   permitido (la petición del navegador debe usar `credentials: "include"`). Cierre: `POST /api/v1/auth/logout`.
4. El rol viaja en el claim `role` (`ADMINISTRADOR_RIESGO` o `ANALISTA_CREDITO`). El frontend puede leerlo
   del token para mostrar u ocultar opciones, pero la autorización real la aplica cada API.

Usuarios de prueba: el primer administrador se crea con el procedimiento offline del perfil
`bootstrap-admin` de `auth-api` (README de `auth-api`, "Primer administrador"); después, ese administrador
registra usuarios con `POST /api/v1/usuarios` `{"correo", "contrasena", "rol"}` (contraseña de 12 caracteres
como mínimo). No existen credenciales por defecto.

## 3. Contrato común

- Errores: `{"errorCode": "...", "message": "...", "details": [...], "traceId": "..."}`.
- 401 `NO_AUTENTICADO` (sin token, inválido o expirado); 403 `ACCESO_DENEGADO` (rol sin permiso).
- `X-Trace-Id`: el frontend puede enviar un UUID; si no lo envía o no es válido, la API genera uno. Todas las
  respuestas lo devuelven y la cabecera está expuesta por CORS.
- Fechas en formato ISO-8601 local, por ejemplo `2026-10-10T10:00:00`.

## 4. CORS

Origen permitido por defecto en las tres API: `http://localhost:3000` (`CORS_ALLOWED_ORIGINS`, separados por coma).

| API | Métodos | Credenciales (cookies) |
|---|---|---|
| `auth-api` | GET, POST, OPTIONS | Sí (necesario para la cookie de renovación) |
| `scoring-api` | GET, POST, PATCH, OPTIONS | No |
| `evaluation-query-api` | GET, OPTIONS | No |

Cabeceras admitidas: `Content-Type`, `Accept`, `Authorization`, `X-Trace-Id`.

## 5. Endpoints disponibles

### auth-api (8081)

| Método y ruta | Acceso |
|---|---|
| `POST /api/v1/auth/login` | Público |
| `POST /api/v1/auth/refresh` | Cookie de renovación + `Origin` permitido |
| `POST /api/v1/auth/logout` | Cookie de renovación + `Origin` permitido |
| `POST /api/v1/usuarios` | `ADMINISTRADOR_RIESGO` |

### scoring-api (8080)

| Método y ruta | Cuerpo | Roles (implementados) |
|---|---|---|
| `POST /api/v1/solicitantes` | `nombreCompleto`, `numeroDocumento`, `ingresosMensuales`, `deudasMensuales`, `numeroMoras`, `historialCrediticio` (`BUENO`/`REGULAR`/`MALO`), `antiguedadLaboral` | ANALISTA |
| `GET /api/v1/solicitantes/documento/{numeroDocumento}` | — | Ambos |
| `POST /api/v1/variables-riesgo` | `variable`, `descripcion` | ADMINISTRADOR |
| `PATCH /api/v1/variables-riesgo/{idRiesgo}/estado` | `estado` (`ACTIVA`/`INACTIVA`) | ADMINISTRADOR |
| `POST /api/v1/reglas-scoring` | `idRiesgo`, `operador` (`=`, `>`, `>=`, `<`, `<=`), `valorCondicion`, `puntaje` (-100 a 100) | ADMINISTRADOR |
| `PATCH /api/v1/reglas-scoring/{idRegla}` | `operador`, `valorCondicion`, `puntaje` | ADMINISTRADOR |
| `GET /api/v1/politicas-clasificacion/vigente` | — | Ambos |
| `POST /api/v1/politicas-clasificacion` | `rangos`: lista de `{categoria (BAJO/MEDIO/ALTO), limiteInferior, limiteSuperior}` | ADMINISTRADOR |
| `POST /api/v1/evaluaciones` | `idSolicitante` o `numeroDocumento` | ANALISTA; 201 con `scoreTotal`, `categoria`, `idPolitica`, `versionPolitica` y `detalles` |
| `GET /api/v1/evaluaciones/{idEvaluacion}` | — | **Solo ANALISTA** (ver "Bloqueos") |

Sin política vigente, `POST /api/v1/evaluaciones` responde 409 `POLITICA_VIGENTE_NO_DISPONIBLE`.

### evaluation-query-api (8082)

| Método y ruta | Roles | Respuesta |
|---|---|---|
| `GET /api/v1/evaluaciones/{idEvaluacion}/explicacion` | Ambos | Explicación (HU13); 404 `EVALUACION_NO_ENCONTRADA` |
| `GET /api/v1/solicitantes/{idSolicitante}/evaluaciones` | Ambos | Historial (HU15); 404 `SOLICITANTE_NO_ENCONTRADO`; `[]` si no tiene evaluaciones |

Formatos de respuesta en el README de `evaluation-query-api`. Cada elemento del historial incluye en
`explicacion` la ruta relativa de su explicación (se resuelve contra la URL base de `evaluation-query-api`).

## 6. Flujo recomendado para probar desde el frontend

1. Iniciar sesión como administrador (`auth-api`) y, si no existe, publicar una política de clasificación
   (`scoring-api`); crear variables de riesgo y reglas de scoring.
2. Iniciar sesión como analista; registrar o buscar un solicitante (`scoring-api`).
3. Ejecutar la evaluación (`POST /api/v1/evaluaciones`): devuelve puntaje, categoría y versión de la política.
4. Consultar la explicación (`evaluation-query-api`, `.../explicacion`) y el historial del solicitante
   (`evaluation-query-api`, `.../solicitantes/{id}/evaluaciones`).
5. Recomendaciones: **no disponibles** (HU14 no implementada).

## 7. Arranque local

Variables comunes: `AUTH_JWT_ISSUER` (igual en las tres API) y `CORS_ALLOWED_ORIGINS`.

| API | Variables propias |
|---|---|
| `auth-api` | `AUTH_DB_URL`, `AUTH_DB_USERNAME`, `AUTH_DB_PASSWORD`, `AUTH_JWT_KEY_ID`, `AUTH_JWT_PRIVATE_KEY_LOCATION`, `AUTH_JWT_PUBLIC_KEY_LOCATION` (opcionales: `AUTH_JWT_AUDIENCES`, `AUTH_ALLOWED_ORIGINS`, `AUTH_COOKIE_SECURE`) |
| `scoring-api` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SCORING_JWT_PUBLIC_KEY_LOCATION` |
| `evaluation-query-api` | `EVALUATION_QUERY_DB_URL`, `EVALUATION_QUERY_DB_USERNAME`, `EVALUATION_QUERY_DB_PASSWORD`, `EVALUATION_QUERY_JWT_PUBLIC_KEY_LOCATION` |

Orden: 1) base de datos con el esquema `auth` creado; 2) `auth-api` (aplica su Flyway); 3) `scoring-api`
(aplica V1–V9 en `public`); 4) `evaluation-query-api` (no migra; necesita las tablas de `scoring-api`).
Las dos API consumidoras usan la clave **pública** del par de `auth-api`; la privada solo la tiene `auth-api`.
En local, `AUTH_COOKIE_SECURE=false` puede ser necesario si el frontend se sirve por HTTP (no verificado).

## 8. Bloqueos y limitaciones

- **HU14 (recomendaciones y saga):** no implementada. Depende del catálogo aprobado por el negocio, de la
  identidad de servicio entre `scoring-api` y `evaluation-query-api` y de la migración V11 (propuesta).
- **Consulta de una evaluación por el administrador:** requisito aprobado (HU10 CA04, P-HU10-01), pero
  `scoring-api` sigue respondiendo 403 al `ADMINISTRADOR_RIESGO` en `GET /api/v1/evaluaciones/{id}`; el cambio
  no está autorizado todavía. El administrador sí puede usar la explicación y el historial.
- **Estado de la saga:** no existe todavía; las evaluaciones no tienen estado `PENDIENTE`/`COMPLETADA`.
- **No verificado:** flujo con las tres API levantadas a la vez, tokens reales de `auth-api`, rol de base de
  datos de solo lectura para `evaluation-query-api` y tiempos de respuesta en un entorno representativo.
