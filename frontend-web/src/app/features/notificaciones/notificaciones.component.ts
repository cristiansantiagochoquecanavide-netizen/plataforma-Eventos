import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EventosService } from '../eventos/servicios/eventos.service';
import { Evento } from '../eventos/modelos/evento.modelo';
import { NotificacionesService } from './servicios/notificaciones.service';
import { NotificacionEvento } from './modelos/notificacion-evento.modelo';

@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './notificaciones.component.html',
  styleUrl: './notificaciones.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificacionesComponent implements OnInit {
  readonly eventos = signal<Evento[]>([]);
  readonly notificaciones = signal<NotificacionEvento[]>([]);
  readonly cargandoEventos = signal(true);
  readonly cargandoHistorial = signal(false);
  readonly enviando = signal(false);
  readonly reintentandoId = signal<number | null>(null);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly pendientes = computed(() => this.notificaciones().filter((item) => item.estado === 'PENDIENTE' || item.estado === 'PROCESANDO').length);
  readonly enviados = computed(() => this.notificaciones().filter((item) => item.estado === 'ENVIADO').length);
  readonly fallidos = computed(() => this.notificaciones().filter((item) => item.estado === 'FALLIDO').length);

  readonly formulario = new FormGroup({
    idEvento: new FormControl<number | null>(null, Validators.required),
    fechaProgramada: new FormControl(this.formatearFechaLocal(new Date(Date.now() + 2 * 60 * 60 * 1000)), Validators.required),
    asunto: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    mensaje: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(4000)] }),
  });

  constructor(
    private readonly eventosService: EventosService,
    private readonly notificacionesService: NotificacionesService,
  ) {}

  ngOnInit(): void {
    this.eventosService.listarEventos().subscribe({
      next: (eventos) => {
        this.eventos.set(eventos);
        this.cargandoEventos.set(false);
        if (eventos.length) {
          this.formulario.controls.idEvento.setValue(eventos[0].id);
          this.formulario.controls.asunto.setValue(`Recordatorio · ${eventos[0].nombre}`);
          this.cargarHistorial();
        }
      },
      error: () => {
        this.error.set('No fue posible cargar eventos de esta organización. Comprueba tu conexión y vuelve a intentar.');
        this.cargandoEventos.set(false);
      },
    });
  }

  eventoSeleccionado(): Evento | undefined {
    const id = this.formulario.controls.idEvento.value;
    return this.eventos().find((evento) => evento.id === id);
  }

  etiquetaTipo(tipo: string): string {
    const etiquetas: Record<string, string> = {
      CONFIRMACION_INSCRIPCION: 'Confirmación',
      RECORDATORIO_24H: 'Recordatorio · 24 h',
      RECORDATORIO_2H: 'Recordatorio · 2 h',
      RECORDATORIO_MANUAL: 'Recordatorio programado',
      EVENTO_REPROGRAMADO: 'Cambio de horario',
      CAMBIO_ESPACIO: 'Cambio de ubicación',
      EVENTO_CANCELADO: 'Cancelación',
    };
    return etiquetas[tipo] ?? tipo.replaceAll('_', ' ');
  }

  etiquetaEstado(estado: string): string {
    const etiquetas: Record<string, string> = {
      PENDIENTE: 'Pendiente',
      PROCESANDO: 'Enviando',
      ENVIADO: 'Enviado',
      FALLIDO: 'Fallido',
      CANCELADO: 'Cancelado',
    };
    return etiquetas[estado] ?? estado;
  }

  formatearFecha(valor: string | null): string {
    if (!valor) return 'Aún no enviado';
    return new Intl.DateTimeFormat('es-BO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(valor));
  }

  formatearFechaEvento(valor: string): string {
    return new Intl.DateTimeFormat('es-BO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(valor));
  }

  cambioEvento(): void {
    const evento = this.eventoSeleccionado();
    if (!evento) return;
    this.formulario.controls.asunto.setValue(`Recordatorio · ${evento.nombre}`);
    this.cargarHistorial();
  }

  cargarHistorial(): void {
    const idEvento = this.formulario.controls.idEvento.value;
    if (idEvento == null) return;
    this.cargandoHistorial.set(true);
    this.notificacionesService.listarPorEvento(idEvento).subscribe({
      next: (items) => {
        this.notificaciones.set(items);
        this.cargandoHistorial.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.mensajeError(error, 'No fue posible cargar el historial de notificaciones.'));
        this.cargandoHistorial.set(false);
      },
    });
  }

  programar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const idEvento = this.formulario.controls.idEvento.value;
    const fechaLocal = this.formulario.controls.fechaProgramada.value;
    if (idEvento == null || !fechaLocal) return;

    this.enviando.set(true);
    this.error.set('');
    this.mensaje.set('');
    this.notificacionesService.programarRecordatorio(idEvento, {
      fechaProgramada: new Date(fechaLocal).toISOString(),
      asunto: this.formulario.controls.asunto.value.trim(),
      mensaje: this.formulario.controls.mensaje.value.trim(),
    }).subscribe({
      next: (resultado) => {
        this.mensaje.set(`Recordatorio programado para ${resultado.destinatarios} participantes confirmados.`);
        this.formulario.controls.mensaje.setValue('');
        this.enviando.set(false);
        this.cargarHistorial();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.mensajeError(error, 'No fue posible programar el recordatorio.'));
        this.enviando.set(false);
      },
    });
  }

  reintentar(item: NotificacionEvento): void {
    this.reintentandoId.set(item.id);
    this.error.set('');
    this.notificacionesService.reintentar(item.idEvento, item.id).subscribe({
      next: () => {
        this.mensaje.set(`El correo para ${item.destinatario} quedó nuevamente en cola.`);
        this.reintentandoId.set(null);
        this.cargarHistorial();
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(this.mensajeError(error, 'No se pudo reintentar el envío.'));
        this.reintentandoId.set(null);
      },
    });
  }

  private mensajeError(error: HttpErrorResponse, fallback: string): string {
    return error.error?.message ?? fallback;
  }

  private formatearFechaLocal(fecha: Date): string {
    const dos = (valor: number) => valor.toString().padStart(2, '0');
    return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}`;
  }
}
