import { useMemo, useState, type FormEvent } from 'react';
import { motion } from 'framer-motion';
import { Building2, Pencil, Plus, Users } from 'lucide-react';
import { Button, Card, Input, Modal, PageHeader, Select } from '../components/ui';
import { ErrorAlert } from '../components/ErrorAlert';
import { createBranch } from '../api/sucursales';
import { createUser, updateUser } from '../api/usuarios';
import { useBranches } from '../hooks/useBranches';
import { useMutation } from '../hooks/useMutation';
import { useUsuarios } from '../hooks/useUsuarios';
import { useToast } from '../context/ToastContext';
import { cn } from '../lib/cn';
import { ROL_LABEL, ROL_OPTIONS, type Rol } from '../types/auth';
import type { Usuario } from '../types/usuario';

export function AdminPage() {
  const [usersOpen, setUsersOpen] = useState(false);
  const [branchesOpen, setBranchesOpen] = useState(false);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Administración"
        description="Alta y edición de usuarios, y alta de sucursales de la red."
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
          <Card hover className="h-full cursor-pointer" onClick={() => setUsersOpen(true)}>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
              <Users className="h-5 w-5" aria-hidden />
            </div>
            <h3 className="mt-4 text-base font-semibold text-slate-900">Usuarios</h3>
            <p className="mt-1 text-sm text-slate-500">
              Crea nuevas personas con acceso al sistema y edita su rol o sucursal.
            </p>
            <Button size="sm" variant="secondary" className="mt-4" onClick={() => setUsersOpen(true)}>
              Gestionar usuarios
            </Button>
          </Card>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2, delay: 0.05 }}
        >
          <Card hover className="h-full cursor-pointer" onClick={() => setBranchesOpen(true)}>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
              <Building2 className="h-5 w-5" aria-hidden />
            </div>
            <h3 className="mt-4 text-base font-semibold text-slate-900">Sucursales</h3>
            <p className="mt-1 text-sm text-slate-500">
              Agrega nuevos puntos de venta o bodegas a la red.
            </p>
            <Button size="sm" variant="secondary" className="mt-4" onClick={() => setBranchesOpen(true)}>
              Gestionar sucursales
            </Button>
          </Card>
        </motion.div>
      </div>

      <UsersModal open={usersOpen} onClose={() => setUsersOpen(false)} />
      <BranchesModal open={branchesOpen} onClose={() => setBranchesOpen(false)} />
    </div>
  );
}

interface UsersModalProps {
  open: boolean;
  onClose: () => void;
}

