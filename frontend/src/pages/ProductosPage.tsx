import { useEffect, useState, type FormEvent } from 'react';
import { Package, Pencil, Plus, Ruler, Trash2 } from 'lucide-react';
import {
  Button,
  ConfirmDialog,
  DataTable,
  EmptyState,
  Input,
  Modal,
  PageHeader,
  Pagination,
  type Column,
} from '../components/ui';
import { ErrorAlert } from '../components/ErrorAlert';
import {
  addProductUnit,
  createProduct,
  deleteProduct,
  updateProduct,
} from '../api/productos';
import { useProductos } from '../hooks/useProductos';
import { useProductUnits } from '../hooks/useProductUnits';
import { useMutation } from '../hooks/useMutation';
import { formatNumber } from '../lib/format';
import type { Producto } from '../types/producto';

const PAGE_SIZE = 20;

export function ProductosPage() {
  const [page, setPage] = useState(0);
  const { data, loading, error, refetch } = useProductos({ page, size: PAGE_SIZE });

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Producto | null>(null);
  const [sku, setSku] = useState('');
  const [nombre, setNombre] = useState('');
  const [unidad, setUnidad] = useState('');

  const [deleteTarget, setDeleteTarget] = useState<Producto | null>(null);
  const [unitsTarget, setUnitsTarget] = useState<Producto | null>(null);

  const createM = useMutation(createProduct);
  const updateM = useMutation(updateProduct);
  const deleteM = useMutation(deleteProduct);
  const formError = editing ? updateM.error : createM.error;
  const formSubmitting = editing ? updateM.submitting : createM.submitting;

  function openCreate() {
    setEditing(null);
    setSku('');
    setNombre('');
    setUnidad('');
    createM.resetError();
    setFormOpen(true);
  }

  function openEdit(producto: Producto) {
    setEditing(producto);
    setSku(producto.sku);
    setNombre(producto.nombre);
    setUnidad(producto.unidadMedidaBase);
    updateM.resetError();
    setFormOpen(true);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      if (editing) {
        await updateM.mutate(editing.id, { nombre, unidadMedidaBase: unidad });
      } else {
        await createM.mutate({ sku, nombre, unidadMedidaBase: unidad });
      }
      setFormOpen(false);
      refetch();
    } catch {
      /* el error se muestra en el modal vía formError */
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    try {
      await deleteM.mutate(deleteTarget.id);
      setDeleteTarget(null);
      refetch();
    } catch {
      /* el error se muestra en el ConfirmDialog */
    }
  }

  const columns: Column<Producto>[] = [
    { key: 'sku', header: 'SKU', render: (p) => <span className="font-medium text-slate-900">{p.sku}</span> },
    { key: 'nombre', header: 'Nombre', render: (p) => p.nombre },
    { key: 'unidad', header: 'Unidad base', render: (p) => p.unidadMedidaBase },
    {
      key: 'acciones',
      header: '',
      align: 'right',
      render: (p) => (
        <div className="flex justify-end gap-1">
          <Button size="sm" variant="ghost" onClick={() => setUnitsTarget(p)} aria-label={`Unidades de ${p.nombre}`}>
            <Ruler className="h-4 w-4" aria-hidden />
          </Button>
          <Button size="sm" variant="ghost" onClick={() => openEdit(p)} aria-label={`Editar ${p.nombre}`}>
            <Pencil className="h-4 w-4" aria-hidden />
          </Button>
          <Button size="sm" variant="ghost" onClick={() => setDeleteTarget(p)} aria-label={`Eliminar ${p.nombre}`}>
            <Trash2 className="h-4 w-4 text-rose-500" aria-hidden />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Productos"
        description="Catálogo de productos y sus unidades de medida."
        actions={
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" aria-hidden />
            Nuevo producto
          </Button>
        }
      />

      {error && <ErrorAlert message={error} />}

      <DataTable
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(p) => p.id}
        loading={loading}
        empty={
          <EmptyState
            icon={Package}
            title="Sin productos"
            description="Crea el primer producto del catálogo para empezar a registrar inventario."
            action={
              <Button onClick={openCreate}>
                <Plus className="h-4 w-4" aria-hidden />
                Nuevo producto
              </Button>
            }
          />
        }
      />

      {data && (
        <Pagination page={data.page} totalPages={data.totalPages} onPageChange={setPage} />
      )}

      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? 'Editar producto' : 'Nuevo producto'}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)} disabled={formSubmitting}>
              Cancelar
            </Button>
            <Button type="submit" form="producto-form" loading={formSubmitting}>
              {editing ? 'Guardar' : 'Crear'}
            </Button>
          </>
        }
      >
        <form id="producto-form" onSubmit={handleSubmit} className="space-y-4">
          {formError && <ErrorAlert message={formError} />}
          <Input
            label="SKU"
            value={sku}
            onChange={(e) => setSku(e.target.value)}
            required
            disabled={editing !== null}
            placeholder="SKU-0001"
          />
          <Input
            label="Nombre"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
          />
          <Input
            label="Unidad de medida base"
            value={unidad}
            onChange={(e) => setUnidad(e.target.value)}
            required
            placeholder="unidad, kg, litro…"
          />
        </form>
      </Modal>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Eliminar producto"
        message={`¿Eliminar "${deleteTarget?.nombre ?? ''}"? Esta acción no se puede deshacer.`}
        confirmLabel="Eliminar"
        loading={deleteM.submitting}
        error={deleteM.error}
        onConfirm={confirmDelete}
        onCancel={() => {
          setDeleteTarget(null);
          deleteM.resetError();
        }}
      />

      <UnitsModal
        producto={unitsTarget}
        onClose={() => setUnitsTarget(null)}
        addUnit={addProductUnit}
      />
    </div>
  );
}

