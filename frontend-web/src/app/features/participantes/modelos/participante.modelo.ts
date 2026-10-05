export interface ParticipanteInscrito {
  idInscripcion: number;
  idUsuario: number;
  nombres: string;
  apellidos: string;
  correo: string;
  idEvento: number;
  tituloEvento: string;
  fechaInscripcion: string;
  estado: 'CONFIRMADA' | 'CANCELADA' | 'EN_ESPERA';
  codigoQr: string;
}
