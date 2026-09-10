export interface Espacio {
  id: number;
  nombre: string;
  ubicacion: string;
  capacidad: number;
}

export interface GuardarEspacioSolicitud {
  nombre: string;
  ubicacion: string;
  capacidad: number;
}