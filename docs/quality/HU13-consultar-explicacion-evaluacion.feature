# language: es
Característica: Consultar la explicación de una evaluación
  Como analista de crédito
  Quiero consultar el detalle de los factores que determinaron una evaluación
  Para comprender cómo se obtuvo el puntaje y justificar el resultado

  # API responsable: evaluation-query-api. Esta historia explica una evaluación ya registrada; no duplica el
  # motor de scoring de scoring-api ni lo vuelve a ejecutar.
  # Estado (2026-10-18): implementada en evaluation-query-api sobre el esquema actual de scoring-api (V1–V9),
  # verificada por ConsultasEvaluacionIntegracionTest (H2) y EsquemaScoringContratoIT (PostgreSQL, esquema
  # temporal). Pendiente: el 409 para evaluaciones PENDIENTE (P-HU13-02), que depende del estado de la saga
  # (V11 de scoring-api, no aplicada); la medición de CA10 en un entorno representativo.
  #
  # Criterios originales (texto sin cambios):
  #   CA01. Permitir consultar una evaluación por su identificador.
  #   CA02. Mostrar el identificador, fecha, puntaje total y clasificación de la evaluación.
  #   CA03. Mostrar las versiones históricas de las reglas utilizadas.
  #   CA04. Indicar si se cumplió la condición de cada regla aplicada.
  #   CA05. Mostrar la contribución individual de cada regla, incluyendo cero cuando su condición no se
  #         cumplió.
  #   CA06. Garantizar que la suma de las contribuciones coincida con el puntaje total almacenado.
  #   CA07. Utilizar los datos históricos persistidos, sin recalcular el puntaje ni aplicar reglas actuales.
  #   CA08. Retornar HTTP 404 si la evaluación no existe.
  #   CA09. Permitir el acceso a los roles autorizados.
  #   CA10. Responder en un máximo de dos segundos bajo condiciones normales.
  #   CA11. No exponer datos personales o financieros que no sean necesarios para explicar la evaluación.
  #   CA12. Incorporar el identificador de trazabilidad X-Trace-Id en las respuestas según el contrato
  #         técnico común.
  #
  # Aclaraciones (2026-10-18; no cambian el alcance):
  # - Datos de origen (scoring-api, persistidos al evaluar): la evaluación (identificador, fecha, puntaje
  #   total, categoría y versión de la política, estas tres desde HU12) y un detalle por regla aplicada con
  #   la instantánea de la regla (operador, valor de la condición y puntaje de la regla en ese momento), si
  #   se cumplió la condición y el puntaje obtenido. El puntaje obtenido es el de la regla si la condición se
  #   cumplió y 0 si no (CA05).
  # - CA07: los valores de cada regla se toman de la instantánea del detalle, nunca de la regla actual, que
  #   puede haberse editado después. No se vuelve a evaluar ninguna condición.
  # - CA09: los roles autorizados son ADMINISTRADOR_RIESGO y ANALISTA_CREDITO (HU10 CA04, "explicaciones").
  # - Responsabilidades: HU13 explica una evaluación; HU14 consulta sus recomendaciones; HU15 lista el
  #   historial del solicitante y enlaza con HU13 y HU14.
  # - Las evaluaciones PENDIENTE (saga de HU14) no se presentan como completas (ver P-HU13-02).
  #
  # Decisiones aprobadas el 2026-10-18 (se aprobaron las propuestas siguientes tal como estaban redactadas):
  # - P-HU13-01. Ruta: GET /api/v1/evaluaciones/{idEvaluacion}/explicacion (implementada).
  # - P-HU13-02. Evaluación PENDIENTE: 409 con el código EVALUACION_PENDIENTE (igual que P-HU14-03). Pendiente
  #   de implementación: requiere el estado de la saga (V11).
  # - P-HU13-03. CA03: las reglas de scoring se editan sin número de versión; la versión histórica disponible
  #   es la instantánea persistida en cada detalle. Propuesta: considerarla la versión histórica de la regla.
  #   Si se exige un identificador de versión, hace falta una decisión y un cambio de modelo en scoring-api.
  # - P-HU13-04. CA02 y CA06 en evaluaciones registradas antes de HU12, que no tienen puntaje total ni
  #   clasificación persistidos. Propuesta: mostrar la clasificación vacía, sin asignarla, y el puntaje como
  #   suma de sus detalles identificada expresamente como valor derivado; CA06 solo es comprobable cuando
  #   existe un puntaje total almacenado.
  # - P-HU13-05. CA06 cuando la suma no coincide con el puntaje almacenado. Propuesta: tratarlo como un error
  #   de integridad (500 INTERNAL_ERROR registrado en el log), sin corregirlo ni ocultarlo.
  # - P-HU13-06. CA11: se excluyen el nombre, el documento, los ingresos, las deudas y los valores reales del
  #   solicitante. La propuesta dejaba abierto el identificador del solicitante: no se incluye (criterio de
  #   minimización); incluirlo requeriría una decisión nueva.
  # - P-HU13-07. Código de error del 404: EVALUACION_NO_ENCONTRADA, con el mismo formato ApiError que
  #   scoring-api.
  # P-HU13-03, P-HU13-04 y P-HU13-05 se aprobaron tal como están redactadas arriba.

  Escenario: Consulta de la explicación de una evaluación (CA01, CA02)
    Dado una evaluación COMPLETADA registrada con su puntaje total y su clasificación
    Cuando un usuario autorizado consulta su explicación por el identificador de la evaluación
    Entonces la respuesta es 200
    Y muestra el identificador, la fecha, el puntaje total y la clasificación de la evaluación

  Escenario: Reglas históricas, condiciones y contribuciones (CA03, CA04, CA05)
    Dado una evaluación en la que se aplicaron una regla cuya condición se cumplió y otra cuya condición no se cumplió
    Cuando un usuario autorizado consulta su explicación
    Entonces cada regla aplicada muestra su versión histórica: operador, valor de la condición y puntaje vigentes al evaluar
    Y indica si su condición se cumplió
    Y muestra su contribución individual
    Y la contribución de la regla cuya condición no se cumplió es 0

  Escenario: Consistencia con el puntaje almacenado (CA06)
    Dado una evaluación con un puntaje total almacenado
    Cuando un usuario autorizado consulta su explicación
    Entonces la suma de las contribuciones individuales es igual al puntaje total almacenado

  Escenario: Sin recalcular ni aplicar reglas actuales (CA07)
    Dado una evaluación registrada con una regla de puntaje 20
    Y que después esa regla se editó con puntaje 30
    Cuando un usuario autorizado consulta la explicación de la evaluación
    Entonces la regla muestra el puntaje 20 con que se evaluó
    Y el puntaje total y la clasificación son los persistidos, sin recalcular

  Escenario: Evaluación inexistente (CA08)
    # Código de error aprobado (P-HU13-07).
    Cuando un usuario autorizado consulta la explicación de una evaluación que no existe
    Entonces la respuesta es 404 con el código EVALUACION_NO_ENCONTRADA

  Esquema del escenario: Acceso por rol (CA09, HU10 CA04, CA06 y CA07)
    Cuando <usuario> consulta la explicación de una evaluación
    Entonces la respuesta es <respuesta>

    Ejemplos:
      | usuario                                 | respuesta               |
      | un usuario con rol ADMINISTRADOR_RIESGO | 200                     |
      | un usuario con rol ANALISTA_CREDITO     | 200                     |
      | una petición sin token                  | 401 con NO_AUTENTICADO  |
      | un token con un rol desconocido         | 403 con ACCESO_DENEGADO |

  Escenario: Tiempo de respuesta (CA10)
    Dado condiciones normales de operación
    Cuando un usuario autorizado consulta la explicación de una evaluación
    Entonces la respuesta se entrega en un máximo de dos segundos

  Escenario: Sin datos personales ni financieros innecesarios (CA11)
    Cuando un usuario autorizado consulta la explicación de una evaluación
    Entonces la respuesta no incluye el nombre, el documento, los ingresos ni las deudas del solicitante

  Escenario: Trazabilidad (CA12)
    Dado una petición con una cabecera X-Trace-Id válida
    Cuando un usuario autorizado consulta la explicación de una evaluación
    Entonces la respuesta devuelve el mismo X-Trace-Id
    Y las respuestas de error incluyen ese traceId

  @aprobado @pendiente-de-implementacion
  Escenario: Explicación de una evaluación PENDIENTE (P-HU13-02, requiere V11)
    Dado una evaluación PENDIENTE
    Cuando un usuario autorizado consulta su explicación
    Entonces la respuesta es 409 con el código EVALUACION_PENDIENTE

  @aprobado
  Escenario: Evaluación registrada antes de HU12 (P-HU13-04)
    Dado una evaluación sin puntaje total ni clasificación persistidos
    Cuando un usuario autorizado consulta su explicación
    Entonces se muestra sin clasificación, sin asignarle ninguna
    Y su puntaje se identifica expresamente como la suma derivada de sus detalles históricos

  @aprobado
  Escenario: Suma inconsistente con el puntaje almacenado (P-HU13-05)
    Dado una evaluación cuya suma de contribuciones no coincide con su puntaje total almacenado
    Cuando un usuario autorizado consulta su explicación
    Entonces la respuesta es 500 con el código INTERNAL_ERROR
    Y la inconsistencia queda registrada sin corregirse
