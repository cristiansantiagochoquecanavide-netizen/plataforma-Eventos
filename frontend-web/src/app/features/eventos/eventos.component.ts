import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { of, switchMap } from 'rxjs';
import { EventosService } from './servicios/eventos.service';
import { CategoriasService } from '../categorias/servicios/categorias.service';
import { EspaciosService } from '../espacios/servicios/espacios.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import {
  EstadoEvento,
  Evento,
  GuardarEventoSolicitud,
  ResponsableEvento,
} from './modelos/evento.modelo';
import { Categoria } from '../categorias/modelos/categoria.modelo';
import { Espacio } from '../espacios/modelos/espacio.modelo';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';

type CampoEvento =
  | 'nombre'
  | 'descripcion'
  | 'fechaInicio'
  | 'horaInicio'
  | 'lugarPrincipal'
  | 'idCategoria'
  | 'idEspacio'
  | 'aforoMaximo';

@Component({
  selector: 'app-eventos',
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './eventos.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventosComponent implements OnInit {
  readonly eventos = signal<Evento[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly responsables = signal<ResponsableEvento[]>([]);
  readonly espacios = signal<Espacio[]>([]);
  readonly responsablesSeleccionados = signal<Set<number>>(new Set());
  readonly eventoSeleccionado = signal<Evento | null>(null);
  readonly modalAbierto = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly intentoEnvio = signal(false);
  readonly puedeCrear = computed(() => this.sesion.puedeCrearEventos());
  readonly puedeEditar = computed(() => this.sesion.puedeEditarEventos());
  readonly puedeAsignarResponsables = computed(() =>
    this.sesion.puedeAsignarResponsablesEvento(),
  );
  readonly puedeMantenerEventos = computed(() => this.puedeCrear() || this.puedeEditar());
  readonly formulario = new FormBuilder().nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', Validators.maxLength(500)],
    fechaInicio: ['', Validators.required],
    horaInicio: ['09:00', Validators.required],
    fechaFin: [''],
    horaFin: ['18:00'],
    lugarPrincipal: ['', Validators.maxLength(150)],
    idCategoria: [0, [Validators.required, Validators.min(1)]],
    idEspacio: [null as number | null, Validators.required],
    aforoMaximo: [null as number | null, [Validators.required, Validators.min(1)]],
    estado: ['PLANIFICADO' as EstadoEvento],
  });

  constructor(
    private readonly eventosService: EventosService,
    private readonly categoriasService: CategoriasService,
    private readonly espaciosService: EspaciosService,
    readonly sesion: SesionService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.cargarEventos();
    if (this.puedeMantenerEventos()) {
      this.cargarCategorias();
      this.cargarEspacios();
    }
    if (this.puedeAsignarResponsables()) {
      this.cargarResponsablesDisponibles();
    }
  }

  cargarEventos() {
    this.eventosService.listarEventos().subscribe({
      next: (eventos) => this.eventos.set(eventos),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar eventos');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  cargarResponsablesDisponibles() {
    this.eventosService.listarResponsablesDisponiblesEvento().subscribe({
      next: (responsables) => this.responsables.set(responsables),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar responsables');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  cargarCategorias() {
    this.categoriasService.listarCategorias().subscribe({
      next: (categorias) => this.categorias.set(categorias),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar categorias');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  cargarEspacios() {
    this.espaciosService.listarEspacios().subscribe({
      next: (espacios) => this.espacios.set(espacios),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar espacios');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  abrirModalCrear() {
    if (!this.puedeCrear()) return;
    this.nuevo();
    this.limpiarAvisos();
    this.intentoEnvio.set(false);
    this.modalAbierto.set(true);
  }

  cerrarModal() {
    this.modalAbierto.set(false);
    this.eventoSeleccionado.set(null);
    this.limpiarAvisos();
    this.intentoEnvio.set(false);
  }

  editar(evento: Evento, event?: Event) {
    if (event) {
      event.stopPropagation();
    }
    if (!this.puedeEditar()) return;
    this.limpiarAvisos();
    this.seleccionar(evento);
    this.modalAbierto.set(true);
  }

  seleccionar(evento: Evento) {
    this.limpiarAvisos();
    this.intentoEnvio.set(false);
    this.eventoSeleccionado.set(evento);
    this.responsablesSeleccionados.set(new Set(evento.responsables.map((responsable) => responsable.id)));
    const dInicio = new Date(evento.fechaInicio);
    const dFin = evento.fechaFin ? new Date(evento.fechaFin) : null;
    this.formulario.reset({
      nombre: evento.nombre,
      descripcion: evento.descripcion ?? '',
      fechaInicio: this.obtenerFechaISO(dInicio),
      horaInicio: this.obtenerHoraISO(dInicio),
      fechaFin: dFin ? this.obtenerFechaISO(dFin) : '',
      horaFin: dFin ? this.obtenerHoraISO(dFin) : '18:00',
      lugarPrincipal: evento.lugarPrincipal ?? '',
      idCategoria: evento.categoria?.id ?? 0,
      idEspacio: evento.espacio?.id ?? null,
      aforoMaximo: evento.aforoMaximo,
      estado: evento.estado,
    });
  }

  nuevo() {
    this.limpiarAvisos();
    this.intentoEnvio.set(false);
    this.eventoSeleccionado.set(null);
    this.responsablesSeleccionados.set(new Set());
    this.formulario.reset({
      nombre: '',
      descripcion: '',
      fechaInicio: '',
      horaInicio: '09:00',
      fechaFin: '',
      horaFin: '18:00',
      lugarPrincipal: '',
      idCategoria: 0,
      idEspacio: null,
      aforoMaximo: null,
      estado: 'PLANIFICADO',
    });
  }

  alternarResponsable(idUsuario: number, seleccionado: boolean) {
    const responsables = new Set(this.responsablesSeleccionados());
    if (seleccionado) {
      responsables.add(idUsuario);
    } else {
      responsables.delete(idUsuario);
    }
    this.responsablesSeleccionados.set(responsables);
  }

  guardar() {
    this.intentoEnvio.set(true);
    if (this.guardando()) return;
    const evento = this.eventoSeleccionado();
    if ((evento && !this.puedeEditar()) || (!evento && !this.puedeCrear())) {
      return;
    }
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      const mensaje = this.mensajeFormularioInvalido();
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    const valor = this.formulario.getRawValue();
    const horaIni = valor.horaInicio ? valor.horaInicio : '00:00';
    const fechaInicio = new Date(`${valor.fechaInicio}T${horaIni}:00`);
    let fechaFin: Date | null = null;
    if (valor.fechaFin) {
      const horaF = valor.horaFin ? valor.horaFin : '23:59';
      fechaFin = new Date(`${valor.fechaFin}T${horaF}:00`);
    }

    if (fechaFin && fechaFin <= fechaInicio) {
      const mensaje = 'La fecha de fin debe ser posterior a la fecha de inicio.';
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    this.limpiarAvisos();
    this.guardando.set(true);
    const responsables = Array.from(this.responsablesSeleccionados());
    const datos: GuardarEventoSolicitud = {
      nombre: valor.nombre.trim(),
      descripcion: valor.descripcion.trim() || null,
      fechaInicio: fechaInicio.toISOString(),
      fechaFin: fechaFin?.toISOString() ?? null,
      lugarPrincipal: valor.lugarPrincipal.trim() || null,
      idCategoria: valor.idCategoria,
      idEspacio: valor.idEspacio!,
      aforoMaximo: valor.aforoMaximo!,
      estado: valor.estado,
    };
    const solicitud = evento
      ? this.eventosService
          .actualizarEvento(evento.id, datos)
          .pipe(
            switchMap((actualizado) =>
              this.puedeAsignarResponsables()
                ? this.eventosService.asignarResponsablesEvento(evento.id, responsables)
                : of(actualizado),
            ),
          )
      : this.eventosService.crearEvento(datos);
    solicitud.subscribe({
      next: () => {
        const mensaje = evento ? 'Evento actualizado correctamente.' : 'Evento creado correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.cargarEventos();
        this.cerrarModal();
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible guardar el evento');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.guardando.set(false);
      },
      complete: () => this.guardando.set(false),
    });
  }

  campoConError(nombre: CampoEvento) {
    const control = this.formulario.controls[nombre];
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  mensajeCampo(nombre: CampoEvento) {
    const control = this.formulario.controls[nombre];
    if (control.hasError('required')) {
      if (nombre === 'idCategoria') return 'Selecciona una categoria.';
      if (nombre === 'idEspacio') return 'Selecciona un espacio.';
      if (nombre === 'aforoMaximo') return 'Ingresa el aforo del evento.';
      return 'Este campo es obligatorio.';
    }
    if (control.hasError('min')) {
      if (nombre === 'idCategoria') return 'Selecciona una categoria.';
      if (nombre === 'aforoMaximo') return 'El aforo debe ser mayor que cero.';
    }
    if (control.hasError('maxlength')) return 'Supera la longitud permitida.';
    return '';
  }

  private mensajeFormularioInvalido() {
    const orden: CampoEvento[] = [
      'nombre',
      'descripcion',
      'idCategoria',
      'fechaInicio',
      'horaInicio',
      'lugarPrincipal',
      'idEspacio',
      'aforoMaximo',
    ];
    const campo = orden.find((nombre) => this.formulario.controls[nombre].invalid);
    return campo ? this.mensajeCampo(campo) : 'Revisa los datos del evento.';
  }

  private obtenerFechaISO(fecha: Date): string {
    const desplazamiento = fecha.getTimezoneOffset() * 60000;
    return new Date(fecha.getTime() - desplazamiento).toISOString().slice(0, 10);
  }

  private obtenerHoraISO(fecha: Date): string {
    const desplazamiento = fecha.getTimezoneOffset() * 60000;
    return new Date(fecha.getTime() - desplazamiento).toISOString().slice(11, 16);
  }

  private limpiarAvisos() {
    this.error.set('');
    this.mensaje.set('');
  }
}
