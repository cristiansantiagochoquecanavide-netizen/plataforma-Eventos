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

export type EstadoEvento =
  | 'BORRADOR'
  | 'PLANIFICADO'
  | 'PUBLICADO'
  | 'EN_CURSO'
  | 'FINALIZADO'
  | 'CANCELADO';

export interface Categoria {
  id: number;
  nombre: string;
  descripcion: string | null;
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

export interface GuardarCategoriaSolicitud {
  nombre: string;
  descripcion: string | null;
}

export interface ResponsableEvento {
  id: number;
  nombres: string;
  apellidos: string;
  correo: string;
  roles: string[];
}

export interface Evento {
  id: number;
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaFin: string | null;
  lugarPrincipal: string | null;
  categoria: Categoria | null;
  estado: EstadoEvento;
  responsables: ResponsableEvento[];
}

export interface GuardarEventoSolicitud {
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaFin: string | null;
  lugarPrincipal: string | null;
  idCategoria: number;
  estado: EstadoEvento;
  responsables: number[];
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

// --- CU15: Gestion de Convocatorias de Staff y Voluntarios ---
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

export interface ErrorApi {
  status: number;
  message: string;
}
