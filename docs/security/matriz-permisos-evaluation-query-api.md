# Matriz de permisos de evaluation-query-api (HU10)

La sección "Comportamiento implementado" está aplicada en
`src/main/java/com/crediticio/evaluationquery/config/SecurityConfig.java` y verificada por
`src/test/java/com/crediticio/evaluationquery/config/SeguridadIntegracionTest.java`. Las demás secciones
describen requisitos y propuestas todavía no implementados.

## Autenticación

- Cada petición protegida exige un JWT de acceso emitido por `auth-api` en la cabecera
  `Authorization: Bearer <token>`. No hay sesión HTTP ni llamadas a `auth-api`.
- El token se valida localmente con la clave pública (`EVALUATION_QUERY_JWT_PUBLIC_KEY_LOCATION`):
  firma RS256, emisor (`AUTH_JWT_ISSUER`), audiencia que incluya `evaluation-query-api`, `exp`
  obligatorio con 60 s de tolerancia de reloj, `sub` con formato UUID y `role` de texto.
- El rol proviene únicamente del claim `role` del token validado.

## Comportamiento implementado

| Ruta | ADMINISTRADOR_RIESGO | ANALISTA_CREDITO | Sin token o token inválido | Requisito |
| --- | --- | --- | --- | --- |
| `GET /api/v1/evaluaciones/{idEvaluacion}/explicacion` | Permitido | Permitido | 401 | HU13 CA09, HU10 CA04 |
| `GET /api/v1/solicitantes/{idSolicitante}/evaluaciones` | Permitido | Permitido | 401 | HU15 CA10, HU10 CA04 |
| `GET /actuator/health` | Público, sin detalles | Público, sin detalles | Público | Operación |
| Cualquier otra ruta o método | 403 | 403 | 401 | Denegada por defecto |

Implementadas el 2026-10-18 y verificadas por `ConsultasEvaluacionIntegracionTest` (cadena de seguridad real,
ambos roles 200, 401 sin token, 403 con un rol desconocido, otros métodos y rutas 403).

## Requisitos aprobados pendientes de implementación (2026-10-18)

- **HU10 CA04:** ambos roles (`ADMINISTRADOR_RIESGO` y `ANALISTA_CREDITO`) pueden consultar recomendaciones
  (HU14 CA11). Las consultas de explicaciones (HU13) e historial (HU15) ya están implementadas (arriba).
- **P-HU10-03:** validación de los tokens conforme al contrato común, con comprobación de `exp`, `sub` y `role`.
  Los detalles que el contrato no define (obligatoriedad de `iat`, `jti` y `kid`, tratamiento de `nbf` y
  composición de la audiencia) siguen pendientes. La sección "Autenticación" describe las comprobaciones
  actuales de esta API.
- **HU14:** la generación de recomendaciones es una operación interna que ningún token de usuario autoriza;
  exige una identidad de servicio.

## Endpoints propuestos no implementados

Rutas propuestas para HU14, **no aplicadas** y pendientes de aprobación (P-HU14-05); los roles son los del
requisito aprobado HU10 CA04.

| Ruta propuesta | ADMINISTRADOR_RIESGO | ANALISTA_CREDITO | Sin token o token inválido | Origen |
| --- | --- | --- | --- | --- |
| `GET /api/v1/evaluaciones/{idEvaluacion}/recomendaciones` | Permitido | Permitido | 401 | HU14 CA07, CA11 (P-HU14-05) |
| `PUT /internal/v1/evaluaciones/{idEvaluacion}/recomendaciones` | Rechazado (ningún token de usuario) | Rechazado (ningún token de usuario) | 401 | HU14 (P-HU14-05): solo identidad de servicio |

## Propuestas pendientes de decisión

- **P-HU10-04:** relación entre la ventana de 15 minutos de CA10 y los 60 s de tolerancia de reloj sobre `exp`.
- **Identidad de servicio (P-HU14-07):** diseño S1 preferido; sus valores (emisor, audiencia, sujeto, `kid`,
  permiso y duración) y su forma de rechazo están pendientes de aprobación.
- **Códigos de error:** `EVALUACION_NO_ENCONTRADA` (P-HU13-07) y `SOLICITANTE_NO_ENCONTRADO` (P-HU15-05),
  aprobados el 2026-10-18 e implementados con el mismo formato y los mismos nombres que `scoring-api`.
  `EVALUACION_PENDIENTE` (P-HU13-02, P-HU14-03) se aplicará cuando exista el estado de la saga (V11).

Requisitos en `docs/quality/HU13-consultar-explicacion-evaluacion.feature`,
`docs/quality/HU14-generar-consultar-recomendaciones.feature` y
`docs/quality/HU15-consultar-historial-evaluaciones.feature`; requisitos transversales de HU10 en
`scoring-api/docs/quality/HU10-autorizar-operaciones-segun-rol.feature`.

## Respuestas de error

| Situación | Estado | `errorCode` |
| --- | --- | --- |
| Sin token, token mal formado, firma inválida, expirado, emisor o audiencia incorrectos, claims obligatorios ausentes | 401 | `NO_AUTENTICADO` |
| Token válido sin permiso para la ruta o con un rol desconocido | 403 | `ACCESO_DENEGADO` |

Ambas respuestas usan el formato `ApiError` e incluyen el `traceId` de la cabecera `X-Trace-Id`.
