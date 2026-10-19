package com.crediticio.evaluationquery.recomendaciones.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Respuesta de la operación interna de generación (HU14), con el cuerpo que espera la saga de scoring-api.
 * creada no se serializa: indica al controlador si responder 201 (generada ahora) o 200 (ya existía, un
 * reintento que devuelve la guardada sin duplicarla ni sobrescribirla, CA06).
 */
public record ResultadoGeneracionResponse(long idEvaluacion, String versionCatalogo, int cantidad,
        @JsonIgnore boolean creada) {
}
