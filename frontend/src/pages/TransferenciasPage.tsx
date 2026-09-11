import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  ArrowLeftRight,
  Check,
  History,
  PackageCheck,
  Plus,
  RotateCcw,
  Send,
  X,
} from 'lucide-react';
import {
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  EmptyState,
  Input,
  Modal,
  PageHeader,
  Pagination,
  Select,
  type Column,
} from '../components/ui';
import { ErrorAlert } from '../components/ErrorAlert';
import {
  approveTransfer,
  dispatchTransfer,
  receiveTransfer,
  requestTransfer,
  resolveTransfer,
} from '../api/transferencias';
import { useAuth } from '../hooks/useAuth';
import { useBranches } from '../hooks/useBranches';
import { useMutation } from '../hooks/useMutation';
import { useProductos } from '../hooks/useProductos';
import { useTransferEvents } from '../hooks/useTransferEvents';
import { useTransferenciasList } from '../hooks/useTransferenciasList';
import { formatDateTime, formatNumber } from '../lib/format';
import type { TransferSort } from '../types/logistica';
import type {
  EstadoTransferencia,
  Transfer,
  TratamientoFaltante,
  Urgencia,
} from '../types/transferencia';

const PAGE_SIZE = 20;

const SORT_OPTIONS: { value: TransferSort; label: string }[] = [
  { value: 'priority', label: 'Prioridad (urgencia)' },
  { value: 'cost', label: 'Cantidad solicitada' },
  { value: 'time', label: 'Llegada estimada' },
];

const ESTADO_OPTIONS: { value: EstadoTransferencia; label: string }[] = [
  { value: 'PENDIENTE', label: 'Pendiente' },
  { value: 'EN_TRANSITO', label: 'En tránsito' },
  { value: 'COMPLETADA', label: 'Completada' },
  { value: 'CON_FALTANTES', label: 'Con faltantes' },
  { value: 'RECHAZADA', label: 'Rechazada' },
  { value: 'REENVIO_SOLICITADO', label: 'Reenvío solicitado' },
  { value: 'CERRADA_AJUSTE', label: 'Cerrada (ajuste)' },
  { value: 'CERRADA_RECLAMACION', label: 'Cerrada (reclamación)' },
];

const ESTADO_TONE: Record<EstadoTransferencia, 'neutral' | 'success' | 'warning' | 'danger' | 'info'> = {
  PENDIENTE: 'warning',
  RECHAZADA: 'danger',
  EN_TRANSITO: 'info',
  COMPLETADA: 'success',
  CON_FALTANTES: 'warning',
  REENVIO_SOLICITADO: 'info',
  CERRADA_AJUSTE: 'neutral',
  CERRADA_RECLAMACION: 'neutral',
};

const URGENCIA_TONE: Record<Urgencia, 'neutral' | 'danger' | 'info'> = {
  BAJA: 'neutral',
  MEDIA: 'info',
  ALTA: 'danger',
};

