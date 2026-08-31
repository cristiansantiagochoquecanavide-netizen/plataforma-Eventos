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
    fechaInicio: ['', Validators.required],
    fechaFin: [''],
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

  seleccionar(evento: Evento) {
    this.limpiarAvisos();
    this.intentoEnvio.set(false);
    this.eventoSeleccionado.set(evento);
    this.responsablesSeleccionados.set(new Set(evento.responsables.map((responsable) => responsable.id)));
    this.formulario.reset({
      nombre: evento.nombre,
      descripcion: evento.descripcion ?? '',
      fechaInicio: this.formatearFechaControl(evento.fechaInicio),
      fechaFin: evento.fechaFin ? this.formatearFechaControl(evento.fechaFin) : '',
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
      fechaInicio: '',
      fechaFin: '',
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
    const fechaInicio = this.convertirFecha(valor.fechaInicio);
    const fechaFin = valor.fechaFin ? this.convertirFecha(valor.fechaFin) : null;
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
      next: (eventoGuardado) => {
        const mensaje = evento ? 'Evento actualizado correctamente.' : 'Evento creado correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.cargarEventos();
        this.seleccionar(eventoGuardado);
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

  campoConError(nombre: 'nombre' | 'descripcion' | 'fechaInicio' | 'lugarPrincipal') {
    const control = this.formulario.controls[nombre];
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  mensajeCampo(nombre: 'nombre' | 'descripcion' | 'fechaInicio' | 'lugarPrincipal') {
    const control = this.formulario.controls[nombre];
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('maxlength')) return 'Supera la longitud permitida.';
    return '';
  }

  private mensajeFormularioInvalido() {
    const orden: Array<'nombre' | 'descripcion' | 'fechaInicio' | 'lugarPrincipal'> = [
      'nombre',
      'descripcion',
      'fechaInicio',
      'lugarPrincipal',
    ];
    const campo = orden.find((nombre) => this.formulario.controls[nombre].invalid);
    return campo ? this.mensajeCampo(campo) : 'Revisa los datos del evento.';
  }

  private convertirFecha(valor: string) {
    return new Date(valor);
  }

  private formatearFechaControl(valor: string) {
    const fecha = new Date(valor);
    const desplazamiento = fecha.getTimezoneOffset() * 60000;
    return new Date(fecha.getTime() - desplazamiento).toISOString().slice(0, 16);
  }

  private limpiarAvisos() {
    this.error.set('');
    this.mensaje.set('');
  }
}
