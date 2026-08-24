import { Injectable, signal } from '@angular/core';

export type TipoToast = 'exito' | 'error' | 'info';

export interface ToastMensaje {
  id: number;
  tipo: TipoToast;
  titulo: string;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly mensajes = signal<ToastMensaje[]>([]);
  private contador = 0;

  exito(mensaje: string, titulo = 'Operacion completada') {
    this.mostrar('exito', mensaje, titulo);
  }

  error(mensaje: string, titulo = 'Revisa la informacion') {
    this.mostrar('error', mensaje, titulo);
  }

  info(mensaje: string, titulo = 'Aviso') {
    this.mostrar('info', mensaje, titulo);
  }

  cerrar(id: number) {
    this.mensajes.update((mensajes) => mensajes.filter((mensaje) => mensaje.id !== id));
  }

  private mostrar(tipo: TipoToast, mensaje: string, titulo: string) {
    const id = ++this.contador;
    this.mensajes.update((mensajes) => [...mensajes, { id, tipo, titulo, mensaje }].slice(-4));
    setTimeout(() => this.cerrar(id), 4600);
  }
}
