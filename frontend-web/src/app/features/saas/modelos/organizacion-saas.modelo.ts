export type EstadoOrganizacion = 'ACTIVA' | 'INACTIVA';
export type PlanSaas = 'BASICO' | 'PROFESIONAL' | 'EMPRESARIAL';
export type EstadoSuscripcionSaas = 'ACTIVA' | 'SUSPENDIDA' | 'VENCIDA';

export interface OrganizacionSaas {
  id: number;
  nombre: string;
  slug: string;
  estado: EstadoOrganizacion;
  fechaCreacion: string;
  miembrosActivos: number;
  plan: PlanSaas | null;
  estadoSuscripcion: EstadoSuscripcionSaas | null;
  fechaInicio: string | null;
  fechaFin: string | null;
  suscripcionVigente: boolean;
}

export interface PaginaOrganizacionesSaas {
  organizaciones: OrganizacionSaas[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}
