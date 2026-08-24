export function obtenerMensajeError(error: unknown, respaldo: string): string {
  if (!esObjeto(error) || !('error' in error)) return respaldo;

  const cuerpo = error['error'];
  if (!esObjeto(cuerpo) || !('message' in cuerpo)) return respaldo;

  const mensaje = cuerpo['message'];
  return typeof mensaje === 'string' && mensaje.trim() ? mensaje : respaldo;
}

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null;
}
