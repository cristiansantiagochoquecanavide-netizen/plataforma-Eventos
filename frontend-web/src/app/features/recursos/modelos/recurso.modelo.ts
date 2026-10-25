export type TipoRecurso = 'AUDIOVISUAL' | 'MOBILIARIO' | 'EQUIPO_COMPUTO' | 'ESPACIO' | 'OTRO';
export type EstadoRecurso = 'DISPONIBLE' | 'EN_USO' | 'MANTENIMIENTO' | 'DE_BAJA';

export interface Recurso {
  id: number;
  codigo: string;
  nombre: string;
  tipo: TipoRecurso;
  cantidadTotal: number;
  cantidadDisponible: number;
  estado: EstadoRecurso;
  ubicacion: string | null;
  descripcion: string | null;
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

export interface CrearRecursoSolicitud {
  codigo: string;
  nombre: string;
  tipo: TipoRecurso;
  cantidadTotal: number;
  ubicacion?: string | null;
  descripcion?: string | null;
}

export interface ActualizarRecursoSolicitud {
  codigo: string;
  nombre: string;
  tipo: TipoRecurso;
  cantidadTotal: number;
  cantidadDisponible: number;
  estado: EstadoRecurso;
  ubicacion?: string | null;
  descripcion?: string | null;
}

export interface CambiarEstadoRecursoSolicitud {
  estado: EstadoRecurso;
}

export type EstadoAsignacion = 'ACTIVA' | 'DEVUELTA' | 'CANCELADA';

export interface AsignacionRecurso {
  id: number;
  idEvento: number;
  nombreEvento: string;
  idRecurso: number;
  codigoRecurso: string;
  nombreRecurso: string;
  tipoRecurso: TipoRecurso;
  salaEspacio: string;
  cantidadAsignada: number;
  estado: EstadoAsignacion;
  observaciones: string | null;
  fechaAsignacion: string;
  fechaDevolucion: string | null;
}

export interface CrearAsignacionSolicitud {
  idEvento: number;
  idRecurso: number;
  salaEspacio: string;
  cantidadAsignada: number;
  observaciones?: string | null;
}