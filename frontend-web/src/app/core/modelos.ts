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
  estado: 'PLANIFICADO' | 'CANCELADO' | 'FINALIZADO';
  responsables: ResponsableEvento[];
}

export interface ErrorApi {
  status: number;
  message: string;
}
