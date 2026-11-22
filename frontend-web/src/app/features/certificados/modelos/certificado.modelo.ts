export interface CertificadoRespuesta {
  idCertificado: number;
  idInscripcion: number;
  idEvento: number;
  tituloEvento: string;
  idParticipante: number;
  nombreParticipante: string;
  correoParticipante: string;
  codigoVerificacion: string;
  hashSeguridad: string;
  fechaEmision: string;
  idOrganizacion: number;
  nombreOrganizacion: string;
}

export interface VerificacionCertificadoRespuesta {
  valido: boolean;
  codigoVerificacion?: string;
  nombreParticipante?: string;
  tituloEvento?: string;
  nombreOrganizacion?: string;
  fechaEmision?: string;
  fechaEvento?: string;
  hashSeguridad?: string;
  mensaje: string;
}
