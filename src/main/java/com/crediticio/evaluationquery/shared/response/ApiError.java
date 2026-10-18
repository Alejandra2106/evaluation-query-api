package com.crediticio.evaluationquery.shared.response;

import java.util.List;

/** Formato común de error de las tres API (HU10): {errorCode, message, details, traceId}. */
public record ApiError(String errorCode, String message, List<String> details, String traceId) {
}
