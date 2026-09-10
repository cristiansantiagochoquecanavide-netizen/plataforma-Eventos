export * from '../features/auth/modelos/auth.modelo';
export * from '../features/usuarios/modelos/usuario.modelo';
export * from '../features/roles/modelos/rol.modelo';
export * from '../features/categorias/modelos/categoria.modelo';
export * from '../features/espacios/modelos/espacio.modelo';
export * from '../features/eventos/modelos/evento.modelo';
export * from '../features/recursos/modelos/recurso.modelo';
export * from '../features/convocatorias/modelos/convocatoria.modelo';

export interface ErrorApi {
  status: number;
  message: string;
}