export function TransferenciasPage() {
  const { rol, sucursalId } = useAuth();
  const isAdmin = rol === 'ADMIN_GENERAL';
  const puedeSolicitar = rol === 'ADMIN_GENERAL' || rol === 'OPERADOR_INVENTARIO';

  const { branches } = useBranches();

  const [page, setPage] = useState(0);
  const [estado, setEstado] = useState('');
  const [filterBranch, setFilterBranch] = useState('');
  const [sort, setSort] = useState('');

  const { data, loading, error, refetch } = useTransferenciasList({
    page,
    size: PAGE_SIZE,
    estado: (estado || undefined) as EstadoTransferencia | undefined,
    branchId: filterBranch ? Number(filterBranch) : undefined,
    sort: (sort || undefined) as TransferSort | undefined,
  });

  const [requestOpen, setRequestOpen] = useState(false);
  const [approveTarget, setApproveTarget] = useState<{ transfer: Transfer; aprobado: boolean } | null>(null);
  const [dispatchTarget, setDispatchTarget] = useState<Transfer | null>(null);
  const [receiveTarget, setReceiveTarget] = useState<Transfer | null>(null);
  const [resolveTarget, setResolveTarget] = useState<Transfer | null>(null);
  const [eventsId, setEventsId] = useState<number | null>(null);

  const approveM = useMutation(approveTransfer);

  function canApprove(t: Transfer) {
    return rol === 'GERENTE_SUCURSAL' && sucursalId === t.sucursalOrigenId && t.estado === 'PENDIENTE';
  }
  function canDispatch(t: Transfer) {
    return rol === 'OPERADOR_INVENTARIO' && sucursalId === t.sucursalOrigenId && t.estado === 'PENDIENTE';
  }
  function canReceive(t: Transfer) {
    return rol === 'OPERADOR_INVENTARIO' && sucursalId === t.sucursalDestinoId && t.estado === 'EN_TRANSITO';
  }
  function canResolve(t: Transfer) {
    return rol === 'GERENTE_SUCURSAL' && sucursalId === t.sucursalDestinoId && t.estado === 'CON_FALTANTES';
  }

  async function handleApproveConfirm() {
    if (!approveTarget) return;
    try {
      await approveM.mutate(approveTarget.transfer.id, { aprobado: approveTarget.aprobado });
      setApproveTarget(null);
      refetch();
    } catch {
      /* error mostrado en el ConfirmDialog */
    }
  }

  const columns: Column<Transfer>[] = [
    { key: 'id', header: 'N.º', render: (t) => <span className="font-medium text-slate-900">#{t.id}</span> },
    { key: 'producto', header: 'Producto', render: (t) => `${t.sku} · ${t.productoNombre}` },
    { key: 'origen', header: 'Origen', render: (t) => t.sucursalOrigenNombre },
    { key: 'destino', header: 'Destino', render: (t) => t.sucursalDestinoNombre },
    {
      key: 'cantidad',
      header: 'Cant.',
      align: 'right',
      render: (t) => (
        <span title="Solicitada / enviada / recibida">
          {formatNumber(t.cantidadSolicitada)}
          {t.cantidadEnviada !== null && ` / ${formatNumber(t.cantidadEnviada)}`}
          {t.cantidadRecibida !== null && ` / ${formatNumber(t.cantidadRecibida)}`}
        </span>
      ),
    },
    {
      key: 'urgencia',
      header: 'Urgencia',
      render: (t) => <Badge tone={URGENCIA_TONE[t.urgencia]}>{t.urgencia}</Badge>,
    },
    {
      key: 'estado',
      header: 'Estado',
      render: (t) => <Badge tone={ESTADO_TONE[t.estado]}>{t.estado.replace('_', ' ')}</Badge>,
    },
    {
      key: 'acciones',
      header: '',
      align: 'right',
      render: (t) => (
        <div className="flex justify-end gap-1">
          {canApprove(t) && (
            <>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setApproveTarget({ transfer: t, aprobado: true })}
                aria-label={`Aprobar transferencia ${t.id}`}
              >
                <Check className="h-4 w-4 text-emerald-600" aria-hidden />
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setApproveTarget({ transfer: t, aprobado: false })}
                aria-label={`Rechazar transferencia ${t.id}`}
              >
                <X className="h-4 w-4 text-rose-500" aria-hidden />
              </Button>
            </>
          )}
          {canDispatch(t) && (
            <Button size="sm" variant="ghost" onClick={() => setDispatchTarget(t)} aria-label={`Despachar transferencia ${t.id}`}>
              <Send className="h-4 w-4 text-brand-700" aria-hidden />
            </Button>
          )}
          {canReceive(t) && (
            <Button size="sm" variant="ghost" onClick={() => setReceiveTarget(t)} aria-label={`Recibir transferencia ${t.id}`}>
              <PackageCheck className="h-4 w-4 text-brand-700" aria-hidden />
            </Button>
          )}
          {canResolve(t) && (
            <Button size="sm" variant="ghost" onClick={() => setResolveTarget(t)} aria-label={`Resolver faltante ${t.id}`}>
              <RotateCcw className="h-4 w-4 text-amber-600" aria-hidden />
            </Button>
          )}
          <Button size="sm" variant="ghost" onClick={() => setEventsId(t.id)} aria-label={`Historial de la transferencia ${t.id}`}>
            <History className="h-4 w-4" aria-hidden />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Transferencias"
        description="Solicitud, aprobación, despacho, recepción y resolución de faltantes (RF-17..RF-21)."
        actions={
          puedeSolicitar ? (
            <Button onClick={() => setRequestOpen(true)}>
              <Plus className="h-4 w-4" aria-hidden />
              Solicitar transferencia
            </Button>
          ) : undefined
        }
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Select
          label="Estado"
          value={estado}
          onChange={(e) => {
            setEstado(e.target.value);
            setPage(0);
          }}
          options={ESTADO_OPTIONS}
          placeholder="Todos"
        />
        {isAdmin && (
          <Select
            label="Sucursal"
            value={filterBranch}
            onChange={(e) => {
              setFilterBranch(e.target.value);
              setPage(0);
            }}
            options={branches.map((b) => ({ value: b.id, label: b.nombre }))}
            placeholder="Todas"
          />
        )}
        <Select
          label="Ordenar por"
          value={sort}
          onChange={(e) => {
            setSort(e.target.value);
            setPage(0);
          }}
          options={SORT_OPTIONS}
          placeholder="Por defecto"
        />
      </div>

      {error && <ErrorAlert message={error} />}

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(t) => t.id}
        loading={loading}
        empty={
          <EmptyState
            icon={ArrowLeftRight}
            title="Sin transferencias"
            description="No hay transferencias registradas para estos filtros."
          />
        }
      />

      {data && <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />}

      <RequestModal
        open={requestOpen}
        isAdmin={isAdmin}
        branches={branches}
        onClose={() => setRequestOpen(false)}
        onCreated={() => {
          setRequestOpen(false);
          refetch();
        }}
      />

      <ConfirmDialog
        open={approveTarget !== null}
        title={approveTarget?.aprobado ? 'Aprobar transferencia' : 'Rechazar transferencia'}
        message={
          approveTarget?.aprobado
            ? `¿Aprobar la transferencia #${approveTarget?.transfer.id}? Quedará disponible para que origen la despache.`
            : `¿Rechazar la transferencia #${approveTarget?.transfer.id}? Esta acción es definitiva.`
        }
        confirmLabel={approveTarget?.aprobado ? 'Aprobar' : 'Rechazar'}
        loading={approveM.submitting}
        error={approveM.error}
        onConfirm={handleApproveConfirm}
        onCancel={() => {
          setApproveTarget(null);
          approveM.resetError();
        }}
      />

      <DispatchModal
        transfer={dispatchTarget}
        onClose={() => setDispatchTarget(null)}
        onDone={() => {
          setDispatchTarget(null);
          refetch();
        }}
      />

      <ReceiveModal
        transfer={receiveTarget}
        onClose={() => setReceiveTarget(null)}
        onDone={() => {
          setReceiveTarget(null);
          refetch();
        }}
      />

      <ResolveModal
        transfer={resolveTarget}
        onClose={() => setResolveTarget(null)}
        onDone={() => {
          setResolveTarget(null);
          refetch();
        }}
      />

      <EventsModal id={eventsId} onClose={() => setEventsId(null)} />
    </div>
  );
}

