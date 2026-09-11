package com.optiplant.inventario.dashboard.service;

import com.optiplant.inventario.dashboard.dto.ActiveTransfersCount;
import com.optiplant.inventario.dashboard.dto.BranchComparisonRow;
import com.optiplant.inventario.dashboard.dto.InventoryRotationItem;
import com.optiplant.inventario.dashboard.dto.InventoryRotationResponse;
import com.optiplant.inventario.dashboard.dto.RestockAlert;
import com.optiplant.inventario.dashboard.dto.SalesComparisonPoint;
import com.optiplant.inventario.inventario.entity.InventarioSucursal;
import com.optiplant.inventario.inventario.entity.TipoMovimiento;
import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.inventario.repository.MovimientoInventarioRepository;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.sucursal.repository.SucursalRepository;
import com.optiplant.inventario.transferencia.entity.EstadoTransferencia;
import com.optiplant.inventario.transferencia.repository.TransferenciaRepository;
import com.optiplant.inventario.venta.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes del Dashboard (RF-26..RF-30, Módulo 6). No persiste nada: agrega en
 * BD sobre entidades de módulos anteriores (RNF-01: prohibido traer filas a la
 * JVM y sumar en Java). Cada endpoint scoped por sucursal sigue el mismo
 * criterio que el resto del sistema desde el Módulo 4: no-admin siempre ve su
 * propia sucursal (el {@code branchId} del parámetro se ignora), admin puede
 * filtrar o dejarlo vacío para ver todas.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Sección 3: únicos estados no terminales del ciclo de vida de una transferencia. */
    private static final List<EstadoTransferencia> ESTADOS_NO_TERMINALES = List.of(
            EstadoTransferencia.PENDIENTE, EstadoTransferencia.EN_TRANSITO,
            EstadoTransferencia.CON_FALTANTES, EstadoTransferencia.REENVIO_SOLICITADO);

    private static final int VENTANA_ROTACION_DIAS = 30;
    private static final int MESES_COMPARACION = 4;
    private static final ZoneOffset ZONA = ZoneOffset.UTC;

    private final VentaRepository ventaRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final InventarioSucursalRepository inventarioSucursalRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final SucursalRepository sucursalRepository;
    private final CurrentUser currentUser;

    /** RF-26: total vendido del mes actual vs. cada uno de los 3 meses anteriores. */
    @Transactional(readOnly = true)
    public List<SalesComparisonPoint> salesComparison(Long branchIdParam) {
        Long branchId = resolverSucursal(branchIdParam);
        YearMonth mesActual = YearMonth.now(ZONA);

        List<SalesComparisonPoint> puntos = new ArrayList<>();
        for (int i = MESES_COMPARACION - 1; i >= 0; i--) {
            YearMonth mes = mesActual.minusMonths(i);
            Instant desde = mes.atDay(1).atStartOfDay(ZONA).toInstant();
            Instant hasta = mes.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant();
            var total = ventaRepository.sumTotalEntreFechas(branchId, desde, hasta);
            puntos.add(new SalesComparisonPoint(mes.toString(), total));
        }
        return puntos;
    }

    /** RF-27: top 5 de mayor y de menor rotación (RETIRO, últimos 30 días). */
    @Transactional(readOnly = true)
    public InventoryRotationResponse inventoryRotation(Long branchIdParam) {
        Long branchId = resolverSucursal(branchIdParam);
        Instant desde = Instant.now().minus(VENTANA_ROTACION_DIAS, ChronoUnit.DAYS);
        Pageable top5 = PageRequest.of(0, 5);

        List<InventoryRotationItem> mayor = movimientoInventarioRepository
                .topMayorRotacion(branchId, TipoMovimiento.RETIRO, desde, top5);
        List<InventoryRotationItem> menor = movimientoInventarioRepository
                .topMenorRotacion(branchId, TipoMovimiento.RETIRO, desde, top5);
        return new InventoryRotationResponse(mayor, menor);
    }

    /** RF-28: conteo de transferencias por estado no terminal. */
    @Transactional(readOnly = true)
    public List<ActiveTransfersCount> activeTransfers(Long branchIdParam) {
        Long branchId = resolverSucursal(branchIdParam);
        return transferenciaRepository.countActivasPorEstado(ESTADOS_NO_TERMINALES, branchId);
    }

    /** RF-29: productos con existencia en el mínimo o por debajo. */
    @Transactional(readOnly = true)
    public List<RestockAlert> restockAlerts(Long branchIdParam) {
        Long branchId = resolverSucursal(branchIdParam);
        return inventarioSucursalRepository.restockAlerts(branchId).stream()
                .map(this::toRestockAlert)
                .toList();
    }

    /** RF-30 (solo ADMIN_GENERAL): ventas totales y rotación agregada por sucursal. */
    @Transactional(readOnly = true)
    public List<BranchComparisonRow> branchComparison() {
        YearMonth mesActual = YearMonth.now(ZONA);
        Instant ventasDesde = mesActual.atDay(1).atStartOfDay(ZONA).toInstant();
        Instant ventasHasta = mesActual.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant();
        Instant rotacionDesde = Instant.now().minus(VENTANA_ROTACION_DIAS, ChronoUnit.DAYS);

        return sucursalRepository.comparativaPorSucursal(
                ventasDesde, ventasHasta, TipoMovimiento.RETIRO, rotacionDesde);
    }

    /** No-admin: siempre su propia sucursal, ignorando lo que venga en el parámetro. */
    private Long resolverSucursal(Long branchIdParam) {
        return currentUser.isAdmin() ? branchIdParam : currentUser.sucursalId();
    }

    private RestockAlert toRestockAlert(InventarioSucursal inv) {
        return new RestockAlert(
                inv.getProducto().getId(), inv.getProducto().getSku(), inv.getProducto().getNombre(),
                inv.getSucursal().getId(), inv.getSucursal().getNombre(),
                inv.getCantidadActual(), inv.getStockMinimo());
    }
}
