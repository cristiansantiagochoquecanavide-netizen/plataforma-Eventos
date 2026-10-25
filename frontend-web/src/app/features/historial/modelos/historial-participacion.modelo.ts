export interface HistorialParticipacion {
  idInscripcion: number;
  idEvento: number;
  nombreEvento: string;
  descripcionEvento?: string;
  categoriaEvento: string;
  fechaInicioEvento: string;
  fechaFinEvento?: string;
  lugarEvento: string;
  fechaInscripcion: string;
  estadoInscripcion: 'CONFIRMADA' | 'CANCELADA' | 'EN_ESPERA';
  asistio: boolean;
  fechaAsistencia?: string;
}
