import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { ArrowLeftRight, Package, SlidersHorizontal } from 'lucide-react';
import {
  Badge,
  Button,
  Input,
  Modal,
  PageHeader,
  Pagination,
  Select,
  DataTable,
  EmptyState,
  type Column,
} from '../components/ui';
import { ErrorAlert } from '../components/ErrorAlert';
import { registerMovement, setMinStock } from '../api/inventario';
import { useAuth } from '../hooks/useAuth';
import { useBranches } from '../hooks/useBranches';
import { useBranchInventory } from '../hooks/useBranchInventory';
import { useProductos } from '../hooks/useProductos';
import { useMutation } from '../hooks/useMutation';
import { useToast } from '../context/ToastContext';
import { formatCurrency, formatNumber } from '../lib/format';
import {
  MOTIVOS_POR_TIPO,
  type InventarioSucursal,
  type MotivoMovimiento,
  type MovimientoResultado,
  type TipoMovimiento,
} from '../types/inventario';

const PAGE_SIZE = 20;

export function InventarioPage() {
  const { rol, sucursalId } = useAuth();
  const isAdmin = rol === 'ADMIN_GENERAL';
  const puedeConfigurarMinimo = rol === 'ADMIN_GENERAL' || rol === 'GERENTE_SUCURSAL';

  const { branches, loading: branchesLoading, error: branchesError } = useBranches();
  const [branchId, setBranchId] = useState<number | null>(sucursalId ?? null);
  const [page, setPage] = useState(0);

  useEffect(() => {
    if (branchId === null && branches.length > 0) {
      setBranchId(branches[0].id);
    }
  }, [branches, branchId]);

  const { data, loading, error, refetch } = useBranchInventory({ branchId, page, size: PAGE_SIZE });

  const puedeEditar = branchId !== null && (isAdmin || branchId === sucursalId);

  const [movementOpen, setMovementOpen] = useState(false);
  const [minStockTarget, setMinStockTarget] = useState<InventarioSucursal | null>(null);

  const columns: Column<InventarioSucursal>[] = [
    { key: 'sku', header: 'SKU', render: (r) => <span className="font-medium text-slate-900">{r.sku}</span> },
    { key: 'producto', header: 'Producto', render: (r) => r.productoNombre },
    { key: 'cantidad', header: 'Cantidad', align: 'right', render: (r) => formatNumber(r.cantidadActual) },
    { key: 'minimo', header: 'Stock mínimo', align: 'right', render: (r) => formatNumber(r.stockMinimo) },
    {
      key: 'costo',
      header: 'Costo prom.',
      align: 'right',
      render: (r) => <span className="text-slate-500">{formatCurrency(r.costoPromedioPonderado)}</span>,
    },
    {
      key: 'estado',
      header: 'Estado',
      render: (r) =>
        r.alertaStockBajo ? (
          <Badge tone="warning">Bajo mínimo</Badge>
        ) : (
          <Badge tone="success">OK</Badge>
        ),
    },
    {
      key: 'acciones',
      header: '',
      align: 'right',
      render: (r) =>
        puedeEditar && puedeConfigurarMinimo ? (
          <Button
            size="sm"
            variant="ghost"
            onClick={() => setMinStockTarget(r)}
            aria-label={`Configurar stock mínimo de ${r.productoNombre}`}
          >
            <SlidersHorizontal className="h-4 w-4" aria-hidden />
            Stock mínimo
          </Button>
        ) : null,
    },
  ];

  const branchOptions = branches.map((b) => ({
    value: b.id,
    label: b.ciudad ? `${b.nombre} — ${b.ciudad}` : b.nombre,
  }));

  return (
    <div className="space-y-6">
      <PageHeader
        title="Inventario por sucursal"
        description={
          puedeEditar
            ? 'Existencias, stock mínimo y costo promedio de la sucursal seleccionada.'
            : 'Consulta de solo lectura del inventario de otra sucursal.'
        }
        actions={
          puedeEditar ? (
            <Button onClick={() => setMovementOpen(true)}>
              <ArrowLeftRight className="h-4 w-4" aria-hidden />
              Registrar movimiento
            </Button>
          ) : undefined
        }
      />

      <div className="max-w-xs">
        <Select
          label="Sucursal"
          value={branchId ?? ''}
          onChange={(e) => {
            setBranchId(e.target.value ? Number(e.target.value) : null);
            setPage(0);
          }}
          options={branchOptions}
          placeholder={branchesLoading ? 'Cargando…' : 'Selecciona una sucursal'}
        />
      </div>

      {branchesError && <ErrorAlert message={branchesError} />}
      {error && <ErrorAlert message={error} />}

      {branchId !== null && !puedeEditar && (
        <p className="text-xs text-slate-500">
          Estás viendo una sucursal distinta a la tuya: solo lectura.
        </p>
      )}

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(r) => r.productId}
        loading={loading}
        empty={
          <EmptyState
            icon={Package}
            title="Sin existencias"
            description="Esta sucursal aún no tiene productos con inventario registrado."
          />
        }
      />

      {data && <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />}

      <MovementModal
        open={movementOpen}
        branchId={branchId}
        onClose={() => setMovementOpen(false)}
        onRegistered={refetch}
      />

      <MinStockModal
        target={minStockTarget}
        branchId={branchId}
        onClose={() => setMinStockTarget(null)}
        onSaved={refetch}
      />
    </div>
  );
}