interface RequestModalProps {
  open: boolean;
  isAdmin: boolean;
  branches: { id: number; nombre: string }[];
  onClose: () => void;
  onCreated: () => void;
}

function RequestModal({ open, isAdmin, branches, onClose, onCreated }: RequestModalProps) {
  const { data: productosData } = useProductos({ page: 0, size: 300, enabled: open });
  const { mutate, submitting, error, resetError } = useMutation(requestTransfer);

  const [productId, setProductId] = useState('');
  const [cantidad, setCantidad] = useState('');
  const [sucursalOrigenId, setSucursalOrigenId] = useState('');
  const [sucursalDestinoId, setSucursalDestinoId] = useState('');
  const [urgencia, setUrgencia] = useState<Urgencia>('MEDIA');

  const productOptions = useMemo(
    () => (productosData?.content ?? []).map((p) => ({ value: p.id, label: `${p.sku} · ${p.nombre}` })),
    [productosData],
  );

  function reset() {
    setProductId('');
    setCantidad('');
    setSucursalOrigenId('');
    setSucursalDestinoId('');
    setUrgencia('MEDIA');
    resetError();
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      await mutate({
        productId: Number(productId),
        cantidad: Number(cantidad),
        sucursalOrigenId: Number(sucursalOrigenId),
        urgencia,
        sucursalDestinoId: isAdmin && sucursalDestinoId ? Number(sucursalDestinoId) : undefined,
      });
      reset();
      onCreated();
    } catch {
      /* error mostrado */
    }
  }

  return (
    <Modal
      open={open}
      onClose={() => {
        reset();
        onClose();
      }}
      title="Solicitar transferencia"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" form="transfer-request-form" loading={submitting}>
            Solicitar
          </Button>
        </>
      }
    >
      <form id="transfer-request-form" onSubmit={handleSubmit} className="space-y-4">
        {error && <ErrorAlert message={error} />}
        <Select
          label="Producto"
          value={productId}
          onChange={(e) => setProductId(e.target.value)}
          options={productOptions}
          placeholder="Selecciona un producto"
          required
        />
        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Cantidad"
            type="number"
            step="0.01"
            min="0"
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            required
          />
          <Select
            label="Urgencia"
            value={urgencia}
            onChange={(e) => setUrgencia(e.target.value as Urgencia)}
            options={[
              { value: 'BAJA', label: 'Baja' },
              { value: 'MEDIA', label: 'Media' },
              { value: 'ALTA', label: 'Alta' },
            ]}
          />
        </div>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Select
            label="Sucursal origen"
            value={sucursalOrigenId}
            onChange={(e) => setSucursalOrigenId(e.target.value)}
            options={branches.map((b) => ({ value: b.id, label: b.nombre }))}
            placeholder="Selecciona el origen"
            required
          />
          {isAdmin && (
            <Select
              label="Sucursal destino"
              value={sucursalDestinoId}
              onChange={(e) => setSucursalDestinoId(e.target.value)}
              options={branches.map((b) => ({ value: b.id, label: b.nombre }))}
              placeholder="Selecciona el destino"
              required
            />
          )}
        </div>
      </form>
    </Modal>
  );
}

