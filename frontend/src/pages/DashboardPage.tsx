import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  AlertTriangle,
  ArrowLeftRight,
  Boxes,
  PackageMinus,
  PackagePlus,
  Shuffle,
  TrendingUp,
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { approveRebalanceSuggestion } from '../api/rebalanceo';
import { Badge, Button, Card, EmptyState, PageHeader, Select, Skeleton } from '../components/ui';
import { ErrorAlert } from '../components/ErrorAlert';
import { useAuth } from '../hooks/useAuth';
import { useBranches } from '../hooks/useBranches';
import { useDashboard } from '../hooks/useDashboard';
import { useMutation } from '../hooks/useMutation';
import { useRebalanceSuggestions } from '../hooks/useRebalanceSuggestions';
import { formatCurrency, formatNumber } from '../lib/format';
import type { EstadoTransferenciaActivo, InventoryRotationItem } from '../types/dashboard';
import type { Sugerencia } from '../types/rebalanceo';

const ESTADO_LABEL: Record<EstadoTransferenciaActivo, string> = {
  PENDIENTE: 'Pendientes',
  EN_TRANSITO: 'En tránsito',
  CON_FALTANTES: 'Con faltantes',
  REENVIO_SOLICITADO: 'Reenvío solicitado',
};

const ESTADO_TONE: Record<EstadoTransferenciaActivo, 'warning' | 'info'> = {
  PENDIENTE: 'warning',
  EN_TRANSITO: 'info',
  CON_FALTANTES: 'warning',
  REENVIO_SOLICITADO: 'info',
};

function formatPeriodo(periodo: string): string {
  const fecha = new Date(`${periodo}-01T00:00:00Z`);
  if (Number.isNaN(fecha.getTime())) return periodo;
  return new Intl.DateTimeFormat('es-CO', { month: 'short', year: 'numeric', timeZone: 'UTC' }).format(fecha);
}