interface MovementModalProps {
  open: boolean;
  branchId: number | null;
  onClose: () => void;
  onRegistered: () => void;
}

function MovementModal({ open, branchId, onClose, onRegistered }: MovementModalProps) {
  const { data: productos, loading: productosLoading } = useProductos({
    page: 0,
    size: 200,
    branchId: branchId ?? undefined,
    enabled: open,
  });
  const { mutate, submitting, error, resetError } = useMutation(registerMovement);

  const [productId, setProductId] = useState('');
  const [tipo, setTipo] = useState<TipoMovimiento>('INGRESO');
  const [motivo, setMotivo] = useState<MotivoMovimiento>('AJUSTE');
  const [cantidad, setCantidad] = useState('');
  const [resultado, setResultado] = useState<MovimientoResultado | null>(null);

  const motivosDisponibles = MOTIVOS_POR_TIPO[tipo];

  useEffect(() => {
    if (!open) {
      setProductId('');
      setTipo('INGRESO');
      setMotivo('AJUSTE');
      setCantidad('');
      setResultado(null);
      resetError();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  useEffect(() => {
    if (!motivosDisponibles.includes(motivo)) {
      setMotivo(motivosDisponibles[0]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tipo]);

  const productOptions = useMemo(
    () => (productos?.content ?? []).map((p) => ({ value: p.id, label: `${p.sku} · ${p.nombre}` })),
    [productos],
  );

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (branchId === null || !productId) return;
    try {
      const res = await mutate({
        productId: Number(productId),
        branchId,
        tipo,
        motivo,
        cantidad: Number(cantidad),
      });
      setResultado(res);
      setCantidad('');
      onRegistered();
    } catch {
      /* error mostrado en el modal */
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Registrar movimiento de inventario"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cerrar
          </Button>
          <Button type="submit" form="movement-form" loading={submitting}>
            Registrar
          </Button>
        </>
      }
    >
      <form id="movement-form" onSubmit={handleSubmit} className="space-y-4">
        {error && <ErrorAlert message={error} />}

        <Select
          label="Producto"
          hint="Producto sobre el que se registrará el movimiento en esta sucursal."
          value={productId}
          onChange={(e) => setProductId(e.target.value)}
          options={productOptions}
          placeholder={productosLoading ? 'Cargando…' : 'Selecciona un producto'}
          required
        />

        <div className="grid grid-cols-2 gap-3">
          <Select
            label="Tipo"
            hint="Ingreso suma al inventario; retiro lo descuenta."
            value={tipo}
            onChange={(e) => setTipo(e.target.value as TipoMovimiento)}
            options={[
              { value: 'INGRESO', label: 'Ingreso' },
              { value: 'RETIRO', label: 'Retiro' },
            ]}
          />
          <Select
            label="Motivo"
            hint="Razón del movimiento (varía según el tipo elegido)."
            value={motivo}
            onChange={(e) => setMotivo(e.target.value as MotivoMovimiento)}
            options={motivosDisponibles.map((m) => ({ value: m, label: m }))}
          />
        </div>

        <Input
          label="Cantidad"
          hint="Cantidad a mover, en la unidad de medida base del producto."
          type="number"
          step="0.01"
          min="0"
          value={cantidad}
          onChange={(e) => setCantidad(e.target.value)}
          required
        />

        {resultado && (
          <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-2.5 text-sm text-emerald-800">
            Movimiento registrado. Existencia actual:{' '}
            <span className="font-semibold">{formatNumber(resultado.cantidadActual)}</span>
            {resultado.alertaStockBajo && ' · por debajo del stock mínimo'}
          </div>
        )}
      </form>
    </Modal>
  );
}

interface MinStockModalProps {
  target: InventarioSucursal | null;
  branchId: number | null;
  onClose: () => void;
  onSaved: () => void;
}

function MinStockModal({ target, branchId, onClose, onSaved }: MinStockModalProps) {
  const { mutate, submitting, error, resetError } = useMutation(setMinStock);
  const { showSuccess, showError } = useToast();
  const [valor, setValor] = useState('');

  useEffect(() => {
    setValor(target ? String(target.stockMinimo) : '');
    resetError();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target?.productId]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!target || branchId === null) return;
    try {
      await mutate(target.productId, { stockMinimo: Number(valor), branchId });
      showSuccess(`Stock mínimo de "${target.productoNombre}" actualizado.`);
      onSaved();
      onClose();
    } catch (err) {
      showError((err as { message?: string }).message ?? 'No se pudo actualizar el stock mínimo.');
      /* el error también se muestra en el modal */
    }
  }

  return (
    <Modal
      open={target !== null}
      onClose={onClose}
      title={target ? `Stock mínimo · ${target.productoNombre}` : 'Stock mínimo'}
      className="max-w-md"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" form="min-stock-form" loading={submitting}>
            Guardar
          </Button>
        </>
      }
    >
      <form id="min-stock-form" onSubmit={handleSubmit} className="space-y-3">
        {error && <ErrorAlert message={error} />}
        <Input
          label="Stock mínimo"
          hint="Se genera una alerta cuando la existencia cae por debajo de este valor."
          type="number"
          step="0.01"
          min="0"
          value={valor}
          onChange={(e) => setValor(e.target.value)}
          required
        />
      </form>
    </Modal>
  );
}