interface DispatchModalProps {
  transfer: Transfer | null;
  onClose: () => void;
  onDone: () => void;
}

function DispatchModal({ transfer, onClose, onDone }: DispatchModalProps) {
  const { mutate, submitting, error, resetError } = useMutation(dispatchTransfer);
  const [cantidadEnviada, setCantidadEnviada] = useState('');
  const [transportista, setTransportista] = useState('');
  const [fecha, setFecha] = useState('');

  useEffect(() => {
    setCantidadEnviada(transfer ? String(transfer.cantidadSolicitada) : '');
    setTransportista('');
    setFecha('');
    resetError();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [transfer?.id]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!transfer || !fecha) return;
    try {
      await mutate(transfer.id, {
        cantidadEnviada: Number(cantidadEnviada),
        transportista,
        fechaEstimadaLlegada: new Date(fecha).toISOString(),
      });
      onDone();
    } catch {
      /* error mostrado */
    }
  }

  return (
    <Modal
      open={transfer !== null}
      onClose={onClose}
      title={transfer ? `Despachar transferencia #${transfer.id}` : 'Despachar'}
      className="max-w-md"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" form="dispatch-form" loading={submitting}>
            Despachar
          </Button>
        </>
      }
    >
      <form id="dispatch-form" onSubmit={handleSubmit} className="space-y-3">
        {error && <ErrorAlert message={error} />}
        <Input
          label="Cantidad enviada"
          type="number"
          step="0.01"
          min="0"
          value={cantidadEnviada}
          onChange={(e) => setCantidadEnviada(e.target.value)}
          required
        />
        <Input
          label="Transportista"
          value={transportista}
          onChange={(e) => setTransportista(e.target.value)}
          required
        />
        <Input
          label="Fecha estimada de llegada"
          type="datetime-local"
          value={fecha}
          onChange={(e) => setFecha(e.target.value)}
          required
        />
      </form>
    </Modal>
  );
}

interface ReceiveModalProps {
  transfer: Transfer | null;
  onClose: () => void;
  onDone: () => void;
}