function UsersModal({ open, onClose }: UsersModalProps) {
  const { usuarios, loading, refetch } = useUsuarios();
  const { branches } = useBranches();
  const { showSuccess, showError } = useToast();
  const createM = useMutation(createUser);
  const updateM = useMutation(updateUser);

  const [editing, setEditing] = useState<Usuario | null>(null);
  const [busqueda, setBusqueda] = useState('');
  const [nombre, setNombre] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rol, setRol] = useState<Rol>('OPERADOR_INVENTARIO');
  const [sucursalId, setSucursalId] = useState('');

  const mutation = editing ? updateM : createM;
  const necesitaSucursal = rol !== 'ADMIN_GENERAL';

  const usuariosFiltrados = useMemo(
    () => usuarios.filter((u) => u.nombre.toLowerCase().includes(busqueda.trim().toLowerCase())),
    [usuarios, busqueda],
  );

  const branchOptions = branches.map((b) => ({ value: b.id, label: b.nombre }));

  function startCreate() {
    setEditing(null);
    setNombre('');
    setEmail('');
    setPassword('');
    setRol('OPERADOR_INVENTARIO');
    setSucursalId('');
    createM.resetError();
    updateM.resetError();
  }

  function startEdit(usuario: Usuario) {
    setEditing(usuario);
    setNombre(usuario.nombre);
    setEmail(usuario.email);
    setPassword('');
    setRol(usuario.rol);
    setSucursalId(usuario.sucursalId ? String(usuario.sucursalId) : '');
    createM.resetError();
    updateM.resetError();
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      if (editing) {
        await updateM.mutate(editing.id, {
          nombre,
          rol,
          sucursalId: necesitaSucursal && sucursalId ? Number(sucursalId) : undefined,
          password: password || undefined,
        });
        showSuccess(`Usuario "${nombre}" actualizado.`);
      } else {
        await createM.mutate({
          nombre,
          email,
          password,
          rol,
          sucursalId: necesitaSucursal && sucursalId ? Number(sucursalId) : undefined,
        });
        showSuccess(`Usuario "${nombre}" creado.`);
      }
      startCreate();
      refetch();
    } catch (err) {
      showError((err as { message?: string }).message ?? 'No se pudo guardar el usuario.');
      /* el error también se muestra en el formulario */
    }
  }

  return (
    <Modal
      open={open}
      onClose={() => {
        startCreate();
        setBusqueda('');
        onClose();
      }}
      title="Usuarios"
      className="max-w-lg"
    >
      <div className="space-y-4">
        <Input
          label="Buscar por nombre"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          placeholder="Escribe el nombre de la persona…"
        />

        {loading && <p className="text-sm text-slate-500">Cargando…</p>}
        {!loading && usuariosFiltrados.length > 0 && (
          <ul className="max-h-56 divide-y divide-slate-100 overflow-y-auto rounded-xl border border-slate-200">
            {usuariosFiltrados.map((u) => (
              <li key={u.id}>
                <button
                  type="button"
                  onClick={() => startEdit(u)}
                  className={cn(
                    'flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm transition-colors hover:bg-slate-50',
                    editing?.id === u.id && 'bg-brand-50',
                  )}
                >
                  <span className="min-w-0 truncate font-medium text-slate-800">{u.nombre}</span>
                  <span className="flex shrink-0 items-center gap-1.5 text-xs text-slate-500">
                    {ROL_LABEL[u.rol]}
                    {u.sucursalNombre ? ` · ${u.sucursalNombre}` : ''}
                    <Pencil className="h-3 w-3" aria-hidden />
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {!loading && usuariosFiltrados.length === 0 && (
          <p className="rounded-xl border border-dashed border-slate-200 px-3 py-4 text-center text-sm text-slate-500">
            {usuarios.length === 0 ? 'Aún no hay usuarios registrados.' : 'Ninguna persona coincide con la búsqueda.'}
          </p>
        )}

        <form onSubmit={handleSubmit} className="space-y-3 border-t border-slate-100 pt-4">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-slate-700">
              {editing ? `Editando a "${editing.nombre}"` : 'Nuevo usuario'}
            </span>
            {editing && (
              <Button type="button" size="sm" variant="ghost" onClick={startCreate}>
                Cancelar edición
              </Button>
            )}
          </div>
          {mutation.error && <ErrorAlert message={mutation.error} />}
          <Input
            label="Nombre completo"
            hint="Nombre de la persona que iniciará sesión en el sistema."
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
          />
          <Input
            label="Correo electrónico"
            hint={
              editing
                ? 'El correo de acceso no se puede modificar.'
                : 'Será el usuario con el que esta persona inicie sesión. No podrá cambiarse después.'
            }
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={editing !== null}
            required={editing === null}
          />
          <Input
            label={editing ? 'Nueva contraseña (opcional)' : 'Contraseña'}
            hint={
              editing
                ? 'Déjala en blanco para mantener la contraseña actual.'
                : 'Contraseña inicial de acceso, de al menos 8 caracteres.'
            }
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required={editing === null}
          />
          <Select
            label="Rol"
            hint="Define qué puede ver y hacer esta persona dentro del sistema."
            value={rol}
            onChange={(e) => {
              const nuevoRol = e.target.value as Rol;
              setRol(nuevoRol);
              if (nuevoRol === 'ADMIN_GENERAL') setSucursalId('');
            }}
            options={ROL_OPTIONS}
          />
          {necesitaSucursal && (
            <Select
              label="Sucursal"
              hint="Sucursal a la que queda asignada esta persona."
              value={sucursalId}
              onChange={(e) => setSucursalId(e.target.value)}
              options={branchOptions}
              placeholder="Selecciona una sucursal"
              required
            />
          )}
          <Button type="submit" size="sm" loading={mutation.submitting}>
            {editing ? (
              'Guardar cambios'
            ) : (
              <>
                <Plus className="h-4 w-4" aria-hidden />
                Crear usuario
              </>
            )}
          </Button>
        </form>
      </div>
    </Modal>
  );
}

interface BranchesModalProps {
  open: boolean;
  onClose: () => void;
}

function BranchesModal({ open, onClose }: BranchesModalProps) {
  const { branches, loading, refetch } = useBranches();
  const { showSuccess, showError } = useToast();
  const { mutate, submitting, error, resetError } = useMutation(createBranch);
  const [busqueda, setBusqueda] = useState('');
  const [nombre, setNombre] = useState('');
  const [ciudad, setCiudad] = useState('');

  const branchesFiltradas = useMemo(
    () => branches.filter((b) => b.nombre.toLowerCase().includes(busqueda.trim().toLowerCase())),
    [branches, busqueda],
  );

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      await mutate({ nombre, ciudad: ciudad || undefined });
      showSuccess(`Sucursal "${nombre}" creada.`);
      setNombre('');
      setCiudad('');
      resetError();
      refetch();
    } catch (err) {
      showError((err as { message?: string }).message ?? 'No se pudo crear la sucursal.');
      /* el error también se muestra en el formulario */
    }
  }

  return (
    <Modal
      open={open}
      onClose={() => {
        setBusqueda('');
        onClose();
      }}
      title="Sucursales"
      className="max-w-lg"
    >
      <div className="space-y-4">
        <Input
          label="Buscar por nombre"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          placeholder="Escribe el nombre de la sucursal…"
        />

        {loading && <p className="text-sm text-slate-500">Cargando…</p>}
        {!loading && branchesFiltradas.length > 0 && (
          <ul className="max-h-56 divide-y divide-slate-100 overflow-y-auto rounded-xl border border-slate-200">
            {branchesFiltradas.map((b) => (
              <li key={b.id} className="flex items-center justify-between px-3 py-2 text-sm">
                <span className="font-medium text-slate-800">{b.nombre}</span>
                <span className="text-xs text-slate-500">{b.ciudad ?? 'Sin ciudad registrada'}</span>
              </li>
            ))}
          </ul>
        )}
        {!loading && branchesFiltradas.length === 0 && (
          <p className="rounded-xl border border-dashed border-slate-200 px-3 py-4 text-center text-sm text-slate-500">
            {branches.length === 0 ? 'Aún no hay sucursales registradas.' : 'Ninguna sucursal coincide con la búsqueda.'}
          </p>
        )}

        <form onSubmit={handleSubmit} className="space-y-3 border-t border-slate-100 pt-4">
          {error && <ErrorAlert message={error} />}
          <Input
            label="Nombre"
            hint="Nombre con el que se identificará esta sucursal en todo el sistema."
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            required
          />
          <Input
            label="Ciudad (opcional)"
            hint="Ciudad donde está ubicada esta sucursal."
            value={ciudad}
            onChange={(e) => setCiudad(e.target.value)}
          />
          <Button type="submit" size="sm" loading={submitting}>
            <Plus className="h-4 w-4" aria-hidden />
            Crear sucursal
          </Button>
        </form>
      </div>
    </Modal>
  );
}
