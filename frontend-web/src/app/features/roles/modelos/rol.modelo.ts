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