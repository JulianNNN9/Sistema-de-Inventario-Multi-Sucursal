import { useMemo, useState, type FormEvent } from 'react';
import { CheckCircle2, Eye, Plus, Trash2, Truck } from 'lucide-react';
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
  Textarea,
  type Column,
} from '../components/ui';
import { ErrorAlert } from '../components/ErrorAlert';
import { confirmReceipt, createPurchaseOrder } from '../api/compras';
import { createSupplier } from '../api/proveedores';
import { useAuth } from '../hooks/useAuth';
import { useBranches } from '../hooks/useBranches';
import { useComprasList } from '../hooks/useComprasList';
import { useMutation } from '../hooks/useMutation';
import { useProductos } from '../hooks/useProductos';
import { useProveedores } from '../hooks/useProveedores';
import { usePurchaseOrder } from '../hooks/usePurchaseOrder';
import { formatCurrency, formatDateTime, formatNumber } from '../lib/format';
import type {
  EstadoOrdenCompra,
  PurchaseOrderInput,
  PurchaseOrderLineInput,
  PurchaseOrderSummary,
} from '../types/compra';

const PAGE_SIZE = 20;

const ESTADO_TONE: Record<EstadoOrdenCompra, 'warning' | 'success' | 'neutral'> = {
  PENDIENTE: 'warning',
  RECIBIDA: 'success',
  CANCELADA: 'neutral',
};

