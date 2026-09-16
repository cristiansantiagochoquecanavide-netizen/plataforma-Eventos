export type EstadoPostulacionStaff = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA';

export interface PostulacionStaff {
  id: number;
  idConvocatoria: number;
  idUsuario: number;
  estado: EstadoPostulacionStaff;
  motivacion: string;
  fechaPostulacion: string;
}

export interface CrearPostulacionStaffSolicitud { motivacion: string; }
