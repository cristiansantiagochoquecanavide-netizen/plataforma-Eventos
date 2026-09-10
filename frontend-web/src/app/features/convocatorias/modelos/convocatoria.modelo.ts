export type TipoPersonalConvocatoria = 'STAFF' | 'VOLUNTARIO';
export type EstadoConvocatoria = 'ABIERTA' | 'CERRADA' | 'CANCELADA';

export interface Convocatoria {
  id: number;
  idEvento: number;
  nombreEvento: string;
  tipoPersonal: TipoPersonalConvocatoria;
  descripcion: string;
  cantidadRequerida: number;
  fechaLimite: string;
  estado: EstadoConvocatoria;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface CrearConvocatoriaSolicitud {
  idEvento: number;
  tipoPersonal: TipoPersonalConvocatoria;
  descripcion: string;
  cantidadRequerida: number;
  fechaLimite: string;
}