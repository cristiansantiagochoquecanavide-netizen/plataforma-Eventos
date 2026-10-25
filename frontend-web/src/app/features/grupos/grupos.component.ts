import { HttpClientModule } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { ToastService } from '../../core/toast.service';
import { UiPrivilegeEditableDirective, UiPrivilegeEnabledDirective, UiPrivilegeVisibleDirective } from '../../core/ui-privilege.directive';
import { Membresia } from '../organizaciones/modelos/organizacion.modelo';
import { OrganizacionesService } from '../organizaciones/servicios/organizaciones.service';
import { GrupoUsuario, RolGrupo } from './modelos/grupo-usuario.modelo';
import { GruposService } from './servicios/grupos.service';

@Component({
  selector: 'app-grupos',
  imports: [
    HttpClientModule,
    ReactiveFormsModule,
    UiPrivilegeVisibleDirective,
    UiPrivilegeEnabledDirective,
    UiPrivilegeEditableDirective,
  ],
  templateUrl: './grupos.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GruposComponent implements OnInit {
  readonly grupos = signal<GrupoUsuario[]>([]);
  readonly miembros = signal<Membresia[]>([]);
  readonly rolesDisponibles = signal<RolGrupo[]>([]);
  readonly grupoSeleccionado = signal<GrupoUsuario | null>(null);
  readonly miembrosSeleccionados = signal<Set<number>>(new Set());
  readonly rolesSeleccionados = signal<Set<number>>(new Set());
  readonly modalAbierto = signal(false);
  readonly editando = signal<GrupoUsuario | null>(null);
  readonly mensaje = signal('');
  readonly error = signal('');
  readonly guardandoGrupo = signal(false);
  readonly guardandoAsignaciones = signal(false);
  readonly intentoEnvio = signal(false);
  readonly miembrosDisponibles = computed(() => this.miembros().filter((miembro) => miembro.activo && miembro.cuentaActiva));
  readonly formulario = new FormBuilder().nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    descripcion: [''],
  });

  constructor(
    private readonly gruposService: GruposService,
    private readonly organizacionesService: OrganizacionesService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.cargarGrupos();
    this.cargarOpciones();
  }

  cargarGrupos() {
    this.gruposService.listarGrupos().subscribe({
      next: (grupos) => this.grupos.set(grupos),
      error: (error) => this.fallar(error, 'No fue posible cargar grupos'),
    });
  }

  cargarOpciones() {
    this.organizacionesService.listarMiembros().subscribe({
      next: (miembros) => this.miembros.set(miembros),
      error: (error) => this.fallar(error, 'No fue posible cargar miembros de la organización'),
    });
    this.gruposService.listarRolesDisponibles().subscribe({
      next: (roles) => this.rolesDisponibles.set(roles),
      error: (error) => this.fallar(error, 'No fue posible cargar roles disponibles'),
    });
  }

  abrirCrear() {
    this.cancelarFormulario();
    this.limpiarAvisos();
    this.modalAbierto.set(true);
  }

  editar(grupo: GrupoUsuario) {
    this.limpiarAvisos();
    this.intentoEnvio.set(false);
    this.editando.set(grupo);
    this.formulario.setValue({ nombre: grupo.nombre, descripcion: grupo.descripcion ?? '' });
    this.modalAbierto.set(true);
  }

  cerrarModal() {
    this.modalAbierto.set(false);
    this.cancelarFormulario();
    this.limpiarAvisos();
  }

  guardarGrupo() {
    this.intentoEnvio.set(true);
    if (this.guardandoGrupo()) return;
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      const mensaje = this.mensajeFormularioInvalido();
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    this.limpiarAvisos();
    this.guardandoGrupo.set(true);
    const datos = this.formulario.getRawValue();
    const editando = this.editando();
    const solicitud = editando
      ? this.gruposService.actualizarGrupo(editando.id, datos)
      : this.gruposService.crearGrupo(datos);

    solicitud.subscribe({
      next: (grupo) => {
        const mensaje = editando ? 'Grupo actualizado correctamente.' : 'Grupo creado correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.cerrarModal();
        this.cargarGrupos();
        if (this.grupoSeleccionado()?.id === grupo.id) this.aplicarSeleccion(grupo);
      },
      error: (error) => {
        this.fallar(error, 'No fue posible guardar el grupo');
        this.guardandoGrupo.set(false);
      },
      complete: () => this.guardandoGrupo.set(false),
    });
  }

  seleccionarGrupo(grupo: GrupoUsuario) {
    this.limpiarAvisos();
    this.gruposService.obtenerGrupo(grupo.id).subscribe({
      next: (detalle) => this.aplicarSeleccion(detalle),
      error: (error) => this.fallar(error, 'No fue posible consultar el grupo'),
    });
  }

  alternarMiembro(idUsuario: number, marcado: boolean) {
    const seleccion = new Set(this.miembrosSeleccionados());
    marcado ? seleccion.add(idUsuario) : seleccion.delete(idUsuario);
    this.miembrosSeleccionados.set(seleccion);
  }

  alternarRol(idRol: number, marcado: boolean) {
    const seleccion = new Set(this.rolesSeleccionados());
    marcado ? seleccion.add(idRol) : seleccion.delete(idRol);
    this.rolesSeleccionados.set(seleccion);
  }

  guardarAsignaciones() {
    const grupo = this.grupoSeleccionado();
    if (!grupo || this.guardandoAsignaciones()) return;

    this.limpiarAvisos();
    this.guardandoAsignaciones.set(true);
    const idsUsuarios = [...this.miembrosSeleccionados()];
    const idsRoles = [...this.rolesSeleccionados()];

    forkJoin([
      this.gruposService.asignarMiembros(grupo.id, idsUsuarios),
      this.gruposService.asignarRoles(grupo.id, idsRoles),
    ]).subscribe({
      next: ([, actualizado]) => {
        this.aplicarSeleccion(actualizado);
        const mensaje = 'Asignaciones del grupo actualizadas correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.cargarGrupos();
      },
      error: (error) => {
        this.fallar(error, 'No fue posible guardar las asignaciones');
        this.guardandoAsignaciones.set(false);
      },
      complete: () => this.guardandoAsignaciones.set(false),
    });
  }

  desactivar(grupo: GrupoUsuario) {
    this.cambiarEstado(grupo, false);
  }

  activar(grupo: GrupoUsuario) {
    this.cambiarEstado(grupo, true);
  }

  campoConError(nombre: 'nombre') {
    const control = this.formulario.controls[nombre];
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  mensajeCampo(nombre: 'nombre') {
    const control = this.formulario.controls[nombre];
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('maxlength')) return 'Usá hasta 100 caracteres.';
    return '';
  }

  private cambiarEstado(grupo: GrupoUsuario, activar: boolean) {
    this.limpiarAvisos();
    const solicitud = activar
      ? this.gruposService.activarGrupo(grupo.id)
      : this.gruposService.desactivarGrupo(grupo.id);
    solicitud.subscribe({
      next: (actualizado) => {
        const mensaje = activar ? 'Grupo activado correctamente.' : 'Grupo desactivado correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.reemplazarGrupo(actualizado);
        if (this.grupoSeleccionado()?.id === actualizado.id) this.aplicarSeleccion(actualizado);
      },
      error: (error) => this.fallar(error, 'No fue posible cambiar el estado del grupo'),
    });
  }

  private aplicarSeleccion(grupo: GrupoUsuario) {
    this.grupoSeleccionado.set(grupo);
    this.miembrosSeleccionados.set(new Set(grupo.miembros.map((miembro) => miembro.idUsuario)));
    this.rolesSeleccionados.set(new Set(grupo.roles.map((rol) => rol.idRol)));
  }

  private reemplazarGrupo(grupo: GrupoUsuario) {
    this.grupos.update((grupos) => grupos.map((actual) => (actual.id === grupo.id ? grupo : actual)));
  }

  private cancelarFormulario() {
    this.intentoEnvio.set(false);
    this.editando.set(null);
    this.formulario.reset({ nombre: '', descripcion: '' });
  }

  private mensajeFormularioInvalido() {
    if (this.formulario.controls.nombre.invalid) return this.mensajeCampo('nombre');
    return 'Revisá los datos del grupo.';
  }

  private fallar(error: unknown, respaldo: string) {
    const mensaje = obtenerMensajeError(error, respaldo);
    this.error.set(mensaje);
    this.toast.error(mensaje);
  }

  private limpiarAvisos() {
    this.mensaje.set('');
    this.error.set('');
  }
}
