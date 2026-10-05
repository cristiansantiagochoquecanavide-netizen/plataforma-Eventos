import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConvocatoriasService } from './servicios/convocatorias.service';
import { EventosService } from '../eventos/servicios/eventos.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import {
  Convocatoria,
  CrearConvocatoriaSolicitud,
  EstadoConvocatoria,
  TipoPersonalConvocatoria,
} from './modelos/convocatoria.modelo';
import { Evento } from '../eventos/modelos/evento.modelo';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { UiPrivilegeEnabledDirective, UiPrivilegeVisibleDirective } from '../../core/ui-privilege.directive';

/**
 * CU15: Gestion de Convocatorias de Staff y Voluntarios.
 *
 * Esta pantalla permite consultar y filtrar las convocatorias existentes. Los
 * administradores y organizadores pueden registrar una necesidad de personal
 * asociada a un evento, indicando tipo, descripcion, cantidad y fecha limite.
 * Tambien pueden cerrar o cancelar convocatorias abiertas. El formulario valida
 * todos los datos obligatorios antes de enviarlos y muestra los errores de la API.
 */
@Component({
  selector: 'app-convocatorias',
  imports: [ReactiveFormsModule, DatePipe, UiPrivilegeEnabledDirective, UiPrivilegeVisibleDirective],
  templateUrl: './convocatorias.component.html',
  styleUrl: './convocatorias.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConvocatoriasComponent implements OnInit {
  readonly convocatorias = signal<Convocatoria[]>([]);
  readonly eventos = signal<Evento[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly idProcesando = signal<number | null>(null);
  readonly modalAbierto = signal(false);
  readonly intentoEnvio = signal(false);
  readonly error = signal('');

  readonly filtroTermino = signal('');
  readonly filtroEvento = signal(0);
  readonly filtroTipo = signal<TipoPersonalConvocatoria | ''>('');
  readonly filtroEstado = signal<EstadoConvocatoria | ''>('');

  readonly tiposPersonal: TipoPersonalConvocatoria[] = ['STAFF', 'VOLUNTARIO'];
  readonly estados: EstadoConvocatoria[] = ['ABIERTA', 'CERRADA', 'CANCELADA'];
  readonly fechaMinima = this.formatearFechaLocal(new Date());

  readonly puedeGestionar = computed(() => this.sesion.puedeGestionarConvocatorias());
  readonly eventosDisponibles = computed(() =>
    this.eventos().filter(
      (evento) => evento.estado !== 'CANCELADO' && evento.estado !== 'FINALIZADO',
    ),
  );
  readonly eventosParaFiltro = computed(() => {
    const unicos = new Map<number, string>();
    this.convocatorias().forEach((convocatoria) =>
      unicos.set(convocatoria.idEvento, convocatoria.nombreEvento),
    );
    return Array.from(unicos, ([id, nombre]) => ({ id, nombre })).sort((a, b) =>
      a.nombre.localeCompare(b.nombre),
    );
  });

  readonly formulario = new FormBuilder().nonNullable.group({
    idEvento: [0, [Validators.required, Validators.min(1)]],
    tipoPersonal: ['STAFF' as TipoPersonalConvocatoria, Validators.required],
    descripcion: ['', [Validators.required, Validators.maxLength(1000)]],
    cantidadRequerida: [1, [Validators.required, Validators.min(1)]],
    fechaLimite: [this.fechaMinima, Validators.required],
  });

  constructor(
    private readonly convocatoriasService: ConvocatoriasService,
    private readonly eventosService: EventosService,
    readonly sesion: SesionService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.cargar();
    if (this.puedeGestionar()) this.cargarEventos();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    const idEvento = this.filtroEvento() || undefined;
    this.convocatoriasService
      .listarConvocatorias(
        idEvento,
        this.filtroTipo(),
        this.filtroEstado(),
        this.filtroTermino(),
      )
      .subscribe({
        next: (convocatorias) => {
          this.convocatorias.set(convocatorias);
          this.cargando.set(false);
        },
        error: (error) => {
          const mensaje = obtenerMensajeError(error, 'No fue posible cargar las convocatorias.');
          this.error.set(mensaje);
          this.toast.error(mensaje);
          this.cargando.set(false);
        },
      });
  }

  cargarEventos() {
    this.eventosService.listarEventos().subscribe({
      next: (eventos) => this.eventos.set(eventos),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar los eventos.');
        this.toast.error(mensaje);
      },
    });
  }

  abrirModal() {
    this.intentoEnvio.set(false);
    this.error.set('');
    const primerEvento = this.eventosDisponibles()[0];
    this.formulario.reset({
      idEvento: primerEvento?.id ?? 0,
      tipoPersonal: 'STAFF',
      descripcion: '',
      cantidadRequerida: 1,
      fechaLimite: this.fechaMinima,
    });
    this.modalAbierto.set(true);
  }

  cerrarModal() {
    if (!this.guardando()) this.modalAbierto.set(false);
  }

  guardar() {
    this.intentoEnvio.set(true);
    this.formulario.markAllAsTouched();
    if (this.formulario.invalid) return;

    this.guardando.set(true);
    const valor = this.formulario.getRawValue();
    this.convocatoriasService
      .crearConvocatoria({
        idEvento: valor.idEvento,
        tipoPersonal: valor.tipoPersonal,
        descripcion: valor.descripcion.trim(),
        cantidadRequerida: valor.cantidadRequerida,
        fechaLimite: valor.fechaLimite,
      })
      .subscribe({
        next: (creada) => {
          this.convocatorias.update((actuales) => [creada, ...actuales]);
          this.guardando.set(false);
          this.modalAbierto.set(false);
          this.toast.exito('Convocatoria registrada correctamente.');
        },
        error: (error) => {
          const mensaje = obtenerMensajeError(error, 'No fue posible registrar la convocatoria.');
          this.error.set(mensaje);
          this.toast.error(mensaje);
          this.guardando.set(false);
        },
      });
  }

  cerrar(convocatoria: Convocatoria) {
    if (!confirm(`¿Cerrar la convocatoria de ${convocatoria.nombreEvento}?`)) return;
    this.procesarEstado(convocatoria, 'CERRADA');
  }

  cancelar(convocatoria: Convocatoria) {
    if (!confirm(`¿Cancelar la convocatoria de ${convocatoria.nombreEvento}?`)) return;
    this.procesarEstado(convocatoria, 'CANCELADA');
  }

  buscar(termino: string) {
    this.filtroTermino.set(termino);
    this.cargar();
  }

  cambiarFiltroEvento(valor: string) {
    this.filtroEvento.set(Number(valor) || 0);
    this.cargar();
  }

  cambiarFiltroTipo(valor: string) {
    this.filtroTipo.set(valor as TipoPersonalConvocatoria | '');
    this.cargar();
  }

  cambiarFiltroEstado(valor: string) {
    this.filtroEstado.set(valor as EstadoConvocatoria | '');
    this.cargar();
  }

  limpiarFiltros() {
    this.filtroTermino.set('');
    this.filtroEvento.set(0);
    this.filtroTipo.set('');
    this.filtroEstado.set('');
    this.cargar();
  }

  campoConError(campo: keyof typeof this.formulario.controls) {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  mensajeCampo(campo: keyof typeof this.formulario.controls) {
    const control = this.formulario.controls[campo];
    if (control.hasError('required')) return 'Este dato es obligatorio.';
    if (control.hasError('min')) return 'El valor debe ser mayor que cero.';
    if (control.hasError('maxlength')) return 'Se supero la longitud permitida.';
    return 'Dato invalido.';
  }

  fechaMaximaEvento() {
    const idEvento = this.formulario.controls.idEvento.value;
    return this.eventos().find((evento) => evento.id === idEvento)?.fechaInicio.slice(0, 10) ?? '';
  }

  etiquetaTipo(tipo: TipoPersonalConvocatoria) {
    return tipo === 'STAFF' ? 'Staff' : 'Voluntariado';
  }

  claseEstado(estado: EstadoConvocatoria) {
    return estado.toLowerCase();
  }

  private procesarEstado(
    convocatoria: Convocatoria,
    destino: Extract<EstadoConvocatoria, 'CERRADA' | 'CANCELADA'>,
  ) {
    this.idProcesando.set(convocatoria.id);
    const operacion =
      destino === 'CERRADA'
        ? this.convocatoriasService.cerrarConvocatoria(convocatoria.id)
        : this.convocatoriasService.cancelarConvocatoria(convocatoria.id);

    operacion.subscribe({
      next: (actualizada) => {
        this.convocatorias.update((actuales) =>
          actuales.map((item) => (item.id === actualizada.id ? actualizada : item)),
        );
        this.idProcesando.set(null);
        this.toast.exito(
          destino === 'CERRADA' ? 'Convocatoria cerrada.' : 'Convocatoria cancelada.',
        );
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible actualizar la convocatoria.');
        this.toast.error(mensaje);
        this.idProcesando.set(null);
      },
    });
  }

  private formatearFechaLocal(fecha: Date) {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }
}
