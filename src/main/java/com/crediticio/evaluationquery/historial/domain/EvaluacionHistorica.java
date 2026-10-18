package com.crediticio.evaluationquery.historial.domain;

import java.time.LocalDateTime;

/**
 * Una evaluación del historial de un solicitante (HU15 CA05), con sus resultados persistidos, sin recalcular
 * (CA07). Las evaluaciones anteriores a HU12 no tienen puntaje ni clasificación persistidos: se muestran sin
 * clasificación y con la suma de sus detalles como puntaje derivado (P-HU15-01).
 */
public final class EvaluacionHistorica {

    private final long idEvaluacion;
    private final LocalDateTime fechaEvaluacion;
    private final Integer scoreTotalPersistido;
    private final int sumaDetalles;
    private final String categoria;
    private final Integer versionPolitica;

    private EvaluacionHistorica(long idEvaluacion, LocalDateTime fechaEvaluacion, Integer scoreTotalPersistido,
            int sumaDetalles, String categoria, Integer versionPolitica) {
        this.idEvaluacion = idEvaluacion;
        this.fechaEvaluacion = fechaEvaluacion;
        this.scoreTotalPersistido = scoreTotalPersistido;
        this.sumaDetalles = sumaDetalles;
        this.categoria = categoria;
        this.versionPolitica = versionPolitica;
    }

    /**
     * @param scoreTotalPersistido null solo en evaluaciones anteriores a HU12
     * @param sumaDetalles         suma de puntaje_obtenido de sus detalles (0 si no tiene)
     */
    public static EvaluacionHistorica reconstruir(long idEvaluacion, LocalDateTime fechaEvaluacion,
            Integer scoreTotalPersistido, int sumaDetalles, String categoria, Integer versionPolitica) {
        if ((scoreTotalPersistido == null) != (categoria == null) || (categoria == null) != (versionPolitica == null)) {
            throw new IllegalStateException("La evaluación " + idEvaluacion
                    + " tiene una clasificación persistida incompleta");
        }
        return new EvaluacionHistorica(idEvaluacion, fechaEvaluacion, scoreTotalPersistido, sumaDetalles, categoria,
                versionPolitica);
    }

    public int getScoreTotal() {
        return scoreTotalPersistido != null ? scoreTotalPersistido : sumaDetalles;
    }

    public OrigenPuntaje getOrigenPuntaje() {
        return scoreTotalPersistido != null ? OrigenPuntaje.PERSISTIDO : OrigenPuntaje.DERIVADO_DE_DETALLES;
    }

    public boolean isClasificada() {
        return categoria != null;
    }

    public long getIdEvaluacion() {
        return idEvaluacion;
    }

    public LocalDateTime getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    /** Null solo en evaluaciones anteriores a HU12. */
    public String getCategoria() {
        return categoria;
    }

    /** Null solo en evaluaciones anteriores a HU12. */
    public Integer getVersionPolitica() {
        return versionPolitica;
    }
}
