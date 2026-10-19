package com.crediticio.evaluationquery.recomendaciones.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo explícito y versionado de recomendaciones (HU14 CA03). Solo un catálogo APROBADO puede generar
 * recomendaciones; su contenido lo aprueba el negocio (P-HU14-01).
 *
 * Selección: como máximo una recomendación por categoría (la de la clasificación persistida) y una por cada
 * variable con factores desfavorables, aunque haya varios sobre la misma variable (reglas solapadas); si hay
 * varias entradas para el mismo criterio, la de código menor. Orden estable: primero la de categoría y después
 * las de variable en orden alfabético. No recalcula puntajes ni clasificaciones (CA09).
 */
public final class CatalogoRecomendaciones {

    private static final int LONGITUD_MAXIMA_VERSION = 30;

    private final String version;
    private final boolean aprobado;
    private final List<EntradaCatalogo> entradas;

    public CatalogoRecomendaciones(String version, boolean aprobado, List<EntradaCatalogo> entradas) {
        if (version == null || version.isBlank() || version.length() > LONGITUD_MAXIMA_VERSION) {
            throw new IllegalArgumentException("La versión del catálogo es obligatoria (hasta 30 caracteres)");
        }
        this.entradas = List.copyOf(Objects.requireNonNull(entradas, "entradas es obligatorio"));
        Set<String> codigos = new HashSet<>();
        for (EntradaCatalogo entrada : this.entradas) {
            if (!codigos.add(entrada.codigo())) {
                throw new IllegalArgumentException("Código repetido en el catálogo: " + entrada.codigo());
            }
        }
        this.version = version;
        this.aprobado = aprobado;
    }

    public List<RecomendacionGenerada> seleccionar(String categoria, List<FactorEvaluado> factores) {
        List<EntradaCatalogo> elegidas = new ArrayList<>();
        entradaPara(TipoActivacion.CATEGORIA, categoria).ifPresent(elegidas::add);
        factores.stream()
                .filter(FactorEvaluado::esDesfavorable)
                .map(FactorEvaluado::variable)
                .distinct()
                .sorted()
                .forEach(variable -> entradaPara(TipoActivacion.FACTOR_DESFAVORABLE, variable).ifPresent(elegidas::add));
        List<RecomendacionGenerada> recomendaciones = new ArrayList<>();
        for (int i = 0; i < elegidas.size(); i++) {
            recomendaciones.add(RecomendacionGenerada.desde(elegidas.get(i), i + 1));
        }
        return recomendaciones;
    }

    private Optional<EntradaCatalogo> entradaPara(TipoActivacion tipo, String valor) {
        return entradas.stream()
                .filter(entrada -> entrada.tipoActivacion() == tipo)
                .filter(entrada -> valor.equals(tipo == TipoActivacion.CATEGORIA ? entrada.categoria() : entrada.variable()))
                .min(Comparator.comparing(EntradaCatalogo::codigo));
    }

    public String getVersion() {
        return version;
    }

    public boolean isAprobado() {
        return aprobado;
    }

    public List<EntradaCatalogo> getEntradas() {
        return entradas;
    }
}
