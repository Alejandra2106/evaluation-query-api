package com.crediticio.evaluationquery.shared.exception;

import com.crediticio.evaluationquery.explicacion.domain.EvaluacionNoEncontradaException;
import com.crediticio.evaluationquery.explicacion.domain.ExplicacionInconsistenteException;
import com.crediticio.evaluationquery.historial.domain.SolicitanteNoEncontradoException;
import com.crediticio.evaluationquery.shared.response.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Errores de negocio con el formato ApiError y el traceId de X-Trace-Id (contrato común de las tres API).
 * Los códigos EVALUACION_NO_ENCONTRADA y SOLICITANTE_NO_ENCONTRADO son los de scoring-api (P-HU13-07,
 * P-HU15-05). Las respuestas 401 y 403 las escribe RespuestaSeguridadJson.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EvaluacionNoEncontradaException.class)
    public ResponseEntity<ApiError> manejarEvaluacionNoEncontrada(EvaluacionNoEncontradaException ex) {
        log.warn("Consulta de una evaluación que no existe");
        return construirRespuesta(HttpStatus.NOT_FOUND, "EVALUACION_NO_ENCONTRADA", ex.getMessage());
    }

    @ExceptionHandler(SolicitanteNoEncontradoException.class)
    public ResponseEntity<ApiError> manejarSolicitanteNoEncontrado(SolicitanteNoEncontradoException ex) {
        log.warn("Consulta del historial de un solicitante que no existe");
        return construirRespuesta(HttpStatus.NOT_FOUND, "SOLICITANTE_NO_ENCONTRADO", ex.getMessage());
    }

    @ExceptionHandler(IdentificadorInvalidoException.class)
    public ResponseEntity<ApiError> manejarIdentificadorInvalido(IdentificadorInvalidoException ex) {
        log.warn("Solicitud rechazada por un identificador no positivo");
        return construirRespuesta(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> manejarTipoDeArgumentoInvalido(MethodArgumentTypeMismatchException ex) {
        log.warn("Solicitud rechazada por un parámetro con formato inválido");
        return construirRespuesta(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "El parámetro '" + ex.getName() + "' tiene un formato inválido");
    }

    /** P-HU13-05: datos persistidos incoherentes; se registran y no se corrigen ni se ocultan. */
    @ExceptionHandler(ExplicacionInconsistenteException.class)
    public ResponseEntity<ApiError> manejarExplicacionInconsistente(ExplicacionInconsistenteException ex) {
        log.error(ex.getMessage());
        return errorInterno();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarErrorInterno(Exception ex) {
        log.error("Error interno no controlado", ex);
        return errorInterno();
    }

    private static ResponseEntity<ApiError> errorInterno() {
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error interno. Intente nuevamente más tarde");
    }

    private static ResponseEntity<ApiError> construirRespuesta(HttpStatus status, String errorCode, String message) {
        return ResponseEntity.status(status).body(new ApiError(errorCode, message, List.of(), MDC.get("traceId")));
    }
}
