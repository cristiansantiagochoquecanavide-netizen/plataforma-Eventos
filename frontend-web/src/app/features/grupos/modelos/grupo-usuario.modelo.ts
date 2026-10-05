export interface MiembroGrupo {
  idUsuario: number;
  nombres: string;
  apellidos: string;
  correo: string;
}

export interface RolGrupo {
  idRol: number;
  nombre: string;
}

export interface GrupoUsuario {
  id: number;
  idOrganizacion: number;
  nombre: string;
  descripcion: string | null;
  activo: boolean;
  miembros: MiembroGrupo[];
  roles: RolGrupo[];
}

export interface GrupoUsuarioFormulario {
  nombre: string;
  descripcion: string;
}
