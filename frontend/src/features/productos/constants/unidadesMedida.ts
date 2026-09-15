import type { SelectOption } from '../../../shared/components/ui';

/** Debe reflejar UnidadesMedida.VALORES del backend. */
export const UNIDADES_MEDIDA_OPTIONS: SelectOption[] = [
  { value: 'unidad', label: 'Unidad' },
  { value: 'kg', label: 'Kilogramo (kg)' },
  { value: 'g', label: 'Gramo (g)' },
  { value: 'litro', label: 'Litro (L)' },
  { value: 'ml', label: 'Mililitro (ml)' },
  { value: 'metro', label: 'Metro (m)' },
  { value: 'caja', label: 'Caja' },
  { value: 'paquete', label: 'Paquete' },
  { value: 'par', label: 'Par' },
  { value: 'galón', label: 'Galón' },
  { value: 'docena', label: 'Docena' },
  { value: 'rollo', label: 'Rollo' },
];
