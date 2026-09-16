export interface Bitacora {
  id: number; organizacion: number; actor: number | null; accion: string; modulo: string;
  entidad: string; idEntidad: number | null; fecha: string; ip: string | null; exito: boolean; resultado: string;
}
export interface PaginaBitacora { content: Bitacora[]; number: number; size: number; totalElements: number; totalPages: number; }
export interface FiltrosBitacora { modulo?: string; entidad?: string; accion?: string; exito?: boolean; page: number; size: number; }
