import { useState, type FormEvent } from 'react';
import { Package, Pencil, Plus, Trash2 } from 'lucide-react';
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
} from '../../../shared/components/ui';
import { ErrorAlert } from '../../../shared/components/ErrorAlert';
import { createProduct, deleteProduct, updateProduct } from '../api/productos';
import { useAuth } from '../../auth/hooks/useAuth';
import { useProductos } from '../hooks/useProductos';
import { useMutation } from '../../../shared/hooks/useMutation';
import { useToast } from '../../../shared/context/ToastContext';
import type { Producto } from '../types/producto';

const PAGE_SIZE = 20;

export function ProductosPage() {
  const { rol } = useAuth();
  // El catálogo es un dato maestro: lo mantiene ADMIN_GENERAL/GERENTE_SUCURSAL.
  // OPERADOR_INVENTARIO es de solo lectura aquí.
  const canManage = rol === 'ADMIN_GENERAL' || rol === 'GERENTE_SUCURSAL';

  const [page, setPage] = useState(0);
  const { data, loading, error, refetch } = useProductos({ page, size: PAGE_SIZE });
  const { showSuccess, showError } = useToast();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Producto | null>(null);
  const [sku, setSku] = useState('');
  const [nombre, setNombre] = useState('');
  const [unidad, setUnidad] = useState('');

  const [deleteTarget, setDeleteTarget] = useState<Producto | null>(null);

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
        showSuccess(`Producto "${nombre}" actualizado.`);
      } else {
        await createM.mutate({ sku, nombre, unidadMedidaBase: unidad });
        showSuccess(`Producto "${nombre}" creado.`);
      }
      setFormOpen(false);
      refetch();
    } catch (err) {
      showError((err as { message?: string }).message ?? 'No se pudo guardar el producto.');
      /* el error también se muestra en el modal vía formError */
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    try {
      await deleteM.mutate(deleteTarget.id);
      showSuccess(`Producto "${deleteTarget.nombre}" eliminado.`);
      setDeleteTarget(null);
      refetch();
    } catch (err) {
      showError((err as { message?: string }).message ?? 'No se pudo eliminar el producto.');
      /* el error también se muestra en el ConfirmDialog */
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
      render: (p) =>
        canManage ? (
          <div className="flex justify-end gap-1">
            <Button size="sm" variant="ghost" onClick={() => openEdit(p)} aria-label={`Editar ${p.nombre}`}>
              <Pencil className="h-4 w-4" aria-hidden />
              Editar
            </Button>
            <Button size="sm" variant="ghost" onClick={() => setDeleteTarget(p)} aria-label={`Eliminar ${p.nombre}`}>
              <Trash2 className="h-4 w-4 text-rose-500" aria-hidden />
              Eliminar
            </Button>
          </div>
        ) : null,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Productos"
        description="Catálogo de productos de la sucursal."
        actions={
          canManage ? (
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" aria-hidden />
              Nuevo producto
            </Button>
          ) : undefined
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
            description={
              canManage
                ? 'Crea el primer producto del catálogo para empezar a registrar inventario.'
                : 'Aún no hay productos en el catálogo.'
            }
            action={
              canManage ? (
                <Button onClick={openCreate}>
                  <Plus className="h-4 w-4" aria-hidden />
                  Nuevo producto
                </Button>
              ) : undefined
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
            hint="Código único del producto. No se puede modificar después de crearlo."
            value={sku}
            onChange={(e) => setSku(e.target.value)}
            required
            disabled={editing !== null}
            placeholder="SKU-0001"
          />
          <Input
            label="Nombre"
            hint="Nombre visible del producto en catálogo, inventario y comprobantes."
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
          />
          <Input
            label="Unidad de medida base"
            hint="Unidad en la que se controla el inventario (ej. unidad, kg, litro)."
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
    </div>
  );
}
