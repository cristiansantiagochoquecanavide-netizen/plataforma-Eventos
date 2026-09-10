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