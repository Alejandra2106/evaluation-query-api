package com.crediticio.evaluationquery.explicacion.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Explicación de una evaluación ya registrada (HU13), construida solo con datos persistidos: no vuelve a
 * evaluar condiciones ni aplica las reglas actuales (CA07).
 *
 * CA06: si la evaluación tiene puntaje total almacenado, la suma de las contribuciones debe coincidir con él;
 * si no coincide, o si la clasificación persistida está incompleta, la explicación es inconsistente
 * (P-HU13-05). Las evaluaciones registradas antes de HU12 no tienen puntaje ni clasificación persistidos: se
 * muestran sin clasificación y con el puntaje derivado de sus detalles (P-HU13-04).
 *
 * El estado es el de la saga de HU14: PENDIENTE, COMPLETADA o null en las evaluaciones anteriores a la saga.
 * Una evaluación PENDIENTE no se presenta como completa (P-HU13-02).
 */
public final class ExplicacionEvaluacion {

    static final String ESTADO_PENDIENTE = "PENDIENTE";

    private final long idEvaluacion;
    private final LocalDateTime fechaEvaluacion;
    private final Integer scoreTotalPersistido;
    private final String categoria;
    private final Integer versionPolitica;
    private final String estado;
    private final List<FactorAplicado> factores;

    private ExplicacionEvaluacion(long idEvaluacion, LocalDateTime fechaEvaluacion, Integer scoreTotalPersistido,
            String categoria, Integer versionPolitica, String estado, List<FactorAplicado> factores) {
        this.idEvaluacion = idEvaluacion;
        this.fechaEvaluacion = fechaEvaluacion;
        this.scoreTotalPersistido = scoreTotalPersistido;
        this.categoria = categoria;
        this.versionPolitica = versionPolitica;
        this.estado = estado;
        this.factores = factores;
    }

    /**
     * @param scoreTotalPersistido null solo en evaluaciones anteriores a HU12
     * @param categoria            null solo en evaluaciones anteriores a HU12
     * @param versionPolitica      null solo en evaluaciones anteriores a HU12
     * @param estado               estado de la saga; null en evaluaciones anteriores a ella
     */
    public static ExplicacionEvaluacion reconstruir(long idEvaluacion, LocalDateTime fechaEvaluacion,
            Integer scoreTotalPersistido, String categoria, Integer versionPolitica, String estado,
            List<FactorAplicado> factores) {
        List<FactorAplicado> copia = List.copyOf(Objects.requireNonNull(factores, "factores es obligatorio"));
        if ((scoreTotalPersistido == null) != (categoria == null) || (categoria == null) != (versionPolitica == null)) {
            throw new ExplicacionInconsistenteException(idEvaluacion, "la clasificación persistida está incompleta");
        }
        int suma = sumar(copia);
        if (scoreTotalPersistido != null && scoreTotalPersistido != suma) {
            throw new ExplicacionInconsistenteException(idEvaluacion, "la suma de las contribuciones (" + suma
                    + ") no coincide con el puntaje total almacenado (" + scoreTotalPersistido + ")");
        }
        return new ExplicacionEvaluacion(idEvaluacion, fechaEvaluacion, scoreTotalPersistido, categoria,
                versionPolitica, estado, copia);
    }

    private static int sumar(List<FactorAplicado> factores) {
        return factores.stream().mapToInt(FactorAplicado::contribucion).sum();
    }

    /** El puntaje almacenado o, en evaluaciones anteriores a HU12, la suma de las contribuciones. */
    public int getScoreTotal() {
        return scoreTotalPersistido != null ? scoreTotalPersistido : sumar(factores);
    }

    public OrigenPuntaje getOrigenPuntaje() {
        return scoreTotalPersistido != null ? OrigenPuntaje.PERSISTIDO : OrigenPuntaje.DERIVADO_DE_DETALLES;
    }

    public boolean isPendiente() {
        return ESTADO_PENDIENTE.equals(estado);
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

    public List<FactorAplicado> getFactores() {
        return factores;
    }
}
