export type EstadoTransferencia =
  | 'PENDIENTE'
  | 'RECHAZADA'
  | 'EN_TRANSITO'
  | 'COMPLETADA'
  | 'CON_FALTANTES'
  | 'REENVIO_SOLICITADO'
  | 'CERRADA_AJUSTE'
  | 'CERRADA_RECLAMACION';

export type Urgencia = 'BAJA' | 'MEDIA' | 'ALTA';

export type TratamientoFaltante = 'REENVIO' | 'AJUSTE' | 'RECLAMACION';

export interface Transfer {
  id: number;
  productId: number;
  sku: string;
  productoNombre: string;
  sucursalOrigenId: number;
  sucursalOrigenNombre: string;
  sucursalDestinoId: number;
  sucursalDestinoNombre: string;
  cantidadSolicitada: number;
  cantidadEnviada: number | null;
  cantidadRecibida: number | null;
  estado: EstadoTransferencia;
  urgencia: Urgencia;
  transportista: string | null;
  fechaEstimadaLlegada: string | null;
  fechaRealLlegada: string | null;
}

export interface TransferRequestInput {
  productId: number;
  cantidad: number;
  sucursalOrigenId: number;
  urgencia: Urgencia;
  sucursalDestinoId?: number;
}

export interface ApproveInput {
  aprobado: boolean;
}

export interface DispatchInput {
  cantidadEnviada: number;
  transportista: string;
  fechaEstimadaLlegada: string;
}

export interface ReceiveInput {
  cantidadRecibida: number;
}

export interface ResolveInput {
  tratamiento: TratamientoFaltante;
}

export interface TransferEvent {
  id: number;
  estado: EstadoTransferencia;
  fecha: string;
  comentario: string | null;
}
