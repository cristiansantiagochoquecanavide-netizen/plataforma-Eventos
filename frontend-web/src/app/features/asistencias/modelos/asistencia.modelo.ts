export interface InscripcionElegible {
  idInscripcion: number;
  nombres: string;
  apellidos: string;
  correo: string;
  idEvento: number;
  tituloEvento: string;
  estado: string;
  asistenciaRegistrada: boolean;
  fechaAsistencia: string | null;
}

export interface Asistencia {
  idAsistencia: number;
  idInscripcion: number;
  participante: string;
  correoParticipante: string;
  idEvento: number;
  nombreEvento: string;
  fechaHora: string;
  idRegistradoPor: number;
  nombreRegistradoPor: string;
}
