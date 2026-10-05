export interface Organizacion {
  id: number;
  nombre: string;
  slug: string;
  estado: string;
}

export interface Membresia {
  idUsuario: number;
  nombres: string;
  apellidos: string;
  correo: string;
  activo: boolean;
  cuentaActiva: boolean;
  roles: string[];
}

export interface AgregarMembresiaSolicitud {
  idUsuario: number;
  roles: string[];
}
