export type EstadoAsignacionStaff = 'ASIGNADA' | 'CANCELADA';

export interface AsignacionStaff {
  id: number;
  idPostulacion: number;
  idUsuario: number;
  tarea: string;
  turnoInicio: string | null;
  turnoFin: string | null;
  estado: EstadoAsignacionStaff;
}

export interface CrearAsignacionStaffSolicitud {
  idPostulacion: number;
  tarea: string;
  turnoInicio: string | null;
  turnoFin: string | null;
  observaciones: string;
}
