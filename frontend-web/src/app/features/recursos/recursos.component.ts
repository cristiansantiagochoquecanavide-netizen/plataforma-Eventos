import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RecursosService } from './servicios/recursos.service';
import { EventosService } from '../eventos/servicios/eventos.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import {
  ActualizarRecursoSolicitud,
  AsignacionRecurso,
  CrearAsignacionSolicitud,
  CrearRecursoSolicitud,
  EstadoAsignacion,
  EstadoRecurso,
  Recurso,
  TipoRecurso,
} from './modelos/recurso.modelo';
import { Evento } from '../eventos/modelos/evento.modelo';
import { ToastService } from '../../core/toast.service';
import { UiPrivilegeEnabledDirective, UiPrivilegeVisibleDirective } from '../../core/ui-privilege.directive';

@Component({
  selector: 'app-recursos',
  imports: [ReactiveFormsModule, DatePipe, UiPrivilegeEnabledDirective, UiPrivilegeVisibleDirective],
  templateUrl: './recursos.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecursosComponent implements OnInit {
  // Pestaña activa: 'inventario' (CU19) | 'asignaciones' (CU20)
  readonly pestanaActiva = signal<'inventario' | 'asignaciones'>('inventario');

  // Datos principales
  readonly recursos = signal<Recurso[]>([]);
  readonly eventos = signal<Evento[]>([]);
  readonly asignaciones = signal<AsignacionRecurso[]>([]);

  // Estados de carga y feedback
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly cargando = signal(false);
  readonly cargandoAsignaciones = signal(false);
  readonly idCambiandoEstado = signal<number | null>(null);
  readonly idLiberando = signal<number | null>(null);

  // Modal de Recurso (Crear / Editar)
  readonly modalAbierto = signal(false);
  readonly editando = signal<Recurso | null>(null);
  readonly guardando = signal(false);
  readonly intentoEnvio = signal(false);

  // Modal de Asignación (CU20)
  readonly modalAsignacionAbierto = signal(false);
  readonly guardandoAsignacion = signal(false);
  readonly intentoEnvioAsignacion = signal(false);
  readonly recursoSeleccionadoParaAsignar = signal<Recurso | null>(null);

  // Filtros de Inventario (CU19)
  readonly filtroTermino = signal('');
  readonly filtroTipo = signal<TipoRecurso | ''>('');
  readonly filtroEstado = signal<EstadoRecurso | ''>('');

  // Filtros de Asignaciones (CU20)
  readonly filtroEventoId = signal<number>(0);
  readonly filtroEstadoAsignacion = signal<EstadoAsignacion | ''>('');

  readonly tiposRecurso: TipoRecurso[] = [
    'AUDIOVISUAL',
    'MOBILIARIO',
    'EQUIPO_COMPUTO',
    'ESPACIO',
    'OTRO',
  ];

  readonly estadosRecurso: EstadoRecurso[] = [
    'DISPONIBLE',
    'EN_USO',
    'MANTENIMIENTO',
    'DE_BAJA',
  ];

  readonly estadosAsignacion: EstadoAsignacion[] = ['ACTIVA', 'DEVUELTA', 'CANCELADA'];

  // Recursos con disponibilidad positiva para asignaciones
  readonly recursosDisponiblesParaAsignar = computed(() =>
    this.recursos().filter(
      (r) => r.cantidadDisponible > 0 && r.estado !== 'DE_BAJA' && r.estado !== 'MANTENIMIENTO',
    ),
  );

  readonly formulario = new FormBuilder().nonNullable.group({
    codigo: ['', [Validators.required, Validators.maxLength(30)]],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    tipo: ['AUDIOVISUAL' as TipoRecurso, Validators.required],
    cantidadTotal: [1, [Validators.required, Validators.min(0)]],
    cantidadDisponible: [1, [Validators.required, Validators.min(0)]],
    estado: ['DISPONIBLE' as EstadoRecurso, Validators.required],
    ubicacion: ['', Validators.maxLength(150)],
    descripcion: ['', Validators.maxLength(255)],
    costoUnitario: [0, [Validators.required, Validators.min(0)]],
  });

  readonly formularioAsignacion = new FormBuilder().nonNullable.group({
    idEvento: [0, [Validators.required, Validators.min(1)]],
    idRecurso: [0, [Validators.required, Validators.min(1)]],
    salaEspacio: ['', [Validators.required, Validators.maxLength(100)]],
    cantidadAsignada: [1, [Validators.required, Validators.min(1)]],
    observaciones: ['', Validators.maxLength(255)],
  });

  constructor(
    private readonly recursosService: RecursosService,
    private readonly eventosService: EventosService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.cargar();
    this.cargarEventos();
  }

  cambiarPestana(pestana: 'inventario' | 'asignaciones') {
    this.pestanaActiva.set(pestana);
    this.error.set('');
    this.mensaje.set('');
    if (pestana === 'asignaciones') {
      this.cargarAsignaciones();
      this.cargarEventos();
    } else {
      this.cargar();
    }
  }

  cargar() {
    this.cargando.set(true);
    this.recursosService
      .listarRecursos(this.filtroTermino(), this.filtroTipo(), this.filtroEstado())
      .subscribe({
        next: (recursos) => {
          this.recursos.set(recursos);
          this.cargando.set(false);
        },
        error: (error) => {
          const mensaje = obtenerMensajeError(error, 'No fue posible cargar los recursos.');
          this.error.set(mensaje);
          this.toast.error(mensaje);
          this.cargando.set(false);
        },
      });
  }

  cargarEventos() {
    this.eventosService.listarEventos().subscribe({
      next: (eventos) => this.eventos.set(eventos),
      error: () => {},
    });
  }

  cargarAsignaciones() {
    this.cargandoAsignaciones.set(true);
    const idEvento = this.filtroEventoId() > 0 ? this.filtroEventoId() : undefined;
    this.recursosService
      .listarAsignaciones(idEvento, undefined, this.filtroEstadoAsignacion())
      .subscribe({
        next: (asignaciones) => {
          this.asignaciones.set(asignaciones);
          this.cargandoAsignaciones.set(false);
        },
        error: (error) => {
          const mensaje = obtenerMensajeError(error, 'No fue posible cargar las asignaciones.');
          this.error.set(mensaje);
          this.toast.error(mensaje);
          this.cargandoAsignaciones.set(false);
        },
      });
  }

  // --- Filtros Inventario ---
  buscar(termino: string) {
    this.filtroTermino.set(termino);
    this.cargar();
  }

  cambiarFiltroTipo(tipo: string) {
    this.filtroTipo.set(tipo as TipoRecurso | '');
    this.cargar();
  }

  cambiarFiltroEstado(estado: string) {
    this.filtroEstado.set(estado as EstadoRecurso | '');
    this.cargar();
  }

  limpiarFiltros() {
    this.filtroTermino.set('');
    this.filtroTipo.set('');
    this.filtroEstado.set('');
    this.cargar();
  }

  // --- Filtros Asignaciones ---
  cambiarFiltroEvento(idEvento: string) {
    this.filtroEventoId.set(Number(idEvento) || 0);
    this.cargarAsignaciones();
  }

  cambiarFiltroEstadoAsignacion(estado: string) {
    this.filtroEstadoAsignacion.set(estado as EstadoAsignacion | '');
    this.cargarAsignaciones();
  }

  limpiarFiltrosAsignaciones() {
    this.filtroEventoId.set(0);
    this.filtroEstadoAsignacion.set('');
    this.cargarAsignaciones();
  }

  // --- Modal Recurso (CU19) ---
  abrirModalCrear() {
    this.editando.set(null);
    this.intentoEnvio.set(false);
    this.error.set('');
    this.formulario.reset({
      codigo: '',
      nombre: '',
      tipo: 'AUDIOVISUAL',
      cantidadTotal: 1,
      cantidadDisponible: 1,
      estado: 'DISPONIBLE',
      ubicacion: '',
      descripcion: '',
      costoUnitario: 0,
    });
    this.modalAbierto.set(true);
  }

  editar(recurso: Recurso) {
    this.editando.set(recurso);
    this.intentoEnvio.set(false);
    this.error.set('');
    this.formulario.setValue({
      codigo: recurso.codigo,
      nombre: recurso.nombre,
      tipo: recurso.tipo,
      cantidadTotal: recurso.cantidadTotal,
      cantidadDisponible: recurso.cantidadDisponible,
      estado: recurso.estado,
      ubicacion: recurso.ubicacion || '',
      descripcion: recurso.descripcion || '',
      costoUnitario: recurso.costoUnitario ?? 0,
    });
    this.modalAbierto.set(true);
  }

  cerrarModal() {
    this.modalAbierto.set(false);
    this.cancelar();
  }

  cancelar() {
    this.editando.set(null);
    this.intentoEnvio.set(false);
    this.error.set('');
    this.formulario.reset({
      codigo: '',
      nombre: '',
      tipo: 'AUDIOVISUAL',
      cantidadTotal: 1,
      cantidadDisponible: 1,
      estado: 'DISPONIBLE',
      ubicacion: '',
      descripcion: '',
      costoUnitario: 0,
    });
  }

  guardar() {
    this.intentoEnvio.set(true);
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }

    const valor = this.formulario.getRawValue();
    if (valor.cantidadDisponible > valor.cantidadTotal) {
      this.error.set('La cantidad disponible no puede ser mayor que la cantidad total.');
      this.toast.error('La cantidad disponible no puede ser mayor que la cantidad total.');
      return;
    }

    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');

    const recursoActual = this.editando();
    if (recursoActual) {
      this.recursosService
        .actualizarRecurso(recursoActual.id, {
          codigo: valor.codigo,
          nombre: valor.nombre,
          tipo: valor.tipo,
          cantidadTotal: valor.cantidadTotal,
          cantidadDisponible: valor.cantidadDisponible,
          estado: valor.estado,
          ubicacion: valor.ubicacion || null,
          descripcion: valor.descripcion || null,
          costoUnitario: Number(valor.costoUnitario) || 0,
        })
        .subscribe({
          next: () => {
            this.toast.exito('Recurso actualizado con éxito.');
            this.mensaje.set('Recurso actualizado con éxito.');
            this.cerrarModal();
            this.cargar();
            this.guardando.set(false);
          },
          error: (error) => {
            const mensaje = obtenerMensajeError(error, 'No fue posible actualizar el recurso.');
            this.error.set(mensaje);
            this.toast.error(mensaje);
            this.guardando.set(false);
          },
        });
    } else {
      this.recursosService
        .crearRecurso({
          codigo: valor.codigo,
          nombre: valor.nombre,
          tipo: valor.tipo,
          cantidadTotal: valor.cantidadTotal,
          ubicacion: valor.ubicacion || null,
          descripcion: valor.descripcion || null,
          costoUnitario: Number(valor.costoUnitario) || 0,
        })
        .subscribe({
          next: () => {
            this.toast.exito('Recurso registrado exitosamente.');
            this.mensaje.set('Recurso registrado exitosamente.');
            this.cerrarModal();
            this.cargar();
            this.guardando.set(false);
          },
          error: (error) => {
            const mensaje = obtenerMensajeError(error, 'No fue posible registrar el recurso.');
            this.error.set(mensaje);
            this.toast.error(mensaje);
            this.guardando.set(false);
          },
        });
    }
  }

  cambiarEstado(recurso: Recurso, nuevoEstado: EstadoRecurso) {
    this.idCambiandoEstado.set(recurso.id);
    this.recursosService.cambiarEstadoRecurso(recurso.id, nuevoEstado).subscribe({
      next: () => {
        this.toast.exito(`Estado de ${recurso.codigo} actualizado a ${nuevoEstado}.`);
        this.cargar();
        this.idCambiandoEstado.set(null);
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cambiar el estado.');
        this.toast.error(mensaje);
        this.idCambiandoEstado.set(null);
      },
    });
  }

  // --- Modal Asignación (CU20) ---
  abrirModalAsignar(recurso?: Recurso) {
    this.intentoEnvioAsignacion.set(false);
    this.error.set('');
    const idRecurso = recurso ? recurso.id : (this.recursosDisponiblesParaAsignar()[0]?.id ?? 0);
    const primerEvento = this.eventos()[0]?.id ?? 0;

    this.formularioAsignacion.reset({
      idEvento: primerEvento,
      idRecurso: idRecurso,
      salaEspacio: '',
      cantidadAsignada: 1,
      observaciones: '',
    });

    this.actualizarRecursoSeleccionado(idRecurso);
    this.modalAsignacionAbierto.set(true);
  }

  cambiarRecursoEnAsignacion(idStr: string) {
    const id = Number(idStr);
    this.actualizarRecursoSeleccionado(id);
  }

  private actualizarRecursoSeleccionado(id: number) {
    const r = this.recursos().find((item) => item.id === id) || null;
    this.recursoSeleccionadoParaAsignar.set(r);

    const ctrl = this.formularioAsignacion.controls.cantidadAsignada;
    ctrl.clearValidators();
    ctrl.addValidators([Validators.required, Validators.min(1)]);
    if (r) {
      ctrl.addValidators(Validators.max(r.cantidadDisponible));
    }
    ctrl.updateValueAndValidity();
  }

  cerrarModalAsignacion() {
    this.modalAsignacionAbierto.set(false);
    this.recursoSeleccionadoParaAsignar.set(null);
    this.intentoEnvioAsignacion.set(false);
  }

  guardarAsignacion() {
    this.intentoEnvioAsignacion.set(true);
    if (this.formularioAsignacion.invalid || this.guardandoAsignacion()) {
      this.formularioAsignacion.markAllAsTouched();
      return;
    }

    const valor = this.formularioAsignacion.getRawValue();
    const recurso = this.recursoSeleccionadoParaAsignar();

    if (!recurso) {
      this.error.set('Debes seleccionar un recurso válido.');
      this.toast.error('Debes seleccionar un recurso válido.');
      return;
    }

    if (valor.cantidadAsignada <= 0) {
      this.error.set('La cantidad asignada debe ser mayor que cero.');
      this.toast.error('La cantidad asignada debe ser mayor que cero.');
      return;
    }

    if (valor.cantidadAsignada > recurso.cantidadDisponible) {
      const msg = `No puedes asignar más de las ${recurso.cantidadDisponible} unidades disponibles.`;
      this.error.set(msg);
      this.toast.error(msg);
      return;
    }

    this.guardandoAsignacion.set(true);
    this.error.set('');

    this.recursosService
      .crearAsignacion({
        idEvento: valor.idEvento,
        idRecurso: valor.idRecurso,
        salaEspacio: valor.salaEspacio,
        cantidadAsignada: valor.cantidadAsignada,
        observaciones: valor.observaciones || null,
      })
      .subscribe({
        next: (creada) => {
          this.toast.exito(`Recurso ${creada.codigoRecurso} asignado al evento con éxito.`);
          this.mensaje.set(`Recurso ${creada.codigoRecurso} asignado con éxito a ${creada.nombreEvento}.`);
          this.cerrarModalAsignacion();
          this.cargar();
          this.cargarAsignaciones();
          this.guardandoAsignacion.set(false);
        },
        error: (error) => {
          const mensaje = obtenerMensajeError(error, 'No fue posible realizar la asignación.');
          this.error.set(mensaje);
          this.toast.error(mensaje);
          this.guardandoAsignacion.set(false);
        },
      });
  }

  liberarAsignacion(asignacion: AsignacionRecurso) {
    if (asignacion.estado !== 'ACTIVA') return;
    this.idLiberando.set(asignacion.id);

    this.recursosService.liberarAsignacion(asignacion.id).subscribe({
      next: () => {
        this.toast.exito(`Asignación liberada. Se reintegraron ${asignacion.cantidadAsignada} unidades de ${asignacion.codigoRecurso}.`);
        this.cargar();
        this.cargarAsignaciones();
        this.idLiberando.set(null);
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible devolver la asignación.');
        this.toast.error(mensaje);
        this.idLiberando.set(null);
      },
    });
  }

  campoConError(campo: keyof typeof this.formulario.controls) {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  campoAsignacionConError(campo: keyof typeof this.formularioAsignacion.controls) {
    const control = this.formularioAsignacion.controls[campo];
    return control.invalid && (control.touched || this.intentoEnvioAsignacion());
  }

  mensajeCampo(campo: keyof typeof this.formulario.controls) {
    const control = this.formulario.controls[campo];
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('min')) return 'El valor no puede ser negativo.';
    if (control.hasError('maxlength')) return 'Supera la longitud máxima permitida.';
    return 'Dato no válido.';
  }

  mensajeCampoAsignacion(campo: keyof typeof this.formularioAsignacion.controls) {
    const control = this.formularioAsignacion.controls[campo];
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('min')) return 'Debe ser mayor a 0.';
    if (control.hasError('max')) {
      const disponible = this.recursoSeleccionadoParaAsignar()?.cantidadDisponible ?? 0;
      return `Stock insuficiente (Disponible: ${disponible}).`;
    }
    if (control.hasError('maxlength')) return 'Supera la longitud permitida.';
    return 'Dato no válido.';
  }
}
