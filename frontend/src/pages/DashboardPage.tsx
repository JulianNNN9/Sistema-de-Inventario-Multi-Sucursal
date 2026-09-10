import { motion } from 'framer-motion';
import { AlertTriangle, ArrowLeftRight, BarChart3, Boxes, type LucideIcon } from 'lucide-react';
import { Card, PageHeader, Skeleton } from '../components/ui';
import { useAuth } from '../hooks/useAuth';

interface Metric {
  label: string;
  icon: LucideIcon;
}

const PLACEHOLDER_METRICS: Metric[] = [
  { label: 'Ventas del mes', icon: BarChart3 },
  { label: 'Rotación de inventario', icon: Boxes },
  { label: 'Transferencias activas', icon: ArrowLeftRight },
  { label: 'Alertas de reabastecimiento', icon: AlertTriangle },
];

export function DashboardPage() {
  const { usuario } = useAuth();

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Hola, ${usuario ?? ''}`.trim()}
        description="Resumen operativo. Los indicadores en tiempo real se activan con el Módulo 6."
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {PLACEHOLDER_METRICS.map(({ label, icon: Icon }, index) => (
          <motion.div
            key={label}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.2, delay: index * 0.04, ease: 'easeOut' }}
          >
            <Card className="p-5" hover>
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-slate-500">{label}</span>
                <Icon className="h-4 w-4 text-slate-400" aria-hidden />
              </div>
              <Skeleton className="mt-4 h-8 w-24" />
              <Skeleton className="mt-2 h-3 w-32" />
            </Card>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
