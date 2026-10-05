export interface InscripcionResumen {
  idInscripcion: number;
  idEvento: number;
  tituloEvento: string;
  fechaInicioEvento: string;
  fechaFinEvento?: string;
  fechaInscripcion: string;
  codigoQr: string;
  estado: 'CONFIRMADA' | 'CANCELADA' | 'EN_ESPERA';
}

export interface ParticipanteResumen {
  idUsuario: number;
  nombres: string;
  apellidos: string;
  correo: string;
  estado: 'ACTIVO' | 'INACTIVO';
  fechaRegistro: string;
  totalInscripciones: number;
  inscripcionesConfirmadas: number;
  inscripcionesCanceladas: number;
  ultimaInscripcion?: string;
}

export interface ParticipanteDetalle {
  idUsuario: number;
  nombres: string;
  apellidos: string;
  correo: string;
  estado: 'ACTIVO' | 'INACTIVO';
  fechaRegistro: string;
  totalInscripciones: number;
  inscripcionesConfirmadas: number;
  inscripcionesCanceladas: number;
  inscripciones: InscripcionResumen[];
}

export interface PaginaParticipantes {
  elementos: ParticipanteResumen[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}
