import { useMemo, useState, type FormEvent } from 'react';
import { Eye, Plus, Receipt, Tags, Trash2 } from 'lucide-react';
import {
  Button,
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
import { createSale } from '../api/ventas';
import { createPriceList } from '../api/priceLists';
import { useAuth } from '../hooks/useAuth';
import { useBranches } from '../hooks/useBranches';
import { useMutation } from '../hooks/useMutation';
import { usePriceLists } from '../hooks/usePriceLists';
import { useProductos } from '../hooks/useProductos';
import { useVenta } from '../hooks/useVenta';
import { useVentasList } from '../hooks/useVentasList';
import { useToast } from '../context/ToastContext';
import { formatCurrency, formatDateTime, formatNumber } from '../lib/format';
import type { PriceList, Sale, SaleInput, SaleLineInput, SaleSummary } from '../types/venta';

const PAGE_SIZE = 20;

function net(cantidad: number, precio: number, descuento: number): number {
  if (!Number.isFinite(cantidad) || !Number.isFinite(precio)) return 0;
  return cantidad * precio * (1 - (Number.isFinite(descuento) ? descuento : 0) / 100);
}

export function VentasPage() {
  const { rol } = useAuth();
  const isAdmin = rol === 'ADMIN_GENERAL';
  // Registrar venta: disponible para los tres roles.
  const canManage = rol === 'ADMIN_GENERAL' || rol === 'GERENTE_SUCURSAL' || rol === 'OPERADOR_INVENTARIO';
  // Listas de precios: política comercial de la sucursal (ADMIN + GERENTE), no del Operador.
  const canManagePriceLists = rol === 'ADMIN_GENERAL' || rol === 'GERENTE_SUCURSAL';

  const { branches } = useBranches();
  const { priceLists, refetch: refetchPriceLists } = usePriceLists();

  const [page, setPage] = useState(0);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [filterBranch, setFilterBranch] = useState('');

  const { data, loading, error, refetch } = useVentasList({
    page,
    size: PAGE_SIZE,
    branchId: filterBranch ? Number(filterBranch) : undefined,
    from,
    to,
  });

  const [saleOpen, setSaleOpen] = useState(false);
  const [priceListsOpen, setPriceListsOpen] = useState(false);
  const [receiptId, setReceiptId] = useState<number | null>(null);
  const [receiptSale, setReceiptSale] = useState<Sale | null>(null);

  const columns: Column<SaleSummary>[] = [
    { key: 'id', header: 'N.º', render: (v) => <span className="font-medium text-slate-900">#{v.id}</span> },
    { key: 'fecha', header: 'Fecha', render: (v) => formatDateTime(v.fecha) },
    { key: 'sucursal', header: 'Sucursal', render: (v) => v.sucursalNombre },
    { key: 'usuario', header: 'Responsable', render: (v) => v.usuarioNombre },
    { key: 'total', header: 'Total', align: 'right', render: (v) => formatCurrency(v.total) },
    {
      key: 'acciones',
      header: '',
      align: 'right',
      render: (v) => (
        <Button size="sm" variant="ghost" onClick={() => setReceiptId(v.id)} aria-label={`Ver comprobante ${v.id}`}>
          <Eye className="h-4 w-4" aria-hidden />
          Ver comprobante
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Ventas"
        description="Registro de ventas, comprobantes e histórico."
        actions={
          canManage || canManagePriceLists ? (
            <>
              {canManagePriceLists && (
                <Button variant="secondary" onClick={() => setPriceListsOpen(true)}>
                  <Tags className="h-4 w-4" aria-hidden />
                  Listas de precios
                </Button>
              )}
              {canManage && (
                <Button onClick={() => setSaleOpen(true)}>
                  <Plus className="h-4 w-4" aria-hidden />
                  Nueva venta
                </Button>
              )}
            </>
          ) : undefined
        }
      />

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Input
          label="Desde"
          type="date"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            setPage(0);
          }}
        />
        <Input
          label="Hasta"
          type="date"
          value={to}
          onChange={(e) => {
            setTo(e.target.value);
            setPage(0);
          }}
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
      </div>

      {error && <ErrorAlert message={error} />}

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(v) => v.id}
        loading={loading}
        empty={
          <EmptyState
            icon={Receipt}
            title="Sin ventas"
            description={canManage ? 'Registra la primera venta.' : 'Aún no hay ventas registradas.'}
            action={
              canManage ? (
                <Button onClick={() => setSaleOpen(true)}>
                  <Plus className="h-4 w-4" aria-hidden />
                  Nueva venta
                </Button>
              ) : undefined
            }
          />
        }
      />

      {data && <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />}

      <SaleModal
        open={saleOpen}
        isAdmin={isAdmin}
        branches={branches}
        priceLists={priceLists}
        onClose={() => setSaleOpen(false)}
        onCreated={(sale) => {
          setSaleOpen(false);
          setReceiptSale(sale);
          refetch();
        }}
      />

      <PriceListsModal
        open={priceListsOpen}
        isAdmin={isAdmin}
        branches={branches}
        priceLists={priceLists}
        onClose={() => setPriceListsOpen(false)}
        onCreated={refetchPriceLists}
      />

      <ReceiptModal
        id={receiptId}
        sale={receiptSale}
        onClose={() => {
          setReceiptId(null);
          setReceiptSale(null);
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

interface SaleModalProps {
  open: boolean;
  isAdmin: boolean;
  branches: { id: number; nombre: string }[];
  priceLists: PriceList[];
  onClose: () => void;
  onCreated: (sale: Sale) => void;
}

function SaleModal({ open, isAdmin, branches, priceLists, onClose, onCreated }: SaleModalProps) {
  const { data: productosData } = useProductos({ page: 0, size: 300, enabled: open });
  const { mutate, submitting, error, resetError } = useMutation(createSale);
  const { showError } = useToast();

  const [branchId, setBranchId] = useState('');
  const [priceListId, setPriceListId] = useState('');
  const [lineas, setLineas] = useState<LineDraft[]>([{ ...EMPTY_LINE }]);

  const selectedList = useMemo(
    () => priceLists.find((l) => String(l.id) === priceListId) ?? null,
    [priceLists, priceListId],
  );

  const productOptions = useMemo(
    () => (productosData?.content ?? []).map((p) => ({ value: p.id, label: `${p.sku} · ${p.nombre}` })),
    [productosData],
  );

  function priceFor(productId: string): number | null {
    if (!selectedList || !productId) return null;
    return selectedList.items.find((i) => String(i.productId) === productId)?.precio ?? null;
  }

  function reset() {
    setBranchId('');
    setPriceListId('');
    setLineas([{ ...EMPTY_LINE }]);
    resetError();
  }

  function close() {
    reset();
    onClose();
  }

  function updateLine(index: number, patch: Partial<LineDraft>) {
    setLineas((prev) => prev.map((l, i) => (i === index ? { ...l, ...patch } : l)));
  }

  const total = lineas.reduce((acc, l) => {
    const precio = selectedList ? priceFor(l.productId) ?? 0 : Number(l.precioUnitario);
    return acc + net(Number(l.cantidad), precio, Number(l.descuento));
  }, 0);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const parsed: SaleLineInput[] = lineas
      .filter((l) => l.productId && l.cantidad)
      .map((l) => ({
        productId: Number(l.productId),
        cantidad: Number(l.cantidad),
        descuento: l.descuento ? Number(l.descuento) : undefined,
        ...(selectedList
          ? { priceListId: selectedList.id }
          : { precioUnitario: l.precioUnitario ? Number(l.precioUnitario) : undefined }),
      }));

    const body: SaleInput = {
      branchId: isAdmin && branchId ? Number(branchId) : undefined,
      lineas: parsed,
    };

    try {
      const sale = await mutate(body);
      reset();
      onCreated(sale);
    } catch (err) {
      showError((err as { message?: string }).message ?? 'No se pudo registrar la venta.');
      /* el error también se muestra en el modal */
    }
  }

  return (
    <Modal
      open={open}
      onClose={close}
      title="Nueva venta"
      className="max-w-2xl"
      footer={
        <>
          <Button variant="secondary" onClick={close} disabled={submitting}>
            Cancelar
          </Button>
          <Button type="submit" form="sale-form" loading={submitting}>
            Registrar venta
          </Button>
        </>
      }
    >
      <form id="sale-form" onSubmit={handleSubmit} className="space-y-4">
        {error && <ErrorAlert message={error} />}

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {isAdmin && (
            <Select
              label="Sucursal"
              hint="Sucursal desde la que se registra la venta y de la que se descuenta el inventario."
              value={branchId}
              onChange={(e) => setBranchId(e.target.value)}
              options={branches.map((b) => ({ value: b.id, label: b.nombre }))}
              placeholder="Selecciona una sucursal"
              required
            />
          )}
          <Select
            label="Lista de precios (opcional)"
            hint="Si eliges una lista, el precio de cada línea se toma de ella automáticamente."
            value={priceListId}
            onChange={(e) => setPriceListId(e.target.value)}
            options={priceLists.map((l) => ({
              value: l.id,
              label: l.sucursalNombre ? `${l.nombre} (${l.sucursalNombre})` : `${l.nombre} (global)`,
            }))}
            placeholder="Sin lista — precio manual"
          />
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-slate-700">Productos</span>
            <Button
              type="button"
              size="sm"
              variant="ghost"
              onClick={() => setLineas((prev) => [...prev, { ...EMPTY_LINE }])}
            >
              <Plus className="h-4 w-4" aria-hidden />
              Agregar
            </Button>
          </div>
          <p className="text-xs text-slate-500">
            Por cada línea: producto, cantidad, precio (si no hay lista) y descuento opcional (%).
          </p>

          {lineas.map((linea, index) => {
            const listPrice = priceFor(linea.productId);
            return (
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
                {selectedList ? (
                  <div className="flex h-10 items-center justify-end px-2 text-sm text-slate-500 sm:w-28">
                    {listPrice !== null ? formatCurrency(listPrice) : 'sin precio'}
                  </div>
                ) : (
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
                )}
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
                  Eliminar
                </Button>
              </div>
            );
          })}
        </div>

        <div className="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-2.5">
          <span className="text-sm font-medium text-slate-600">Total estimado</span>
          <span className="text-lg font-semibold text-slate-900">{formatCurrency(total)}</span>
        </div>
      </form>
    </Modal>
  );
}

interface ReceiptModalProps {
  id: number | null;
  sale: Sale | null;
  onClose: () => void;
}

function ReceiptModal({ id, sale, onClose }: ReceiptModalProps) {
  const { sale: fetched, loading, error } = useVenta(sale ? null : id);
  const data = sale ?? fetched;
  const isOpen = id !== null || sale !== null;

  return (
    <Modal open={isOpen} onClose={onClose} title={data ? `Comprobante de venta #${data.id}` : 'Comprobante'} className="max-w-xl">
      {loading && <p className="text-sm text-slate-500">Cargando…</p>}
      {error && <ErrorAlert message={error} />}

      {data && (
        <div className="space-y-4">
          <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
            <div>
              <dt className="text-slate-500">Sucursal</dt>
              <dd className="font-medium text-slate-900">{data.sucursalNombre}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Responsable</dt>
              <dd className="font-medium text-slate-900">{data.usuarioNombre}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Fecha</dt>
              <dd className="text-slate-700">{formatDateTime(data.fecha)}</dd>
            </div>
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
                {data.lineas.map((linea) => (
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
                  <td className="px-3 py-2 text-right">{formatCurrency(data.total)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}
    </Modal>
  );
}

interface PriceItemDraft {
  productId: string;
  precio: string;
}

const EMPTY_PRICE_ITEM: PriceItemDraft = { productId: '', precio: '' };

interface PriceListsModalProps {
  open: boolean;
  isAdmin: boolean;
  branches: { id: number; nombre: string }[];
  priceLists: PriceList[];
  onClose: () => void;
  onCreated: () => void;
}

function PriceListsModal({ open, isAdmin, branches, priceLists, onClose, onCreated }: PriceListsModalProps) {
  const { data: productosData } = useProductos({ page: 0, size: 300, enabled: open });
  const { mutate, submitting, error, resetError } = useMutation(createPriceList);
  const { showSuccess, showError } = useToast();

  const [nombre, setNombre] = useState('');
  const [branchId, setBranchId] = useState('');
  const [items, setItems] = useState<PriceItemDraft[]>([{ ...EMPTY_PRICE_ITEM }]);

  const productOptions = useMemo(
    () => (productosData?.content ?? []).map((p) => ({ value: p.id, label: `${p.sku} · ${p.nombre}` })),
    [productosData],
  );

  function reset() {
    setNombre('');
    setBranchId('');
    setItems([{ ...EMPTY_PRICE_ITEM }]);
    resetError();
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      await mutate({
        nombre,
        branchId: isAdmin && branchId ? Number(branchId) : undefined,
        items: items
          .filter((i) => i.productId && i.precio)
          .map((i) => ({ productId: Number(i.productId), precio: Number(i.precio) })),
      });
      showSuccess(`Lista de precios "${nombre}" creada.`);
      reset();
      onCreated();
    } catch (err) {
      showError((err as { message?: string }).message ?? 'No se pudo crear la lista de precios.');
      /* el error también se muestra en el modal */
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Listas de precios" className="max-w-xl">
      <div className="space-y-4">
        {priceLists.length > 0 && (
          <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200">
            {priceLists.map((l) => (
              <li key={l.id} className="flex items-center justify-between px-3 py-2 text-sm">
                <span className="font-medium text-slate-800">{l.nombre}</span>
                <span className="text-slate-500">
                  {l.sucursalNombre ?? 'Global'} · {l.items.length} ítems
                </span>
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={handleSubmit} className="space-y-3 border-t border-slate-100 pt-4">
          {error && <ErrorAlert message={error} />}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Input
              label="Nombre"
              hint="Nombre con el que se identificará esta lista al aplicarla en una venta."
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              required
            />
            {isAdmin && (
              <Select
                label="Sucursal"
                hint="Deja sin seleccionar para crear una lista global (todas las sucursales)."
                value={branchId}
                onChange={(e) => setBranchId(e.target.value)}
                options={branches.map((b) => ({ value: b.id, label: b.nombre }))}
                placeholder="Global"
              />
            )}
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-slate-700">Ítems</span>
              <Button
                type="button"
                size="sm"
                variant="ghost"
                onClick={() => setItems((prev) => [...prev, { ...EMPTY_PRICE_ITEM }])}
              >
                <Plus className="h-4 w-4" aria-hidden />
                Agregar
              </Button>
            </div>
            <p className="text-xs text-slate-500">Define el precio de cada producto incluido en esta lista.</p>
            {items.map((item, index) => (
              <div key={index} className="grid grid-cols-[1fr_auto_auto] gap-2">
                <Select
                  value={item.productId}
                  onChange={(e) =>
                    setItems((prev) => prev.map((it, i) => (i === index ? { ...it, productId: e.target.value } : it)))
                  }
                  options={productOptions}
                  placeholder="Producto"
                  aria-label={`Producto del ítem ${index + 1}`}
                />
                <Input
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="Precio"
                  className="w-28"
                  value={item.precio}
                  onChange={(e) =>
                    setItems((prev) => prev.map((it, i) => (i === index ? { ...it, precio: e.target.value } : it)))
                  }
                  aria-label={`Precio del ítem ${index + 1}`}
                />
                <Button
                  type="button"
                  size="sm"
                  variant="ghost"
                  disabled={items.length === 1}
                  onClick={() => setItems((prev) => prev.filter((_, i) => i !== index))}
                  aria-label={`Eliminar ítem ${index + 1}`}
                >
                  <Trash2 className="h-4 w-4 text-rose-500" aria-hidden />
                  Eliminar
                </Button>
              </div>
            ))}
          </div>

          <Button type="submit" size="sm" loading={submitting}>
            <Plus className="h-4 w-4" aria-hidden />
            Crear lista
          </Button>
        </form>
      </div>
    </Modal>
  );
}