export function ComprasPage() {
  const { rol } = useAuth();
  const canManage = rol === 'ADMIN_GENERAL' || rol === 'OPERADOR_INVENTARIO';
  const isAdmin = rol === 'ADMIN_GENERAL';

  const { proveedores } = useProveedores();
  const { data: productosData } = useProductos({ page: 0, size: 300 });
  const { branches } = useBranches();

  const [page, setPage] = useState(0);
  const [supplierId, setSupplierId] = useState('');
  const [productId, setProductId] = useState('');
  const [branchId, setBranchId] = useState('');

  const { data, loading, error, refetch } = useComprasList({
    page,
    size: PAGE_SIZE,
    supplierId: supplierId ? Number(supplierId) : undefined,
    productId: productId ? Number(productId) : undefined,
    branchId: branchId ? Number(branchId) : undefined,
  });

  const [formOpen, setFormOpen] = useState(false);
  const [suppliersOpen, setSuppliersOpen] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);
  const [receiptTarget, setReceiptTarget] = useState<PurchaseOrderSummary | null>(null);

  const receiptM = useMutation(confirmReceipt);

  async function handleConfirmReceipt() {
    if (!receiptTarget) return;
    try {
      await receiptM.mutate(receiptTarget.id);
      setReceiptTarget(null);
      refetch();
    } catch {
      /* error mostrado en el ConfirmDialog */
    }
  }

  const columns: Column<PurchaseOrderSummary>[] = [
    { key: 'id', header: 'N.º', render: (o) => <span className="font-medium text-slate-900">#{o.id}</span> },
    { key: 'fecha', header: 'Fecha', render: (o) => formatDateTime(o.fecha) },
    { key: 'proveedor', header: 'Proveedor', render: (o) => o.supplierNombre },
    { key: 'sucursal', header: 'Sucursal', render: (o) => o.sucursalNombre },
    { key: 'total', header: 'Total', align: 'right', render: (o) => formatCurrency(o.total) },
    { key: 'estado', header: 'Estado', render: (o) => <Badge tone={ESTADO_TONE[o.estado]}>{o.estado}</Badge> },
    {
      key: 'acciones',
      header: '',
      align: 'right',
      render: (o) => (
        <div className="flex justify-end gap-1">
          <Button size="sm" variant="ghost" onClick={() => setDetailId(o.id)} aria-label={`Ver orden ${o.id}`}>
            <Eye className="h-4 w-4" aria-hidden />
          </Button>
          {canManage && o.estado === 'PENDIENTE' && (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => setReceiptTarget(o)}
              aria-label={`Confirmar recepción de la orden ${o.id}`}
            >
              <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-hidden />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Compras"
        description="Órdenes de compra a proveedores e histórico (RF-08..RF-12)."
        actions={
          canManage ? (
            <>
              <Button variant="secondary" onClick={() => setSuppliersOpen(true)}>
                <Truck className="h-4 w-4" aria-hidden />
                Proveedores
              </Button>
              <Button onClick={() => setFormOpen(true)}>
                <Plus className="h-4 w-4" aria-hidden />
                Nueva orden
              </Button>
            </>
          ) : undefined
        }
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Select
          label="Proveedor"
          value={supplierId}
          onChange={(e) => {
            setSupplierId(e.target.value);
            setPage(0);
          }}
          options={proveedores.map((p) => ({ value: p.id, label: p.nombre }))}
          placeholder="Todos"
        />
        <Select
          label="Producto"
          value={productId}
          onChange={(e) => {
            setProductId(e.target.value);
            setPage(0);
          }}
          options={(productosData?.content ?? []).map((p) => ({ value: p.id, label: `${p.sku} · ${p.nombre}` }))}
          placeholder="Todos"
        />
        {isAdmin && (
          <Select
            label="Sucursal"
            value={branchId}
            onChange={(e) => {
              setBranchId(e.target.value);
              setPage(0);
            }}
            options={branches.map((b) => ({ value: b.id, label: b.nombre }))}
            placeholder="Todas"
          />
        )}
      </div>

      {error && <ErrorAlert message={error} />}

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(o) => o.id}
        loading={loading}
        empty={
          <EmptyState
            icon={Truck}
            title="Sin órdenes de compra"
            description={canManage ? 'Crea la primera orden de compra a un proveedor.' : 'Aún no hay órdenes registradas.'}
            action={
              canManage ? (
                <Button onClick={() => setFormOpen(true)}>
                  <Plus className="h-4 w-4" aria-hidden />
                  Nueva orden
                </Button>
              ) : undefined
            }
          />
        }
      />

      {data && <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />}

      <PurchaseOrderModal
        open={formOpen}
        isAdmin={isAdmin}
        onClose={() => setFormOpen(false)}
        onCreated={() => {
          setFormOpen(false);
          refetch();
        }}
      />

      <SuppliersModal open={suppliersOpen} onClose={() => setSuppliersOpen(false)} />

      <OrderDetailModal
        id={detailId}
        canManage={canManage}
        onClose={() => setDetailId(null)}
        onReceived={() => {
          setDetailId(null);
          refetch();
        }}
      />

      <ConfirmDialog
        open={receiptTarget !== null}
        title="Confirmar recepción"
        message={`¿Confirmar la recepción completa de la orden #${receiptTarget?.id ?? ''}? Se actualizará el inventario y el costo promedio.`}
        confirmLabel="Confirmar recepción"
        loading={receiptM.submitting}
        error={receiptM.error}
        onConfirm={handleConfirmReceipt}
        onCancel={() => {
          setReceiptTarget(null);
          receiptM.resetError();
        }}
      />
    </div>
  );
}

interface LineDraft {
  productId: string;
  cantidad: string;
  precioUnitario: string;
  descuento: string;
}

const EMPTY_LINE: LineDraft = { productId: '', cantidad: '', precioUnitario: '', descuento: '' };

interface PurchaseOrderModalProps {
  open: boolean;
  isAdmin: boolean;
  onClose: () => void;
  onCreated: () => void;
}

function PurchaseOrderModal({ open, isAdmin, onClose, onCreated }: PurchaseOrderModalProps) {
  const { proveedores } = useProveedores();
  const { branches } = useBranches();
  const { data: productosData } = useProductos({ page: 0, size: 300, enabled: open });
  const { mutate, submitting, error, resetError } = useMutation(createPurchaseOrder);

  const [supplierId, setSupplierId] = useState('');
  const [branchId, setBranchId] = useState('');
  const [plazoPago, setPlazoPago] = useState('');
  const [lineas, setLineas] = useState<LineDraft[]>([{ ...EMPTY_LINE }]);

  const productOptions = useMemo(
    () => (productosData?.content ?? []).map((p) => ({ value: p.id, label: `${p.sku} · ${p.nombre}` })),
    [productosData],
  );

  function reset() {
    setSupplierId('');
    setBranchId('');
    setPlazoPago('');
    setLineas([{ ...EMPTY_LINE }]);
    resetError();
  }

  function updateLine(index: number, patch: Partial<LineDraft>) {
    setLineas((prev) => prev.map((l, i) => (i === index ? { ...l, ...patch } : l)));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const parsedLines: PurchaseOrderLineInput[] = lineas
      .filter((l) => l.productId && l.cantidad && l.precioUnitario)
      .map((l) => ({
        productId: Number(l.productId),
        cantidad: Number(l.cantidad),
        precioUnitario: Number(l.precioUnitario),
        descuento: l.descuento ? Number(l.descuento) : undefined,
      }));

    const body: PurchaseOrderInput = {
      supplierId: Number(supplierId),
      branchId: isAdmin && branchId ? Number(branchId) : undefined,
      plazoPago: plazoPago || undefined,
      lineas: parsedLines,
    };

    try {
      await mutate(body);
      reset();
      onCreated();
    } catch {
      /* error mostrado en el modal */
    }
  }

  return (
    <Modal
      open={open}
      onClose={() => {
        reset();
        onClose();
      }}
      title="Nueva orden de compra"
      className="max-w-2xl"
      footer={
        <>
          <Button
            variant="secondary"
            onClick={() => {
              reset();
              onClose();
            }}
            disabled={submitting}
          >
            Cancelar
          </Button>
          <Button type="submit" form="purchase-order-form" loading={submitting}>
            Crear orden
          </Button>
        </>
      }
    >
      <form id="purchase-order-form" onSubmit={handleSubmit} className="space-y-4">
        {error && <ErrorAlert message={error} />}

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Select
            label="Proveedor"
            value={supplierId}
            onChange={(e) => setSupplierId(e.target.value)}
            options={proveedores.map((p) => ({ value: p.id, label: p.nombre }))}
            placeholder="Selecciona un proveedor"
            required
          />
          {isAdmin && (
            <Select
              label="Sucursal"
              value={branchId}
              onChange={(e) => setBranchId(e.target.value)}
              options={branches.map((b) => ({ value: b.id, label: b.nombre }))}
              placeholder="Selecciona una sucursal"
              required
            />
          )}
          <Input
            label="Plazo de pago"
            value={plazoPago}
            onChange={(e) => setPlazoPago(e.target.value)}
            placeholder="30 días"
          />
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-slate-700">Líneas</span>
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={() => setLineas((prev) => [...prev, { ...EMPTY_LINE }])}
            >
              <Plus className="h-4 w-4" aria-hidden />
              Agregar línea
            </Button>
          </div>

          {lineas.map((linea, index) => (
            <div
              key={index}
              className="grid grid-cols-1 gap-2 rounded-xl border border-slate-200 p-3 sm:grid-cols-[1fr_auto_auto_auto_auto]"
            >
              <Select
                value={linea.productId}
                onChange={(e) => updateLine(index, { productId: e.target.value })}
                options={productOptions}
                placeholder="Producto"
                aria-label={`Producto de la línea ${index + 1}`}
              />
              <Input
                type="number"
                step="0.01"
                min="0"
                placeholder="Cant."
                className="sm:w-24"
                value={linea.cantidad}
                onChange={(e) => updateLine(index, { cantidad: e.target.value })}
                aria-label={`Cantidad de la línea ${index + 1}`}
              />
              <Input
                type="number"
                step="0.01"
                min="0"
                placeholder="Precio"
                className="sm:w-28"
                value={linea.precioUnitario}
                onChange={(e) => updateLine(index, { precioUnitario: e.target.value })}
                aria-label={`Precio unitario de la línea ${index + 1}`}
              />
              <Input
                type="number"
                step="0.01"
                min="0"
                max="100"
                placeholder="Desc. %"
                className="sm:w-24"
                value={linea.descuento}
                onChange={(e) => updateLine(index, { descuento: e.target.value })}
                aria-label={`Descuento de la línea ${index + 1}`}
              />
              <Button
                type="button"
                size="sm"
                variant="ghost"
                disabled={lineas.length === 1}
                onClick={() => setLineas((prev) => prev.filter((_, i) => i !== index))}
                aria-label={`Eliminar línea ${index + 1}`}
              >
                <Trash2 className="h-4 w-4 text-rose-500" aria-hidden />
              </Button>
            </div>
          ))}
        </div>
      </form>
    </Modal>
  );
}

interface OrderDetailModalProps {
  id: number | null;
  canManage: boolean;
  onClose: () => void;
  onReceived: () => void;
}

function OrderDetailModal({ id, canManage, onClose, onReceived }: OrderDetailModalProps) {
  const { order, loading, error } = usePurchaseOrder(id);
  const receiptM = useMutation(confirmReceipt);

  async function handleReceipt() {
    if (!order) return;
    try {
      await receiptM.mutate(order.id);
      onReceived();
    } catch {
      /* error mostrado */
    }
  }

  return (
    <Modal
      open={id !== null}
      onClose={onClose}
      title={order ? `Orden de compra #${order.id}` : 'Orden de compra'}
      className="max-w-2xl"
      footer={
        canManage && order?.estado === 'PENDIENTE' ? (
          <Button variant="primary" onClick={handleReceipt} loading={receiptM.submitting}>
            <CheckCircle2 className="h-4 w-4" aria-hidden />
            Confirmar recepción
          </Button>
        ) : undefined
      }
    >
      {loading && <p className="text-sm text-slate-500">Cargando…</p>}
      {error && <ErrorAlert message={error} />}
      {receiptM.error && <ErrorAlert message={receiptM.error} className="mt-2" />}

      {order && (
        <div className="space-y-4">
          <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
            <div>
              <dt className="text-slate-500">Proveedor</dt>
              <dd className="font-medium text-slate-900">{order.supplierNombre}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Sucursal</dt>
              <dd className="font-medium text-slate-900">{order.sucursalNombre}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Fecha</dt>
              <dd className="text-slate-700">{formatDateTime(order.fecha)}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Estado</dt>
              <dd>
                <Badge tone={ESTADO_TONE[order.estado]}>{order.estado}</Badge>
              </dd>
            </div>
            {order.plazoPago && (
              <div>
                <dt className="text-slate-500">Plazo de pago</dt>
                <dd className="text-slate-700">{order.plazoPago}</dd>
              </div>
            )}
          </dl>

          <div className="overflow-x-auto rounded-xl border border-slate-200">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-xs font-medium uppercase tracking-wide text-slate-500">
                  <th className="px-3 py-2 text-left">Producto</th>
                  <th className="px-3 py-2 text-right">Cant.</th>
                  <th className="px-3 py-2 text-right">Precio</th>
                  <th className="px-3 py-2 text-right">Desc. %</th>
                  <th className="px-3 py-2 text-right">Subtotal</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {order.lineas.map((linea) => (
                  <tr key={linea.productId}>
                    <td className="px-3 py-2 text-slate-700">
                      {linea.sku} · {linea.productoNombre}
                    </td>
                    <td className="px-3 py-2 text-right">{formatNumber(linea.cantidad)}</td>
                    <td className="px-3 py-2 text-right">{formatCurrency(linea.precioUnitario)}</td>
                    <td className="px-3 py-2 text-right">{formatNumber(linea.descuento)}</td>
                    <td className="px-3 py-2 text-right">{formatCurrency(linea.subtotal)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t border-slate-200 font-medium text-slate-900">
                  <td className="px-3 py-2" colSpan={4}>
                    Total
                  </td>
                  <td className="px-3 py-2 text-right">{formatCurrency(order.total)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}
    </Modal>
  );
}

interface SuppliersModalProps {
  open: boolean;
  onClose: () => void;
}

function SuppliersModal({ open, onClose }: SuppliersModalProps) {
  const { proveedores, loading, refetch } = useProveedores();
  const { mutate, submitting, error, resetError } = useMutation(createSupplier);
  const [nombre, setNombre] = useState('');
  const [condiciones, setCondiciones] = useState('');

  async function handleAdd(event: FormEvent) {
    event.preventDefault();
    try {
      await mutate({ nombre, condiciones: condiciones || undefined });
      setNombre('');
      setCondiciones('');
      resetError();
      refetch();
    } catch {
      /* error mostrado */
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Proveedores" className="max-w-lg">
      <div className="space-y-4">
        {loading && <p className="text-sm text-slate-500">Cargando…</p>}
        {!loading && proveedores.length > 0 && (
          <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200">
            {proveedores.map((p) => (
              <li key={p.id} className="px-3 py-2 text-sm">
                <p className="font-medium text-slate-800">{p.nombre}</p>
                {p.condiciones && <p className="text-slate-500">{p.condiciones}</p>}
              </li>
            ))}
          </ul>
        )}
        {!loading && proveedores.length === 0 && (
          <p className="rounded-xl border border-dashed border-slate-200 px-3 py-4 text-center text-sm text-slate-500">
            Aún no hay proveedores registrados.
          </p>
        )}

        <form onSubmit={handleAdd} className="space-y-3 border-t border-slate-100 pt-4">
          {error && <ErrorAlert message={error} />}
          <Input
            label="Nombre"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
          />
          <Textarea
            label="Condiciones comerciales"
            rows={2}
            value={condiciones}
            onChange={(e) => setCondiciones(e.target.value)}
          />
          <Button type="submit" size="sm" loading={submitting}>
            <Plus className="h-4 w-4" aria-hidden />
            Agregar proveedor
          </Button>
        </form>
      </div>
    </Modal>
  );
}
