import { motion } from 'framer-motion';
import { Building2, Users, type LucideIcon } from 'lucide-react';
import { Card, PageHeader } from '../components/ui';

interface AdminSection {
  title: string;
  description: string;
  icon: LucideIcon;
  endpoint: string;
}

const SECTIONS: AdminSection[] = [
  {
    title: 'Usuarios',
    description: 'Alta y edición de usuarios, con asignación de rol y sucursal.',
    icon: Users,
    endpoint: '/api/v1/users',
  },
  {
    title: 'Sucursales',
    description: 'Alta y consulta de los nodos de la red.',
    icon: Building2,
    endpoint: '/api/v1/branches',
  },
];

export function AdminPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Administración"
        description="Gestión de usuarios y sucursales. Restringido al rol ADMIN_GENERAL."
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {SECTIONS.map(({ title, description, icon: Icon, endpoint }, index) => (
          <motion.div
            key={title}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.2, delay: index * 0.05, ease: 'easeOut' }}
          >
            <Card hover className="h-full">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
                <Icon className="h-5 w-5" aria-hidden />
              </div>
              <h3 className="mt-4 text-base font-semibold text-slate-900">{title}</h3>
              <p className="mt-1 text-sm text-slate-500">{description}</p>
              <code className="mt-3 inline-block rounded-md bg-slate-100 px-2 py-1 text-xs text-slate-600">
                {endpoint}
              </code>
            </Card>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
