import { TipoPersonalConvocatoria } from './convocatoria.modelo';

export type EstadoPostulacion = 'PENDIENTE' | 'RETIRADA' | 'SELECCIONADA' | 'RECHAZADA';

export interface PostulanteConvocatoria {
  id: number;
  idUsuario: number;
  nombres: string;
  apellidos: string;
  correo: string;
  idConvocatoria: number;
  nombreEvento: string;
  tipoPersonal: TipoPersonalConvocatoria;
  estado: EstadoPostulacion;
  fechaPostulacion: string;
  fechaActualizacion: string;
}
