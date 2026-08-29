export interface SesionRespuesta {
  accessToken: string;
  refreshToken: string;
  tipoToken: string;
}

export interface Usuario {
  id: number;
  nombres: string;
  apellidos: string;
  correo: string;
  estado?: string;
  roles: string[];
}

export interface PaginaUsuarios {
  elementos: Usuario[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface Rol {
  id: number;
  nombre: string;
  descripcion: string | null;
  estado: 'ACTIVO' | 'INACTIVO';
}

export interface Permiso {
  id: number;
  nombre: string;
  descripcion: string | null;
}

export interface RolPermisos {
  idRol: number;
  nombreRol: string;
  permisos: Permiso[];
}

// --- CU19: Modelos de Recursos ---
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

// --- CU20: Modelos de Eventos y Asignación de Recursos ---
export type EstadoEvento = 'BORRADOR' | 'PUBLICADO' | 'EN_CURSO' | 'FINALIZADO' | 'CANCELADO';

export interface Evento {
  id: number;
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaFin: string;
  lugarPrincipal: string | null;
  estado: EstadoEvento;
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

export interface ErrorApi {
  status: number;
  message: string;
}
