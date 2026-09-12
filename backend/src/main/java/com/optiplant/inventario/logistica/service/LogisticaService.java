package com.optiplant.inventario.logistica.service;

import com.optiplant.inventario.logistica.dto.ComplianceReportResponse;
import com.optiplant.inventario.security.CurrentUser;
import com.optiplant.inventario.transferencia.repository.TransferenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reportes de logística (RF-25). No crea tablas: se apoya en {@code transferencia} (Módulo 4). */
@Service
@RequiredArgsConstructor
public class LogisticaService {

    private final TransferenciaRepository transferenciaRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<ComplianceReportResponse> complianceReport(Long branchIdParam, String route) {
        Long branchId = currentUser.isAdmin() ? branchIdParam : currentUser.sucursalId();
        return transferenciaRepository.complianceReport(branchId, route).stream()
                .map(row -> new ComplianceReportResponse(
                        row.getSucursalOrigenId(),
                        row.getSucursalOrigenNombre(),
                        row.getTransportista(),
                        row.getCantidad(),
                        row.getDesviacionPromedioHoras()))
                .toList();
    }
}
