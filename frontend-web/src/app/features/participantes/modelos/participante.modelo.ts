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

export type ColumnaReporteParticipantes =
  | 'ID'
  | 'NOMBRES'
  | 'APELLIDOS'
  | 'CORREO'
  | 'ESTADO'
  | 'TOTAL_INSCRIPCIONES'
  | 'INSCRIPCIONES_CONFIRMADAS'
  | 'INSCRIPCIONES_CANCELADAS'
  | 'ULTIMA_INSCRIPCION'
  | 'FECHA_REGISTRO';

export type FormatoReporteParticipantes = 'CSV' | 'XLSX' | 'HTML' | 'PDF';

export interface ReporteParticipantesSolicitud {
  eventoId: number | null;
  busqueda: string;
  estado: 'ACTIVO' | 'INACTIVO' | null;
  columnas: ColumnaReporteParticipantes[];
  ordenarPor: ColumnaReporteParticipantes | null;
  ascendente: boolean;
}

export interface VistaPreviaReporteParticipantes {
  columnas: string[];
  filas: string[][];
  totalFilas: number;
  limiteAplicado: boolean;
}
