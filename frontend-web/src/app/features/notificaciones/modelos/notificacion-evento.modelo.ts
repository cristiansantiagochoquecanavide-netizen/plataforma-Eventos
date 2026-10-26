export interface NotificacionEvento {
  id: number;
  idEvento: number;
  evento: string;
  idInscripcion: number;
  destinatario: string;
  tipo: string;
  canal: string;
  asunto: string;
  fechaProgramada: string;
  fechaEnvio: string | null;
  estado: 'PENDIENTE' | 'PROCESANDO' | 'ENVIADO' | 'FALLIDO' | 'CANCELADO';
  idMensajeExterno: string | null;
  ultimoError: string | null;
  intentos: number;
}

export interface RecordatorioMasivoSolicitud {
  fechaProgramada: string;
  asunto: string;
  mensaje: string;
}

export interface RecordatorioMasivoRespuesta {
  idEvento: number;
  destinatarios: number;
  fechaProgramada: string;
}