function ReceiveModal({ transfer, onClose, onDone }: ReceiveModalProps) {
  const { mutate, submitting, error, resetError } = useMutation(receiveTransfer);
  const [cantidadRecibida, setCantidadRecibida] = useState('');

  useEffect(() => {
    setCantidadRecibida(transfer?.cantidadEnviada != null ? String(transfer.cantidadEnviada) : '');
    resetError();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [transfer?.id]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!transfer) return;
    try {
      await mutate(transfer.id, { cantidadRecibida: Number(cantidadRecibida) });
      onDone();
    } catch {
      /* error mostrado */
    }
  }

  return (
    <Modal
      open={transfer !== null}
      onClose={onClose}
      title={transfer ? `Confirmar recepción · transferencia #${transfer.id}` : 'Confirmar recepción'}
      className="max-w-md"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" form="receive-form" loading={submitting}>
            Confirmar
          </Button>
        </>
      }
    >
      <form id="receive-form" onSubmit={handleSubmit} className="space-y-3">
        {error && <ErrorAlert message={error} />}
        <p className="text-xs text-slate-500">
          Cantidad enviada: <span className="font-medium">{transfer ? formatNumber(transfer.cantidadEnviada ?? 0) : '—'}</span>.
          Si recibes menos, la transferencia quedará "Con faltantes" (RF-21).
        </p>
        <Input
          label="Cantidad recibida"
          type="number"
          step="0.01"
          min="0"
          value={cantidadRecibida}
          onChange={(e) => setCantidadRecibida(e.target.value)}
          required
        />
      </form>
    </Modal>
  );
}

interface ResolveModalProps {
  transfer: Transfer | null;
  onClose: () => void;
  onDone: () => void;
}

function ResolveModal({ transfer, onClose, onDone }: ResolveModalProps) {
  const { mutate, submitting, error, resetError } = useMutation(resolveTransfer);
  const [tratamiento, setTratamiento] = useState<TratamientoFaltante>('REENVIO');

  useEffect(() => {
    setTratamiento('REENVIO');
    resetError();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [transfer?.id]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!transfer) return;
    try {
      await mutate(transfer.id, { tratamiento });
      onDone();
    } catch {
      /* error mostrado */
    }
  }

  return (
    <Modal
      open={transfer !== null}
      onClose={() => {
        resetError();
        onClose();
      }}
      title={transfer ? `Resolver faltante · transferencia #${transfer.id}` : 'Resolver faltante'}
      className="max-w-md"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" form="resolve-form" loading={submitting}>
            Aplicar
          </Button>
        </>
      }
    >
      <form id="resolve-form" onSubmit={handleSubmit} className="space-y-3">
        {error && <ErrorAlert message={error} />}
        <p className="text-xs text-slate-500">
          Faltan{' '}
          <span className="font-medium">
            {transfer ? formatNumber((transfer.cantidadEnviada ?? 0) - (transfer.cantidadRecibida ?? 0)) : 0}
          </span>{' '}
          unidades por recibir.
        </p>
        <Select
          label="Tratamiento"
          value={tratamiento}
          onChange={(e) => setTratamiento(e.target.value as TratamientoFaltante)}
          options={[
            { value: 'REENVIO', label: 'Reenvío (nueva transferencia por el faltante)' },
            { value: 'AJUSTE', label: 'Ajuste (cerrar y asumir la pérdida)' },
            { value: 'RECLAMACION', label: 'Reclamación formal a origen' },
          ]}
        />
      </form>
    </Modal>
  );
}

interface EventsModalProps {
  id: number | null;
  onClose: () => void;
}

function EventsModal({ id, onClose }: EventsModalProps) {
  const { events, loading, error } = useTransferEvents(id);

  return (
    <Modal open={id !== null} onClose={onClose} title={id ? `Historial · transferencia #${id}` : 'Historial'} className="max-w-lg">
      {loading && <p className="text-sm text-slate-500">Cargando…</p>}
      {error && <ErrorAlert message={error} />}
      {!loading && events.length === 0 && !error && (
        <p className="text-sm text-slate-500">Sin eventos registrados.</p>
      )}
      {events.length > 0 && (
        <ol className="space-y-3 border-l-2 border-slate-100 pl-4">
          {events.map((e) => (
            <li key={e.id} className="relative">
              <span className="absolute -left-[21px] top-1 h-2.5 w-2.5 rounded-full bg-brand-500" />
              <div className="flex items-center gap-2">
                <Badge tone={ESTADO_TONE[e.estado]}>{e.estado.replace('_', ' ')}</Badge>
                <span className="text-xs text-slate-400">{formatDateTime(e.fecha)}</span>
              </div>
              {e.comentario && <p className="mt-1 text-sm text-slate-600">{e.comentario}</p>}
            </li>
          ))}
        </ol>
      )}
    </Modal>
  );
}
