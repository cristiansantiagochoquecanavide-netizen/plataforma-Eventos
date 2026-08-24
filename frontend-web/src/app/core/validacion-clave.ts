import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export type IdRequisitoClave = 'longitud' | 'mayuscula' | 'minuscula' | 'numeros';

export interface RequisitoClave {
  id: IdRequisitoClave;
  texto: string;
  valido: boolean;
}

export const MENSAJE_CLAVE_SEGURA =
  'La contrasena debe tener minimo 8 caracteres, una mayuscula, una minuscula y al menos 2 numeros.';

export function obtenerRequisitosClave(clave: string): RequisitoClave[] {
  const numeros = clave.match(/\d/g)?.length ?? 0;

  return [
    {
      id: 'longitud',
      texto: 'Tiene minimo 8 caracteres.',
      valido: clave.length >= 8,
    },
    {
      id: 'mayuscula',
      texto: 'Tiene una mayuscula.',
      valido: /[A-Z]/.test(clave),
    },
    {
      id: 'minuscula',
      texto: 'Tiene una minuscula.',
      valido: /[a-z]/.test(clave),
    },
    {
      id: 'numeros',
      texto: 'Tiene al menos 2 numeros.',
      valido: numeros >= 2,
    },
  ];
}

export function claveCumplePolitica(clave: string): boolean {
  return obtenerRequisitosClave(clave).every((requisito) => requisito.valido);
}

export function validadorClaveSegura(): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const clave = control.value;
    if (!clave) return null;
    return claveCumplePolitica(clave) ? null : { claveSegura: true };
  };
}

export function mensajeClaveInvalida(clave: string): string {
  const requisito = obtenerRequisitosClave(clave).find((item) => !item.valido);
  if (!requisito) return '';

  const mensajes: Record<IdRequisitoClave, string> = {
    longitud: 'Faltan caracteres minimos.',
    mayuscula: 'Falta una letra mayuscula.',
    minuscula: 'Falta una letra minuscula.',
    numeros: 'La contrasena necesita al menos 2 numeros.',
  };

  return mensajes[requisito.id];
}
