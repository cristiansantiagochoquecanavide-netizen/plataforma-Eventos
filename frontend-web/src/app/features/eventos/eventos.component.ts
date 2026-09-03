import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { EstadoEvento, Evento, GuardarEventoSolicitud, Usuario } from '../../core/modelos';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-eventos',
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './eventos.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventosComponent implements OnInit {
  readonly eventos = signal<Evento[]>([]);
  readonly usuarios = signal<Usuario[]>([]);
  readonly responsablesSeleccionados = signal<Set<number>>(new Set());
  readonly eventoSeleccionado = signal<Evento | null>(null);
  readonly modalAbierto = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly intentoEnvio = signal(false);
  readonly responsablesDisponibles = computed(() =>
    this.usuarios().filter(
      (usuario) =>
        usuario.estado === 'ACTIVO' &&
        usuario.roles.some((rol) => rol === 'ORGANIZADOR' || rol === 'STAFF'),
    ),
  );
  readonly formulario = new FormBuilder().nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', Validators.maxLength(500)],
    categoria: ['GENERAL', [Validators.required, Validators.maxLength(80)]],
    fechaInicio: ['', Validators.required],
    horaInicio: ['09:00', Validators.required],
    fechaFin: [''],
    horaFin: ['18:00'],
    lugarPrincipal: ['', Validators.maxLength(150)],
    estado: ['PLANIFICADO' as EstadoEvento],
  });

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.cargarEventos();
    this.cargarUsuarios();
  }

  cargarEventos() {
    this.api.listarEventos().subscribe({
      next: (eventos) => this.eventos.set(eventos),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar eventos');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  cargarUsuarios() {
    this.api.listarUsuarios(0, 100).subscribe({
      next: (pagina) => this.usuarios.set(pagina.elementos),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar responsables');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  abrirModalCrear() {
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
      categoria: evento.categoria,
      fechaInicio: this.obtenerFechaISO(dInicio),
      horaInicio: this.obtenerHoraISO(dInicio),
      fechaFin: dFin ? this.obtenerFechaISO(dFin) : '',
      horaFin: dFin ? this.obtenerHoraISO(dFin) : '18:00',
      lugarPrincipal: evento.lugarPrincipal ?? '',
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
      categoria: 'GENERAL',
      fechaInicio: '',
      horaInicio: '09:00',
      fechaFin: '',
      horaFin: '18:00',
      lugarPrincipal: '',
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
    if (this.formulario.invalid || this.responsablesSeleccionados().size === 0) {
      this.formulario.markAllAsTouched();
      const mensaje = this.formulario.invalid
        ? this.mensajeFormularioInvalido()
        : 'Debe asignarse al menos un responsable.';
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

    if (fechaFin && fechaFin < fechaInicio) {
      const mensaje = 'La fecha de fin no puede ser anterior a la fecha de inicio.';
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    this.limpiarAvisos();
    this.guardando.set(true);
    const evento = this.eventoSeleccionado();
    const responsables = Array.from(this.responsablesSeleccionados());
    const datos: GuardarEventoSolicitud = {
      nombre: valor.nombre.trim(),
      descripcion: valor.descripcion.trim() || null,
      categoria: valor.categoria.trim(),
      fechaInicio: fechaInicio.toISOString(),
      fechaFin: fechaFin?.toISOString() ?? null,
      lugarPrincipal: valor.lugarPrincipal.trim() || null,
      estado: valor.estado,
      responsables,
    };
    const solicitud = evento
      ? this.api
          .actualizarEvento(evento.id, datos)
          .pipe(switchMap(() => this.api.asignarResponsablesEvento(evento.id, responsables)))
      : this.api.crearEvento(datos);
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

  campoConError(nombre: 'nombre' | 'descripcion' | 'fechaInicio' | 'horaInicio' | 'lugarPrincipal') {
    const control = this.formulario.controls[nombre];
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  mensajeCampo(nombre: 'nombre' | 'descripcion' | 'fechaInicio' | 'horaInicio' | 'lugarPrincipal') {
    const control = this.formulario.controls[nombre];
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('maxlength')) return 'Supera la longitud permitida.';
    return '';
  }

  private mensajeFormularioInvalido() {
    const orden: Array<'nombre' | 'descripcion' | 'fechaInicio' | 'horaInicio' | 'lugarPrincipal'> = [
      'nombre',
      'descripcion',
      'fechaInicio',
      'horaInicio',
      'lugarPrincipal',
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
