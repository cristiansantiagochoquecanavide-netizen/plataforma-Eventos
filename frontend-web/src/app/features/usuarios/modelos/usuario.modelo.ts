export interface Usuario {
  id: number;
  nombres: string;
  apellidos: string;
  correo: string;
  estado?: string;
  roles: string[];
  permisos?: string[];
}

export interface PaginaUsuarios {
  elementos: Usuario[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}