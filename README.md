# evaluation-query-api

API del Motor de Scoring de Riesgo Crediticio para consultar la explicación de una evaluación (HU13), sus
recomendaciones (HU14) y el historial de evaluaciones de un solicitante (HU15), con la autenticación y
autorización transversal de HU10.

Estado (2026-10-18): HU13 y HU15 implementadas sobre el esquema actual de `scoring-api` (V1–V9, sin el
estado de la saga). HU14 (recomendaciones y saga) **no está implementada**: depende del catálogo aprobado por
el negocio, de la identidad de servicio y de la migración V11 de `scoring-api` (ver
`docs/quality/HU14-generar-consultar-recomendaciones.feature`).

## Arquitectura

- Aplicación Spring Boot independiente (Java 21, Maven), organizada como monolito modular con Package by
  Feature (`explicacion`, `historial`) y Ports & Adapters.
- Valida localmente los JWT de acceso emitidos por `auth-api`, sin llamarla en cada petición.
- Lee con SELECT (JDBC, `NamedParameterJdbcTemplate`) las tablas de `scoring-api` en la misma base
  PostgreSQL: `evaluacion`, `detalle_evaluacion`, `regla_scoring`, `riesgo`, `politica_clasificacion` y, de
  `solicitante`, solo el identificador. No escribe en `public`, no ejecuta Flyway y no recalcula evaluaciones.

## Endpoints

Todos exigen `Authorization: Bearer <token>` de `auth-api` con rol `ADMINISTRADOR_RIESGO` o `ANALISTA_CREDITO`
(HU10 CA04). Errores con el formato común `{errorCode, message, details, traceId}`; todas las respuestas llevan
`X-Trace-Id` (se reutiliza el recibido si es un UUID válido).

| Método y ruta | Respuestas |
|---|---|
| `GET /api/v1/evaluaciones/{idEvaluacion}/explicacion` | 200 explicación; 400 `VALIDATION_ERROR`; 401 `NO_AUTENTICADO`; 403 `ACCESO_DENEGADO`; 404 `EVALUACION_NO_ENCONTRADA`; 500 `INTERNAL_ERROR` si los datos persistidos son incoherentes |
| `GET /api/v1/solicitantes/{idSolicitante}/evaluaciones` | 200 lista (vacía si no tiene evaluaciones); 400; 401; 403; 404 `SOLICITANTE_NO_ENCONTRADO` |
| `GET /actuator/health` | Público, sin detalles |
| `GET /v3/api-docs`, `/swagger-ui.html` | Solo con el perfil `dev` (`-Dspring-boot.run.profiles=dev`); si no, 401 |

Explicación (HU13):

```json
{
  "idEvaluacion": 101,
  "fechaEvaluacion": "2026-10-10T10:00:00",
  "scoreTotal": -20,
  "origenPuntaje": "PERSISTIDO",
  "clasificada": true,
  "categoria": "MEDIO",
  "versionPolitica": 1,
  "factores": [
    { "idRegla": 7, "variable": "NUMERO_MORAS", "operadorAplicado": ">", "valorCondicionAplicado": "2",
      "puntajeReglaAplicado": -20, "condicionCumplida": true, "contribucion": -20 },
    { "idRegla": 3, "variable": "INGRESOS_MENSUALES", "operadorAplicado": ">=", "valorCondicionAplicado": "3000000",
      "puntajeReglaAplicado": 20, "condicionCumplida": false, "contribucion": 0 }
  ]
}
```

Historial (HU15), de la más reciente a la más antigua y, a igual fecha, por identificador descendente:

```json
[
  { "idEvaluacion": 101, "fechaEvaluacion": "2026-10-10T10:00:00", "scoreTotal": -20,
    "origenPuntaje": "PERSISTIDO", "clasificada": true, "categoria": "MEDIO", "versionPolitica": 1,
    "explicacion": "/api/v1/evaluaciones/101/explicacion" }
]
```

- Las reglas se muestran con la instantánea guardada al evaluar (operador, valor y puntaje), no con su valor
  actual. La contribución es 0 cuando la condición no se cumplió.
- Evaluaciones anteriores a HU12: `clasificada: false`, `categoria` y `versionPolitica` en `null`, y
  `scoreTotal` igual a la suma de sus detalles con `origenPuntaje: "DERIVADO_DE_DETALLES"`.
- Las respuestas no incluyen el identificador ni los datos personales o financieros del solicitante.
- La referencia a las recomendaciones (HU14) se añadirá al historial cuando HU14 esté implementada.

## Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `AUTH_JWT_ISSUER` | Sí | Emisor de los JWT; debe ser el mismo valor configurado en `auth-api` (allí, `auth-api` por defecto) |
| `EVALUATION_QUERY_JWT_PUBLIC_KEY_LOCATION` | Sí | Ubicación de la clave pública RS256 de `auth-api`, p. ej. `file:/ruta/jwt-publica.pem` |
| `EVALUATION_QUERY_DB_URL` | Sí | Base de `scoring-api`, con el formato `postgresql://host:puerto/base` (Spring agrega `jdbc:`) |
| `EVALUATION_QUERY_DB_USERNAME` / `EVALUATION_QUERY_DB_PASSWORD` | Sí | Credenciales de un rol con **solo lectura** sobre las tablas consultadas |
| `CORS_ALLOWED_ORIGINS` | No | Orígenes del frontend separados por coma (por defecto `http://localhost:3000`) |
| `EVALUATION_QUERY_API_PORT` | No | Puerto HTTP (por defecto 8082) |

Sin `AUTH_JWT_ISSUER`, sin una clave pública válida o sin la conexión a la base la aplicación no arranca.
Las conexiones se marcan de solo lectura, pero la garantía real es el rol de base de datos: debe crearlo el
administrador de la base con `SELECT` únicamente (script pendiente de autorización).

## Clave pública

- `auth-api` firma los tokens con su clave privada (RS256). La clave privada **solo** se instala en
  `auth-api` y nunca se copia a esta API ni se versiona (`.gitignore` excluye `*.pem`).
- Esta API recibe únicamente la clave pública X.509 en formato PEM (`-----BEGIN PUBLIC KEY-----`) del
  mismo par que usa `auth-api` (su variable `AUTH_JWT_PUBLIC_KEY_LOCATION`). El procedimiento de
  generación del par está descrito en el README de `auth-api`.
- `auth-api` incluye `evaluation-query-api` en la audiencia de sus tokens por defecto
  (`AUTH_JWT_AUDIENCES`); si se cambia esa lista, debe seguir incluyéndola.

## Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas generan claves RSA efímeras y usan H2 en memoria con una réplica mínima del esquema de
`scoring-api` (`src/test/resources/esquema-scoring-h2.sql`); no requieren claves reales ni PostgreSQL.

`EsquemaScoringContratoIT` comprueba las consultas contra el esquema real: aplica las migraciones V1–V9 de
`scoring-api` (carpeta hermana `../scoring-api`) en un esquema temporal de `motor_scoring_it` y lo elimina al
terminar. Requiere autorización expresa para usar esa base:

```powershell
.\mvnw.cmd -B test "-Dtest=EsquemaScoringContratoIT" "-Dit.base-dedicada=motor_scoring_it" "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/motor_scoring_it"
```

con `DB_USERNAME` y `DB_PASSWORD` en el entorno. La matriz de permisos está en
`docs/security/matriz-permisos-evaluation-query-api.md` y los requisitos en `docs/quality/`.
