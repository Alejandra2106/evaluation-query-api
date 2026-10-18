# language: es
Característica: Consultar el historial de evaluaciones de un solicitante
  Como analista de crédito
  Quiero consultar el historial de evaluaciones de un solicitante
  Para revisar la evolución de sus resultados a lo largo del tiempo

  # API responsable: evaluation-query-api.
  # Este documento especifica requisitos; no declara la historia implementada. Estado al 2026-10-18: no
  # implementada.
  #
  # Criterios originales (texto sin cambios):
  #   CA01. Permitir consultar el historial mediante el identificador del solicitante.
  #   CA02. Verificar la existencia del solicitante antes de consultar su historial.
  #   CA03. Retornar HTTP 404 si el solicitante no existe.
  #   CA04. Retornar HTTP 200 con una lista vacía si el solicitante existe, pero todavía no tiene evaluaciones.
  #   CA05. Mostrar el identificador de cada evaluación, fecha, puntaje total y clasificación.
  #   CA06. Ordenar las evaluaciones desde la más reciente hasta la más antigua, con un criterio secundario
  #         estable cuando las fechas coincidan.
  #   CA07. Utilizar los resultados históricos persistidos, sin recalcular puntajes ni clasificaciones.
  #   CA08. No presentar evaluaciones PENDIENTE como evaluaciones completas dentro del historial.
  #   CA09. Permitir consultar el detalle de una evaluación mediante HU13 y sus recomendaciones mediante HU14.
  #   CA10. Permitir el acceso a los roles autorizados.
  #   CA11. Garantizar que la consulta no modifique evaluaciones, clasificaciones ni recomendaciones.
  #   CA12. Responder en un máximo de dos segundos bajo condiciones normales.
  #   CA13. Evitar exponer información personal o financiera innecesaria.
  #   CA14. Incorporar el identificador de trazabilidad X-Trace-Id según el contrato técnico común.
  #
  # Aclaraciones (2026-10-18; no cambian el alcance):
  # - CA06: criterio secundario estable = identificador de la evaluación, descendente.
  # - CA08 (decisión aprobada para el diseño): las evaluaciones PENDIENTE se excluyen del historial; se
  #   incluyen las COMPLETADA y las anteriores a la saga (estado nulo). La exclusión no impide que la saga
  #   recupere las pendientes.
  # - CA09: el historial solo referencia los contratos de HU13 (explicación, HU13-consultar-explicacion-
  #   evaluacion.feature) y HU14 (recomendaciones, HU14-generar-consultar-recomendaciones.feature); no devuelve
  #   detalles por regla ni recomendaciones, para no duplicar sus responsabilidades. Responsabilidades: HU15
  #   lista el historial del solicitante; HU13 explica una evaluación; HU14 consulta sus recomendaciones.
  # - CA10: los roles autorizados son ADMINISTRADOR_RIESGO y ANALISTA_CREDITO (HU10 CA04).
  # - CA13: cada elemento no incluye nombre, documento, ingresos, deudas ni otros datos del solicitante.
  # - CA05 y CA07: las evaluaciones anteriores a la clasificación (HU12) no tienen clasificación persistida;
  #   se muestran sin clasificación y nunca se reclasifican.
  #
  # Decisiones pendientes de aprobación (no se inventa una regla):
  # - P-HU15-01. Puntaje de las evaluaciones anteriores a HU12, que no tienen puntaje total persistido.
  #   Propuesta: mostrar la suma de sus detalles históricos identificada expresamente como valor derivado,
  #   nunca como dato persistido originalmente.
  # - P-HU15-02. Ruta: propuesta GET /api/v1/solicitantes/{idSolicitante}/evaluaciones, pendiente de
  #   implementación.
  # - P-HU15-03. Forma de las referencias de CA09: depende de las rutas propuestas para HU13 (P-HU13-01) y
  #   HU14 (P-HU14-05), ambas pendientes de implementación.
  # - P-HU15-05. Código de error del 404: propuesto SOLICITANTE_NO_ENCONTRADO (el mismo que usa scoring-api),
  #   sujeto a verificar su compatibilidad con el contrato común de errores.
  # - P-HU15-04. Campos adicionales por evaluación (estado, indicador de clasificada, versión de la política,
  #   origen del puntaje): propuestos, no aprobados.

  Escenario: Historial de un solicitante con evaluaciones (CA01, CA02, CA05, CA06, CA07)
    Dado un solicitante con tres evaluaciones COMPLETADA de fechas distintas
    Cuando un usuario autorizado consulta su historial por el identificador del solicitante
    Entonces la respuesta es 200 con las tres evaluaciones de la más reciente a la más antigua
    Y cada evaluación muestra su identificador, su fecha, su puntaje total y su clasificación persistidos
    Y no se recalcula ningún puntaje ni ninguna clasificación

  Escenario: Desempate estable cuando las fechas coinciden (CA06)
    Dado un solicitante con dos evaluaciones de la misma fecha
    Cuando un usuario autorizado consulta su historial
    Entonces aparece primero la evaluación con el identificador mayor
    Y el orden es el mismo en cada consulta

  Escenario: Solicitante inexistente (CA02, CA03)
    # Código de error propuesto (P-HU15-05).
    Cuando un usuario autorizado consulta el historial de un solicitante que no existe
    Entonces la respuesta es 404 con el código SOLICITANTE_NO_ENCONTRADO
    Y no se consulta ninguna evaluación

  Escenario: Solicitante sin evaluaciones (CA04)
    Dado un solicitante existente sin evaluaciones
    Cuando un usuario autorizado consulta su historial
    Entonces la respuesta es 200 con una lista vacía

  Escenario: Las evaluaciones PENDIENTE no se presentan como completas (CA08)
    Dado un solicitante con una evaluación COMPLETADA y otra PENDIENTE
    Cuando un usuario autorizado consulta su historial
    Entonces el historial contiene solo la evaluación COMPLETADA

  Escenario: Evaluaciones anteriores a la saga y a la clasificación (CA05, CA07)
    Dado un solicitante con una evaluación registrada antes de incorporar la clasificación
    Cuando un usuario autorizado consulta su historial
    Entonces la evaluación aparece sin clasificación
    Y no se le asigna ninguna clasificación

  Escenario: Acceso al detalle y a las recomendaciones (CA09)
    Cuando un usuario autorizado consulta el historial de un solicitante
    Entonces cada evaluación incluye la referencia para consultar su explicación (HU13) y sus recomendaciones (HU14)
    Y el historial no incluye detalles por regla ni recomendaciones

  Esquema del escenario: Acceso por rol (CA10, HU10 CA04, CA06 y CA07)
    Cuando <usuario> consulta el historial de un solicitante
    Entonces la respuesta es <respuesta>

    Ejemplos:
      | usuario                                 | respuesta               |
      | un usuario con rol ADMINISTRADOR_RIESGO | 200                     |
      | un usuario con rol ANALISTA_CREDITO     | 200                     |
      | una petición sin token                  | 401 con NO_AUTENTICADO  |
      | un token con un rol desconocido         | 403 con ACCESO_DENEGADO |

  Escenario: La consulta no modifica datos (CA11)
    Cuando un usuario autorizado consulta el historial de un solicitante
    Entonces no se modifica ninguna evaluación, clasificación ni recomendación

  Escenario: Tiempo de respuesta (CA12)
    Dado condiciones normales de operación
    Cuando un usuario autorizado consulta el historial de un solicitante
    Entonces la respuesta se entrega en un máximo de dos segundos

  Escenario: Sin datos personales ni financieros innecesarios (CA13)
    Cuando un usuario autorizado consulta el historial de un solicitante
    Entonces ninguna evaluación incluye el nombre, el documento, los ingresos ni las deudas del solicitante

  Escenario: Trazabilidad (CA14)
    Dado una petición con una cabecera X-Trace-Id válida
    Cuando un usuario autorizado consulta el historial de un solicitante
    Entonces la respuesta devuelve el mismo X-Trace-Id
    Y las respuestas de error incluyen ese traceId

  @pendiente-aprobacion
  Escenario: Puntaje de una evaluación anterior a HU12 (P-HU15-01)
    Dado una evaluación registrada antes de HU12, sin puntaje total persistido
    Cuando un usuario autorizado consulta el historial de su solicitante
    Entonces su puntaje se muestra como la suma de sus detalles históricos
    Y se identifica expresamente como un valor derivado
