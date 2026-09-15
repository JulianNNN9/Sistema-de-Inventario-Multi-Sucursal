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
  costo: number;
  estado: EstadoTransferencia;
  urgencia: Urgencia;
  transportista: string | null;
  fechaEstimadaLlegada: string | null;
  fechaRealLlegada: string | null;
  /** El modelo no tiene un estado "APROBADA" propio: indica si, estando PENDIENTE, ya fue aprobada y está lista para despacho. */
  aprobada: boolean;
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
  costo: number;
}

export interface ReceiveInput {
  cantidadRecibida: number;
}

export interface ResolveInput {
  tratamiento: TratamientoFaltante;
  /** PQRS: qué pasó con el faltante y por qué se resuelve así; queda en el historial. */
  detalle: string;
}

export interface TransferEvent {
  id: number;
  estado: EstadoTransferencia;
  fecha: string;
  comentario: string | null;
}
