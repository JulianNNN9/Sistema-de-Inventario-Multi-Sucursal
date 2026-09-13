import { useState } from 'react';
import { Truck } from 'lucide-react';
import { Badge, DataTable, EmptyState, Input, PageHeader, Select, type Column } from '../../../shared/components/ui';
import { ErrorAlert } from '../../../shared/components/ErrorAlert';
import { useAuth } from '../../auth/hooks/useAuth';
import { useBranches } from '../../sucursales/hooks/useBranches';
import { useComplianceReport } from '../hooks/useComplianceReport';
import { formatNumber } from '../../../shared/lib/format';
import type { ComplianceReportRow } from '../types/logistica';

function formatHoras(horas: number | null): string {
  if (horas === null) return '—';
  const signo = horas > 0 ? '+' : '';
  return `${signo}${formatNumber(horas)} h`;
}

function desviacionTone(horas: number | null): 'neutral' | 'success' | 'warning' | 'danger' {
  if (horas === null) return 'neutral';
  if (horas <= 0) return 'success';
  if (horas <= 24) return 'warning';
  return 'danger';
}

export function LogisticaPage() {
  const { rol } = useAuth();
  const isAdmin = rol === 'ADMIN_GENERAL';
  const { branches } = useBranches();
  const [branchId, setBranchId] = useState('');
  const [route, setRoute] = useState('');

  const { data, loading, error } = useComplianceReport({
    branchId: branchId ? Number(branchId) : undefined,
    route: route.trim() || undefined,
  });

  const columns: Column<ComplianceReportRow>[] = [
    { key: 'origen', header: 'Sucursal origen', render: (r) => r.sucursalOrigenNombre },
    { key: 'transportista', header: 'Transportista', render: (r) => r.transportista },
    {
      key: 'cantidad',
      header: 'Transferencias',
      align: 'right',
      render: (r) => formatNumber(r.cantidadTransferencias),
    },
    {
      key: 'desviacion',
      header: 'Desviación promedio',
      align: 'right',
      render: (r) => (
        <Badge tone={desviacionTone(r.desviacionPromedioHoras)}>
          {formatHoras(r.desviacionPromedioHoras)}
        </Badge>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Logística"
        description="Cumplimiento de tiempos de entrega por ruta (sucursal de origen + transportista). Positivo = tardanza promedio, negativo = adelanto promedio."
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {isAdmin && (
          <Select
            label="Sucursal de origen"
            value={branchId}
            onChange={(e) => setBranchId(e.target.value)}
            options={branches.map((b) => ({ value: b.id, label: b.nombre }))}
            placeholder="Todas"
          />
        )}
        <Input
          label="Transportista"
          value={route}
          onChange={(e) => setRoute(e.target.value)}
          placeholder="Nombre exacto del transportista"
        />
      </div>

      {error && <ErrorAlert message={error} />}

      <DataTable
        columns={columns}
        rows={data}
        rowKey={(r) => `${r.sucursalOrigenId}-${r.transportista}`}
        loading={loading}
        empty={
          <EmptyState
            icon={Truck}
            title="Sin datos de cumplimiento"
            description="No hay transferencias despachadas que coincidan con estos filtros."
          />
        }
      />
    </div>
  );
}