export function DashboardPage() {
  const { usuario, rol } = useAuth();
  const isAdmin = rol === 'ADMIN_GENERAL';
  const { branches } = useBranches();

  const [filterBranch, setFilterBranch] = useState('');
  const branchId = filterBranch ? Number(filterBranch) : undefined;

  const { data, loading, error, refetch } = useDashboard({ branchId, includeBranchComparison: isAdmin });

  const chartData = useMemo(
    () =>
      (data?.sales ?? []).map((p) => ({
        periodo: formatPeriodo(p.periodo),
        totalVentas: p.totalVentas,
      })),
    [data?.sales],
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Hola, ${usuario ?? ''}`.trim()}
        description="Resumen operativo: ventas, rotación de inventario, transferencias activas y alertas de reabastecimiento (RF-26..RF-30)."
        actions={
          isAdmin ? (
            <Select
              value={filterBranch}
              onChange={(e) => setFilterBranch(e.target.value)}
              options={branches.map((b) => ({ value: b.id, label: b.nombre }))}
              placeholder="Todas las sucursales"
              className="w-56"
            />
          ) : undefined
        }
      />

      {error && <ErrorAlert message={error} />}

      {isAdmin && <RebalancePanel onApproved={refetch} />}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold text-slate-900">Comparativa de ventas</h2>
              <p className="text-xs text-slate-500">Mes actual vs. los 3 meses anteriores (RF-26)</p>
            </div>
            <TrendingUp className="h-4 w-4 text-slate-400" aria-hidden />
          </div>
          {loading ? (
            <Skeleton className="h-64 w-full" />
          ) : (
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ left: 0, right: 8, top: 4, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="periodo" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
                  <YAxis
                    tick={{ fontSize: 12, fill: '#64748b' }}
                    axisLine={false}
                    tickLine={false}
                    width={70}
                    tickFormatter={(v: number) => formatCurrency(v)}
                  />
                  <Tooltip
                    formatter={(value: number) => formatCurrency(value)}
                    contentStyle={{ borderRadius: 12, borderColor: '#e2e8f0', fontSize: 13 }}
                  />
                  <Bar dataKey="totalVentas" name="Ventas" fill="#0d9488" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <Card>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold text-slate-900">Transferencias activas</h2>
              <p className="text-xs text-slate-500">Por estado no terminal (RF-28)</p>
            </div>
            <ArrowLeftRight className="h-4 w-4 text-slate-400" aria-hidden />
          </div>
          {loading ? (
            <div className="space-y-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-8 w-full" />
              ))}
            </div>
          ) : (data?.activeTransfers ?? []).length === 0 ? (
            <EmptyState icon={ArrowLeftRight} title="Sin transferencias activas" />
          ) : (
            <ul className="space-y-2.5">
              {(data?.activeTransfers ?? []).map((item) => (
                <li key={item.estado} className="flex items-center justify-between">
                  <Badge tone={ESTADO_TONE[item.estado]}>{ESTADO_LABEL[item.estado]}</Badge>
                  <span className="text-sm font-semibold text-slate-900">{formatNumber(item.cantidad)}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <RotationCard
          title="Mayor rotación"
          description="Top 5 productos con más unidades retiradas (30 días)"
          icon={PackageMinus}
          items={data?.rotation.mayorRotacion}
          loading={loading}
        />
        <RotationCard
          title="Menor rotación"
          description="Top 5 productos con menos unidades retiradas (30 días)"
          icon={PackagePlus}
          items={data?.rotation.menorRotacion}
          loading={loading}
        />
      </div>

      <Card>
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold text-slate-900">Alertas de reabastecimiento</h2>
            <p className="text-xs text-slate-500">Productos en el mínimo o por debajo (RF-29)</p>
          </div>
          <AlertTriangle className="h-4 w-4 text-slate-400" aria-hidden />
        </div>
        {loading ? (
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : (data?.restockAlerts ?? []).length === 0 ? (
          <EmptyState icon={AlertTriangle} title="Sin alertas" description="Ningún producto está por debajo de su stock mínimo." />
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {(data?.restockAlerts ?? []).map((alert) => (
              <div
                key={`${alert.productId}-${alert.sucursalId}`}
                className="flex items-start justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 px-3.5 py-2.5"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-slate-900">{alert.productoNombre}</p>
                  <p className="text-xs text-slate-500">
                    {alert.sku} · {alert.sucursalNombre}
                  </p>
                </div>
                <Badge tone="danger" className="shrink-0">
                  {formatNumber(alert.cantidadActual)} / {formatNumber(alert.stockMinimo)}
                </Badge>
              </div>
            ))}
          </div>
        )}
      </Card>

      {isAdmin && (
        <Card>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold text-slate-900">Comparativa entre sucursales</h2>
              <p className="text-xs text-slate-500">Ventas del mes y rotación de 30 días (RF-30)</p>
            </div>
            <Boxes className="h-4 w-4 text-slate-400" aria-hidden />
          </div>
          {loading ? (
            <Skeleton className="h-32 w-full" />
          ) : (data?.branchComparison ?? []).length === 0 ? (
            <EmptyState icon={Boxes} title="Sin datos" />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[420px] text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-xs font-medium uppercase tracking-wide text-slate-500">
                    <th className="px-3 py-2 text-left">Sucursal</th>
                    <th className="px-3 py-2 text-right">Ventas (mes actual)</th>
                    <th className="px-3 py-2 text-right">Rotación (30 días)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {(data?.branchComparison ?? []).map((row) => (
                    <tr key={row.sucursalId}>
                      <td className="px-3 py-2.5 font-medium text-slate-900">{row.sucursalNombre}</td>
                      <td className="px-3 py-2.5 text-right text-slate-700">{formatCurrency(row.ventasTotales)}</td>
                      <td className="px-3 py-2.5 text-right text-slate-700">{formatNumber(row.rotacionTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}
    </div>
  );
}

function suggestionKey(s: Sugerencia): string {
  return `${s.productId}-${s.sucursalOrigenId}-${s.sucursalDestinoId}`;
}

interface RebalancePanelProps {
  /** RF-33: tras aprobar, refresca el dashboard (transferencias activas incluidas). */
  onApproved: () => void;
}

/** RF-31..RF-34: panel de sugerencias de rebalanceo, solo ADMIN_GENERAL. */
function RebalancePanel({ onApproved }: RebalancePanelProps) {
  const { data, loading, error, refetch } = useRebalanceSuggestions();
  const { mutate, submitting, error: approveError, resetError } = useMutation(approveRebalanceSuggestion);
  const [target, setTarget] = useState<string | null>(null);

  async function handleApprove(s: Sugerencia) {
    resetError();
    setTarget(suggestionKey(s));
    try {
      await mutate({
        productId: s.productId,
        cantidadSugerida: s.cantidadSugerida,
        sucursalOrigenId: s.sucursalOrigenId,
        sucursalDestinoId: s.sucursalDestinoId,
      });
      refetch();
      onApproved();
    } catch {
      /* error mostrado abajo */
    } finally {
      setTarget(null);
    }
  }

  return (
    <Card>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-sm font-semibold text-slate-900">Sugerencias de rebalanceo</h2>
          <p className="text-xs text-slate-500">Traslados recomendados entre sucursales, por urgencia (RF-31)</p>
        </div>
        <Shuffle className="h-4 w-4 text-slate-400" aria-hidden />
      </div>
      {error && <ErrorAlert message={error} className="mb-3" />}
      {approveError && <ErrorAlert message={approveError} className="mb-3" />}
      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 2 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full" />
          ))}
        </div>
      ) : data.length === 0 ? (
        <EmptyState
          icon={Shuffle}
          title="Sin sugerencias"
          description="No hay traslados recomendados con los niveles de stock actuales."
        />
      ) : (
        <ul className="space-y-2.5">
          {data.map((s) => {
            const key = suggestionKey(s);
            return (
              <li
                key={key}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 px-3.5 py-2.5"
              >
                <div className="min-w-0">
                  <p className="text-sm font-medium text-slate-900">
                    {s.sku} · {s.productoNombre}
                  </p>
                  <p className="text-xs text-slate-500">
                    {s.sucursalOrigenNombre} → {s.sucursalDestinoNombre} · {formatNumber(s.cantidadSugerida)} u.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge tone={s.urgencia === 'ALTA' ? 'danger' : 'info'}>{s.urgencia}</Badge>
                  <Button
                    size="sm"
                    onClick={() => handleApprove(s)}
                    loading={submitting && target === key}
                    disabled={submitting}
                  >
                    Aprobar
                  </Button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}

interface RotationCardProps {
  title: string;
  description: string;
  icon: typeof PackageMinus;
  items: InventoryRotationItem[] | undefined;
  loading: boolean;
}

function RotationCard({ title, description, icon: Icon, items, loading }: RotationCardProps) {
  return (
    <Card>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-sm font-semibold text-slate-900">{title}</h2>
          <p className="text-xs text-slate-500">{description}</p>
        </div>
        <Icon className="h-4 w-4 text-slate-400" aria-hidden />
      </div>
      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-6 w-full" />
          ))}
        </div>
      ) : !items || items.length === 0 ? (
        <EmptyState icon={Icon} title="Sin movimientos" description="No hay retiros registrados en los últimos 30 días." />
      ) : (
        <ol className="space-y-2.5">
          {items.map((item, index) => (
            <motion.li
              key={item.productId}
              initial={{ opacity: 0, x: -6 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.15, delay: index * 0.03 }}
              className="flex items-center justify-between gap-3"
            >
              <span className="min-w-0 truncate text-sm text-slate-700">
                <span className="mr-2 text-xs font-medium text-slate-400">{index + 1}.</span>
                {item.sku} · {item.nombre}
              </span>
              <span className="shrink-0 text-sm font-semibold text-slate-900">{formatNumber(item.cantidadRetirada)}</span>
            </motion.li>
          ))}
        </ol>
      )}
    </Card>
  );
}