interface UnitsModalProps {
  producto: Producto | null;
  onClose: () => void;
  addUnit: typeof addProductUnit;
}

function UnitsModal({ producto, onClose, addUnit }: UnitsModalProps) {
  const { units, loading, error, refetch } = useProductUnits(producto?.id ?? null);
  const addM = useMutation(addUnit);
  const [nombreUnidad, setNombreUnidad] = useState('');
  const [factor, setFactor] = useState('');

  useEffect(() => {
    setNombreUnidad('');
    setFactor('');
    addM.resetError();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [producto?.id]);

  async function handleAdd(event: FormEvent) {
    event.preventDefault();
    if (!producto) return;
    try {
      await addM.mutate(producto.id, {
        nombreUnidad,
        factorConversion: Number(factor),
      });
      setNombreUnidad('');
      setFactor('');
      refetch();
    } catch {
      /* addM.error */
    }
  }

  return (
    <Modal
      open={producto !== null}
      onClose={onClose}
      title={producto ? `Unidades · ${producto.nombre}` : 'Unidades'}
    >
      <div className="space-y-4">
        <p className="text-xs text-slate-500">
          Unidad base: <span className="font-medium text-slate-700">{producto?.unidadMedidaBase}</span>.
          El factor indica cuántas unidades base equivalen a 1 de la nueva unidad.
        </p>

        {loading && <p className="text-sm text-slate-500">Cargando unidades…</p>}
        {error && <ErrorAlert message={error} />}

        {!loading && units.length > 0 && (
          <ul className="divide-y divide-slate-100 rounded-xl border border-slate-200">
            {units.map((u) => (
              <li key={u.id} className="flex items-center justify-between px-3 py-2 text-sm">
                <span className="font-medium text-slate-800">{u.nombreUnidad}</span>
                <span className="text-slate-500">× {formatNumber(u.factorConversion)}</span>
              </li>
            ))}
          </ul>
        )}
        {!loading && units.length === 0 && !error && (
          <p className="rounded-xl border border-dashed border-slate-200 px-3 py-4 text-center text-sm text-slate-500">
            Este producto solo usa su unidad base.
          </p>
        )}

        <form onSubmit={handleAdd} className="space-y-3 border-t border-slate-100 pt-4">
          {addM.error && <ErrorAlert message={addM.error} />}
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Nombre de unidad"
              value={nombreUnidad}
              onChange={(e) => setNombreUnidad(e.target.value)}
              required
              placeholder="caja"
            />
            <Input
              label="Factor de conversión"
              type="number"
              step="0.0001"
              min="0"
              value={factor}
              onChange={(e) => setFactor(e.target.value)}
              required
              placeholder="12"
            />
          </div>
          <Button type="submit" size="sm" loading={addM.submitting}>
            <Plus className="h-4 w-4" aria-hidden />
            Agregar unidad
          </Button>
        </form>
      </div>
    </Modal>
  );
}
