# HU14: decisiones (actualizado el 2026-10-19)

La parte técnica de HU14 está implementada y verificada: unitarias, integración con H2 y PostgreSQL, y un
flujo de extremo a extremo con las tres API, incluidos concurrencia, reinicios y un catálogo no aprobado. Las
decisiones 1 y 2 quedaron resueltas por María Alejandra el 2026-10-19; las demás siguen pendientes.

| # | Decisión | Propuesta técnica (implementada salvo que se indique) | Implicaciones | Decisión necesaria |
|---|---|---|---|---|
| 1 | Catálogo de recomendaciones (P-HU14-01) | JSON versionado con `estado` BORRADOR/APROBADO; solo uno APROBADO genera. El incluido (`BORRADOR-1`, 7 entradas) es un borrador | Sin catálogo APROBADO, la generación responde 503 y la evaluación sigue PENDIENTE. Tras 10 intentos (unos 20 min con la espera exponencial) el trabajo pasa a ERROR_PERMANENTE y requiere `reactivar_trabajo_recomendacion` por evaluación. Verificado en el ensayo | **Resuelta el 2026-10-19**: BORRADOR-1 aprobado sin cambios como versión 1 |
| 2 | Factor desfavorable (P-HU14-02) | Condición cumplida con contribución negativa; una condición incumplida no cuenta | Con la política actual, solo las reglas de puntaje negativo generan recomendaciones por factor. Alternativa: contar también una regla de puntaje positivo no cumplida ("le faltó un factor favorable") | **Resuelta el 2026-10-19**: se confirma la definición implementada |
| 3 | 409 para una evaluación PENDIENTE (P-HU14-03) | 409 `EVALUACION_PENDIENTE`, igual que en HU13 (P-HU13-02, aprobado) | El frontend debe mostrar "en proceso" y volver a consultar | Confirmar para HU14 |
| 4 | Evaluaciones anteriores a la saga (P-HU14-04) | 200 con `generadas: false` y lista vacía; sin recomendaciones retroactivas (regla de la saga aprobada) | Distingue "no aplica" de "no existe" (404) y de "en proceso" (409) | Confirmar |
| 5 | Rutas y contratos (P-HU14-05) | `GET /api/v1/evaluaciones/{id}/recomendaciones` (ambos roles) y `PUT /internal/v1/evaluaciones/{id}/recomendaciones` (solo el token de servicio; 201 o 200 con `{idEvaluacion, versionCatalogo, cantidad}`) | La ruta interna no aparece en Swagger ni forma parte del contrato del frontend | Confirmar |
| 6 | Esquema propio y tablas (P-HU14-06, CA17) | Esquema `consulta` con `generacion_recomendaciones` (idempotencia por clave primaria) y `recomendacion_evaluacion` (texto histórico, CA05), inmutables por triggers | Dos tablas nuevas, fuera de las cinco del modelo base de scoring-api y en otra API; requiere la justificación de CA17 | Aprobar la justificación |
| 7 | Códigos de error (P-HU14-08) | `EVALUACION_NO_ENCONTRADA` (404) y `EVALUACION_PENDIENTE` (409) públicos; `EVALUACION_NO_PENDIENTE` (409) y `CATALOGO_NO_DISPONIBLE` (503) solo en la operación interna | Formato común `ApiError` con `traceId` | Confirmar |
| 8 | Identidad de servicio y reintentos (P-HU14-07) | **Aprobados el 2026-10-18** e implementados: S1 con RS256 y rotación por `kid`; 10 intentos con espera de 5 s a 300 s y ±20 % | El par de servicio es distinto del de `auth-api` | Operación: custodia de la clave privada y periodicidad de rotación |
| 9 | Idempotencia y recuperación (CA06, CA14–CA16) | Outbox con concesión y barrera por token; reintento que recibe la generación guardada; reenvío programado; COMPLETADA solo con el trabajo PROCESADO (trigger diferido de V11) | Verificado: ninguna evaluación COMPLETADA sin recomendaciones; 6 generaciones simultáneas guardan una; recuperación tras caer `evaluation-query-api` y tras reiniciar `scoring-api` | Operación: quién vigila los trabajos en ERROR_PERMANENTE y ejecuta la reactivación |

Pendiente: la confirmación formal de 3–7 y quién asume el seguimiento operativo de 8 y 9.
