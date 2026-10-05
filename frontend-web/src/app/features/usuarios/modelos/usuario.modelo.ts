export interface PerfilOrganizacionActiva {
  idOrganizacion: number;
  roles: string[];
  permisos: string[];
  administrador: boolean;
}

export interface Usuario {
  id: number;
  nombres: string;
  apellidos: string;
  correo: string;
  estado?: string;
  roles: string[];
  permisos?: string[];
  perfilOrganizacionActiva?: PerfilOrganizacionActiva | null;
}

export interface PaginaUsuarios {
  elementos: Usuario[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}