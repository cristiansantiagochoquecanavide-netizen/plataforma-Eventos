import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { Permiso, Rol } from '../../core/modelos';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-roles',
  imports: [ReactiveFormsModule],
  templateUrl: './roles.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RolesComponent implements OnInit {
  readonly roles = signal<Rol[]>([]);
  readonly permisos = signal<Permiso[]>([]);
  readonly rolSeleccionado = signal<Rol | null>(null);
  readonly permisosSeleccionados = signal<Set<string>>(new Set());
  readonly editando = signal<Rol | null>(null);
  readonly mensaje = signal('');
  readonly error = signal('');
  readonly guardandoRol = signal(false);
  readonly guardandoPermisos = signal(false);
  readonly intentoEnvio = signal(false);
  readonly formulario = new FormBuilder().nonNullable.group({
    nombre: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]{1,49}$/)]],
    descripcion: [''],
    estado: ['ACTIVO'],
  });

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.cargar();
    this.api.listarPermisos().subscribe({
      next: (permisos) => this.permisos.set(permisos),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar permisos');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  cargar() {
    this.api
      .listarRoles()
      .subscribe({
        next: (roles) => this.roles.set(roles),
        error: (error) => {
          const mensaje = obtenerMensajeError(error, 'No fue posible cargar roles');
          this.error.set(mensaje);
          this.toast.error(mensaje);
        },
      });
  }

  editar(rol: Rol) {
    this.limpiarAvisos();
    this.intentoEnvio.set(false);
    this.editando.set(rol);
    this.formulario.patchValue({ ...rol, descripcion: rol.descripcion ?? '' });
  }

  cancelar() {
    this.intentoEnvio.set(false);
    this.editando.set(null);
    this.formulario.reset({ nombre: '', descripcion: '', estado: 'ACTIVO' });
  }

  guardarRol() {
    this.intentoEnvio.set(true);
    if (this.guardandoRol()) return;
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      const mensaje = this.mensajeFormularioInvalido();
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    this.limpiarAvisos();
    this.guardandoRol.set(true);
    const datos = this.formulario.getRawValue();
    const editando = this.editando();
    const solicitud = editando
      ? this.api.actualizarRol(editando.id, datos)
      : this.api.crearRol(datos);
    solicitud.subscribe({
      next: () => {
        const mensaje = editando ? 'Rol actualizado correctamente.' : 'Rol creado correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.cancelar();
        this.cargar();
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible guardar el rol');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.guardandoRol.set(false);
      },
      complete: () => this.guardandoRol.set(false),
    });
  }

  seleccionarRol(rol: Rol) {
    this.limpiarAvisos();
    this.rolSeleccionado.set(rol);
    this.api.permisosRol(rol.id).subscribe({
      next: (respuesta) =>
        this.permisosSeleccionados.set(
          new Set(respuesta.permisos.map((permiso) => permiso.nombre)),
        ),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible consultar los permisos');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  alternarPermiso(nombre: string, marcado: boolean) {
    const seleccion = new Set(this.permisosSeleccionados());
    marcado ? seleccion.add(nombre) : seleccion.delete(nombre);
    this.permisosSeleccionados.set(seleccion);
  }

  guardarPermisos() {
    const rol = this.rolSeleccionado();
    if (!rol || this.guardandoPermisos()) return;
    this.limpiarAvisos();
    this.guardandoPermisos.set(true);
    this.api.asignarPermisos(rol.id, [...this.permisosSeleccionados()]).subscribe({
      next: () => {
        const mensaje = `Permisos de ${rol.nombre} actualizados correctamente.`;
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible asignar permisos');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.guardandoPermisos.set(false);
      },
      complete: () => this.guardandoPermisos.set(false),
    });
  }

  campoConError(nombre: 'nombre' | 'estado') {
    const control = this.formulario.controls[nombre];
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  mensajeCampo(nombre: 'nombre' | 'estado') {
    const control = this.formulario.controls[nombre];
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('pattern')) return 'Usa mayusculas, numeros o guion bajo. Debe iniciar con letra.';
    return '';
  }

  private mensajeFormularioInvalido() {
    if (this.formulario.controls.nombre.invalid) return this.mensajeCampo('nombre');
    if (this.formulario.controls.estado.invalid) return this.mensajeCampo('estado');
    return 'Revisa los datos del rol.';
  }

  private limpiarAvisos() {
    this.mensaje.set('');
    this.error.set('');
  }
}
