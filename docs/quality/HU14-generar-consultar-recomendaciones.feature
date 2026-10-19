# language: es
Característica: Generar y consultar recomendaciones
  Como analista de crédito
  Quiero consultar recomendaciones asociadas a una evaluación
  Para identificar acciones que puedan ayudar a mejorar el perfil de riesgo del solicitante

  # API responsable: evaluation-query-api, coordinada con scoring-api mediante la saga aprobada.
  # Estado (2026-10-18): implementada. scoring-api: saga con V11 promovida (estado y outbox), intento en línea
  # y reenvío programado. evaluation-query-api: generación interna idempotente con el token de servicio S1,
  # consulta, esquema propio con su V1 y catálogo en JSON. Verificada con pruebas unitarias y de integración
  # (H2), EsquemaScoringContratoIT y TrabajoRecomendacionAdapterIT (PostgreSQL, esquemas temporales) y un flujo
  # de extremo a extremo con las tres API en motor_scoring_it, con un catálogo APROBADO solo de prueba.
  # V10 y V11 no están aplicadas en motor_scoring. El catálogo incluido es un BORRADOR (P-HU14-01): con él no
  # se genera ninguna recomendación. P-HU14-02 a P-HU14-06 y P-HU14-08 se implementaron tal como están
  # propuestos y siguen pendientes de confirmación formal.
  #
  # Criterios originales (texto sin cambios):
  #   CA01. Generar las recomendaciones durante el proceso de evaluación, no durante la primera consulta.
  #   CA02. Utilizar la clasificación persistida y los factores desfavorables identificados en la evaluación.
  #   CA03. Obtener las recomendaciones de un catálogo explícito y versionado.
  #   CA04. Persistir las recomendaciones asociadas al identificador de la evaluación.
  #   CA05. Conservar el texto histórico de cada recomendación aunque posteriormente cambie el catálogo.
  #   CA06. Garantizar que los reintentos no dupliquen ni sobrescriban indebidamente las recomendaciones ya
  #         generadas.
  #   CA07. Permitir consultar las recomendaciones por identificador de evaluación.
  #   CA08. Retornar HTTP 404 cuando la evaluación consultada no exista.
  #   CA09. No recalcular el puntaje ni la clasificación para generar o consultar recomendaciones.
  #   CA10. No interpretar las recomendaciones como una aprobación o rechazo automático del crédito.
  #   CA11. Permitir el acceso a los roles autorizados.
  #   CA12. Responder en un máximo de dos segundos para las consultas bajo condiciones normales.
  #   CA13. Garantizar que una evaluación no se presente como completada mientras las recomendaciones
  #         requeridas no estén persistidas correctamente.
  #   CA14. Incorporar mecanismos de idempotencia, reintentos y recuperación de operaciones incompletas.
  #   CA15. Contemplar el escenario en que las recomendaciones se guardan correctamente, pero falla la
  #         confirmación final de la evaluación.
  #   CA16. Conservar información suficiente para reanudar las operaciones pendientes después de un fallo o
  #         reinicio.
  #   CA17. No agregar tablas, dependencias o infraestructura adicional hasta justificar su necesidad y
  #         verificar su compatibilidad con las restricciones del proyecto.
  #
  # Regla transversal de la saga (texto sin cambios):
  #   La evaluación manejará los estados PENDIENTE y COMPLETADA.
  #   PENDIENTE: la operación no ha terminado; no debe presentarse como una evaluación completa.
  #   COMPLETADA: el puntaje, la clasificación, los detalles y las recomendaciones requeridas están
  #   persistidos correctamente.
  #   Los reintentos deberán ser seguros y no generar duplicados. Las transacciones locales de cada API no se
  #   considerarán una transacción distribuida.
  #
  # Aclaraciones (2026-10-18; no cambian el alcance):
  # - Responsabilidades: HU14 consulta las recomendaciones persistidas y establece los requisitos de su
  #   generación mediante la saga; no explica la evaluación (HU13) ni lista el historial (HU15).
  # - CA11: los roles autorizados son ADMINISTRADOR_RIESGO y ANALISTA_CREDITO (HU10 CA04). La generación es
  #   una operación interna: no la autoriza ningún rol de usuario, sino una identidad de servicio (HU10).
  # - CA01 y CA13: la generación la inicia la saga de scoring-api al registrar la evaluación; una consulta
  #   nunca genera recomendaciones.
  # - Transiciones verificables: toda evaluación nueva se registra PENDIENTE junto con su trabajo de saga, en
  #   la misma transacción local de scoring-api. Pasa a COMPLETADA solo cuando evaluation-query-api confirma
  #   que la generación quedó persistida. COMPLETADA es definitiva. Las evaluaciones anteriores a la saga
  #   conservan un estado nulo y no se incorporan a ella ni reciben recomendaciones retroactivas.
  # - CA15: si las recomendaciones se guardan pero falla la confirmación en scoring-api, la evaluación sigue
  #   PENDIENTE y el reintento obtiene la generación ya guardada (sin regenerarla), tras lo cual se completa.
  # - Decisiones aprobadas para el diseño (2026-10-18): outbox trabajo_recomendacion en scoring-api (CA14,
  #   CA16 y CA17); barrera contra reintentos desfasados en la aplicación; catálogo en un archivo JSON
  #   versionado que solo puede usarse con estado APROBADO; la saga no se activa en bases compartidas sin un
  #   catálogo aprobado; S1 como mecanismo preferido de identidad de servicio.
  #
  # Decisiones pendientes de aprobación (no se inventa una regla):
  # - P-HU14-01. Contenido del catálogo (códigos, textos, criterios de activación y versión). El borrador
  #   existente no está aprobado por el negocio.
  # - P-HU14-02. Definición de factor desfavorable (CA02). Borrador: condición cumplida con contribución
  #   negativa; una condición incumplida no es por sí misma un factor desfavorable.
  # - P-HU14-03. Respuesta cuando la evaluación está PENDIENTE: propuesta 409 con el código
  #   EVALUACION_PENDIENTE.
  # - P-HU14-04. Respuesta para evaluaciones anteriores a la saga: propuesta 200 con lista vacía y
  #   generadas: false.
  # - P-HU14-05. Rutas: propuestas GET /api/v1/evaluaciones/{idEvaluacion}/recomendaciones (consulta) y
  #   PUT /internal/v1/evaluaciones/{idEvaluacion}/recomendaciones (generación interna, idempotente).
  #   Implementadas tal como se proponen.
  # - P-HU14-06. Persistencia en evaluation-query-api (esquema propio, cabecera de generación y
  #   recomendaciones) y dependencias de acceso a datos: pendientes de justificación y aprobación (CA17).
  # - P-HU14-07. Valores de la identidad de servicio y de la política de reintentos: aprobados el 2026-10-18 e
  #   implementados.
  # - P-HU14-08. Códigos de error propuestos: EVALUACION_NO_ENCONTRADA (404) y EVALUACION_PENDIENTE (409),
  #   sujetos a verificar su compatibilidad con el contrato común de errores.
  #
  # Limitaciones técnicas conocidas (2026-10-18): no está demostrado que un rol de aplicación sin propiedad de
  # las tablas no pueda reactivar directamente un trabajo de saga; la prueba del límite total de las llamadas
  # HTTP de la saga está aplazada; las evaluaciones anteriores a V8 no admiten ninguna actualización (V8).

  Escenario: Generación durante la evaluación (CA01, CA02, CA03, CA04, CA09)
    Dado que existe un catálogo de recomendaciones APROBADO con una versión
    Cuando un analista registra una evaluación en scoring-api
    Entonces la evaluación se registra PENDIENTE con su clasificación persistida
    Y evaluation-query-api genera las recomendaciones a partir de esa clasificación y de los factores desfavorables persistidos
    Y las recomendaciones quedan persistidas asociadas al identificador de la evaluación
    Y no se recalculan el puntaje ni la clasificación

  Escenario: La consulta no genera recomendaciones (CA01, CA07)
    Dado una evaluación COMPLETADA con recomendaciones persistidas
    Cuando un usuario autorizado consulta sus recomendaciones
    Entonces obtiene las recomendaciones persistidas
    Y no se genera ni se modifica ninguna recomendación

  Escenario: Conservación del texto histórico (CA05)
    Dado una evaluación cuyas recomendaciones se generaron con la versión 1 del catálogo
    Cuando se aprueba una versión 2 del catálogo con otros textos
    Entonces la consulta devuelve el código, el texto y la versión con que se generaron originalmente

  Escenario: Reintentos sin duplicar ni sobrescribir (CA06, CA14)
    Dado una evaluación cuyas recomendaciones ya fueron generadas
    Cuando la saga reintenta la generación, aunque el catálogo haya cambiado de versión
    Entonces se devuelve la generación existente
    Y no se duplican ni se sobrescriben las recomendaciones

  Escenario: Generación sin recomendaciones aplicables (CA04, CA06)
    # El catálogo puede no tener entradas aplicables a una evaluación (por ejemplo, sin factores desfavorables).
    Dado una evaluación para la que el catálogo no tiene recomendaciones aplicables
    Cuando se generan sus recomendaciones
    Entonces queda registrada una generación completada con cero recomendaciones
    Y un reintento no la vuelve a generar

  Escenario: Evaluación inexistente (CA08)
    # Código de error propuesto (P-HU14-08).
    Cuando un usuario autorizado consulta las recomendaciones de una evaluación que no existe
    Entonces la respuesta es 404 con el código EVALUACION_NO_ENCONTRADA

  Esquema del escenario: Acceso por rol (CA11, HU10 CA04, CA06 y CA07)
    Cuando <usuario> consulta las recomendaciones de una evaluación
    Entonces la respuesta es <respuesta>

    Ejemplos:
      | usuario                                           | respuesta                     |
      | un usuario con rol ADMINISTRADOR_RIESGO           | 200                           |
      | un usuario con rol ANALISTA_CREDITO               | 200                           |
      | una petición sin token                            | 401 con NO_AUTENTICADO        |
      | un token con un rol desconocido                   | 403 con ACCESO_DENEGADO       |

  Escenario: La generación interna exige identidad de servicio (CA11, HU10)
    Cuando un usuario con cualquier rol llama a la operación interna de generación con su token de usuario
    Entonces la operación se rechaza y no se genera ninguna recomendación

  Escenario: Las recomendaciones no son una decisión de crédito (CA10)
    Cuando un usuario autorizado consulta las recomendaciones de una evaluación
    Entonces la respuesta no contiene ningún campo de aprobación ni de rechazo del crédito

  Escenario: Tiempo de respuesta (CA12)
    Dado condiciones normales de operación
    Cuando un usuario autorizado consulta las recomendaciones de una evaluación
    Entonces la respuesta se entrega en un máximo de dos segundos

  Escenario: No se presenta completada sin recomendaciones persistidas (CA13)
    Dado una evaluación PENDIENTE cuyas recomendaciones todavía no están persistidas
    Entonces la evaluación no puede pasar a COMPLETADA
    Y no se presenta como una evaluación completa

  Escenario: Fallo después de guardar las recomendaciones (CA15)
    Dado que evaluation-query-api guardó las recomendaciones de una evaluación
    Y falló la confirmación final en scoring-api
    Entonces la evaluación sigue PENDIENTE
    Cuando la saga reintenta la operación
    Entonces obtiene la generación ya guardada sin regenerarla
    Y la evaluación pasa a COMPLETADA

  Escenario: Reanudación después de un fallo o reinicio (CA14, CA16)
    Dado una evaluación PENDIENTE cuya generación no terminó por un fallo o un reinicio
    Cuando el proceso de la saga vuelve a ejecutarse
    Entonces dispone de la información necesaria para reanudar la operación
    Y la evaluación termina COMPLETADA o queda marcada para intervención operativa, nunca COMPLETADA sin recomendaciones

  Escenario: Evaluaciones anteriores a la saga (regla de la saga)
    Dado una evaluación registrada antes de incorporar la saga
    Entonces conserva su estado nulo
    Y no se incorpora a la saga ni recibe recomendaciones retroactivas

  @pendiente-aprobacion
  Escenario: Consulta de una evaluación PENDIENTE (P-HU14-03)
    Dado una evaluación PENDIENTE
    Cuando un usuario autorizado consulta sus recomendaciones
    Entonces la respuesta es 409 con el código EVALUACION_PENDIENTE

  @pendiente-aprobacion
  Escenario: Consulta de una evaluación anterior a la saga (P-HU14-04)
    Dado una evaluación registrada antes de incorporar la saga
    Cuando un usuario autorizado consulta sus recomendaciones
    Entonces la respuesta es 200 con una lista vacía y la indicación de que no se generaron recomendaciones

  Escenario: Justificación de tablas, dependencias e infraestructura (CA17)
    Entonces cada tabla, dependencia o componente de infraestructura nuevo tiene una justificación documentada y aprobada antes de agregarse
    Y no se incorpora infraestructura de mensajería externa
