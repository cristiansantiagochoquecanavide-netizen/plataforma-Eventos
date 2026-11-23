import { Categoria } from '../../categorias/modelos/categoria.modelo';
import { Espacio } from '../../espacios/modelos/espacio.modelo';

export type EstadoEvento =
  | 'BORRADOR'
  | 'PLANIFICADO'
  | 'PUBLICADO'
  | 'EN_CURSO'
  | 'FINALIZADO'
  | 'CANCELADO';

export interface ResponsableEvento {
  id: number;
  nombres: string;
  apellidos: string;
  correo: string;
  roles: string[];
}

export interface TipoEntrada {
  id?: number;
  nombre: string;
  precio: number;
  cupoMaximo: number;
  descripcion?: string;
  entradasVendidas?: number;
  cuposDisponibles?: number;
}

export interface Evento {
  id: number;
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaFin: string | null;
  lugarPrincipal: string | null;
  categoria: Categoria | null;
  categoriaNombre: string | null;
  espacio: Espacio | null;
  aforoMaximo: number | null;
  estado: EstadoEvento;
  responsables: ResponsableEvento[];
  tiposEntrada?: TipoEntrada[];
}

export interface GuardarEventoSolicitud {
  nombre: string;
  descripcion: string | null;
  fechaInicio: string;
  fechaFin: string | null;
  lugarPrincipal: string | null;
  idCategoria: number;
  idEspacio: number;
  aforoMaximo: number;
  estado: EstadoEvento;
  responsables?: number[];
  tiposEntrada?: TipoEntrada[];
}
