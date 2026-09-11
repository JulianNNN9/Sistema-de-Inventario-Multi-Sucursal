package com.optiplant.inventario.rebalanceo.strategy;

import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.rebalanceo.dto.Sugerencia;
import com.optiplant.inventario.transferencia.entity.Urgencia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Única implementación de {@link RebalanceoStrategy} (RF-31, apartado 8.2).
 * Para cada producto: separa las sucursales candidatas en déficit
 * ({@code cantidad_actual < stock_minimo}) y superávit
 * ({@code cantidad_actual > stock_minimo * 1.5}), y empareja cada déficit —
 * el de mayor magnitud primero — con el superávit de mayor excedente
 * <em>disponible</em> en ese momento (el excedente se consume a medida que se
 * asigna, para no ofrecer el mismo stock de una sucursal a dos destinos
 * distintos). Un déficit sin superávit disponible con qué cubrirlo
 * simplemente no genera sugerencia (el roadmap no contempla dividir un
 * déficit entre varios orígenes).
 */
@Component
@RequiredArgsConstructor
public class DeficitSuperavitStrategy implements RebalanceoStrategy {

    /** Umbral fijo documentado en RF-31: 1.5× el mínimo propio. */
    private static final BigDecimal UMBRAL_SUPERAVIT = new BigDecimal("1.5");

    private final InventarioSucursalRepository inventarioSucursalRepository;

    @Override
    public List<Sugerencia> calcular() {
        List<InventarioSucursal> candidatos = inventarioSucursalRepository.findCandidatosRebalanceo(UMBRAL_SUPERAVIT);

        Map<Long, List<InventarioSucursal>> porProducto = candidatos.stream()
                .collect(Collectors.groupingBy(inv -> inv.getProducto().getId(), LinkedHashMap::new, Collectors.toList()));

        List<Sugerencia> sugerencias = new ArrayList<>();
        porProducto.values().forEach(grupo -> sugerencias.addAll(emparejarProducto(grupo)));

        return sugerencias.stream()
                .sorted(Comparator.comparing(s -> s.urgencia() == Urgencia.ALTA ? 0 : 1))
                .toList();
    }

    private List<Sugerencia> emparejarProducto(List<InventarioSucursal> grupo) {
        List<InventarioSucursal> deficits = grupo.stream()
                .filter(this::esDeficit)
                .sorted(Comparator.comparing(this::magnitudDeficit).reversed())
                .toList();
        List<InventarioSucursal> superavits = grupo.stream()
                .filter(this::esSuperavit)
                .toList();

        if (deficits.isEmpty() || superavits.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> excedenteDisponible = superavits.stream()
                .collect(Collectors.toMap(inv -> inv.getSucursal().getId(), this::excedente, (a, b) -> a, LinkedHashMap::new));

        List<Sugerencia> resultado = new ArrayList<>();
        for (InventarioSucursal deficit : deficits) {
            InventarioSucursal origen = superavits.stream()
                    .filter(s -> excedenteDisponible.get(s.getSucursal().getId()).signum() > 0)
                    .max(Comparator.comparing(s -> excedenteDisponible.get(s.getSucursal().getId())))
                    .orElse(null);
            if (origen == null) {
                continue;
            }

            Long origenId = origen.getSucursal().getId();
            BigDecimal cantidadSugerida = magnitudDeficit(deficit).min(excedenteDisponible.get(origenId));
            excedenteDisponible.merge(origenId, cantidadSugerida, BigDecimal::subtract);

            resultado.add(new Sugerencia(
                    deficit.getProducto().getId(), deficit.getProducto().getSku(), deficit.getProducto().getNombre(),
                    cantidadSugerida,
                    origenId, origen.getSucursal().getNombre(),
                    deficit.getSucursal().getId(), deficit.getSucursal().getNombre(),
                    urgenciaPara(deficit)));
        }
        return resultado;
    }

    private boolean esDeficit(InventarioSucursal inv) {
        return inv.getCantidadActual().compareTo(inv.getStockMinimo()) < 0;
    }

    private boolean esSuperavit(InventarioSucursal inv) {
        return inv.getCantidadActual().compareTo(inv.getStockMinimo().multiply(UMBRAL_SUPERAVIT)) > 0;
    }

    private BigDecimal magnitudDeficit(InventarioSucursal inv) {
        return inv.getStockMinimo().subtract(inv.getCantidadActual());
    }

    /** Lo que la sucursal puede ceder sin caer por debajo de su propio mínimo. */
    private BigDecimal excedente(InventarioSucursal inv) {
        return inv.getCantidadActual().subtract(inv.getStockMinimo());
    }

    private Urgencia urgenciaPara(InventarioSucursal deficit) {
        return deficit.getCantidadActual().signum() == 0 ? Urgencia.ALTA : Urgencia.MEDIA;
    }
}
