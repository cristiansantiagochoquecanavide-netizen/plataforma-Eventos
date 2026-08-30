import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { Rol, Usuario } from '../../core/modelos';
import { ToastService } from '../../core/toast.service';
import { mensajeClaveInvalida, validadorClaveSegura } from '../../core/validacion-clave';
import { RequisitosClaveComponent } from '../../shared/requisitos-clave.component';

@Component({
  selector: 'app-usuarios',
  imports: [ReactiveFormsModule, RequisitosClaveComponent],
  templateUrl: './usuarios.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsuariosComponent implements OnInit {
  readonly usuarios = signal<Usuario[]>([]);
  readonly roles = signal<Rol[]>([]);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly modalAbierto = signal(false);
  readonly editando = signal<Usuario | null>(null);
  readonly guardando = signal(false);
  readonly idCambiandoEstado = signal<number | null>(null);
  readonly intentoEnvio = signal(false);
  readonly formulario = new FormBuilder().nonNullable.group({
    nombres: ['', Validators.required],
    apellidos: ['', Validators.required],
    correo: ['', [Validators.required, Validators.email]],
    clave: ['', [Validators.required, Validators.maxLength(72), validadorClaveSegura()]],
    roles: ['PARTICIPANTE', Validators.required],
  });

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.cargar();
    this.api.listarRoles().subscribe({
      next: (roles) => this.roles.set(roles),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar roles');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  cargar() {
    this.api.listarUsuarios().subscribe({
      next: (pagina) => this.usuarios.set(pagina.elementos),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar usuarios');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  abrirModalCrear() {
    this.cancelar();
    this.limpiarAvisos();
    this.modalAbierto.set(true);
  }

  cerrarModal() {
    this.modalAbierto.set(false);
    this.cancelar();
  }

  editar(usuario: Usuario) {
    this.limpiarAvisos();
    this.intentoEnvio.set(false);
    this.editando.set(usuario);
    this.formulario.patchValue({ ...usuario, clave: '', roles: usuario.roles.join(', ') });
    this.formulario.controls.clave.clearValidators();
    this.formulario.controls.clave.updateValueAndValidity();
    this.modalAbierto.set(true);
  }

  cancelar() {
    this.intentoEnvio.set(false);
    this.editando.set(null);
    this.formulario.reset({
      nombres: '',
      apellidos: '',
      correo: '',
      clave: '',
      roles: 'PARTICIPANTE',
    });
    this.formulario.controls.clave.setValidators([
      Validators.required,
      Validators.maxLength(72),
      validadorClaveSegura(),
    ]);
    this.formulario.controls.clave.updateValueAndValidity();
  }

  guardar() {
    this.intentoEnvio.set(true);
    if (this.guardando()) return;
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      const mensaje = this.mensajeFormularioInvalido();
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    this.limpiarAvisos();
    this.guardando.set(true);
    const valor = this.formulario.getRawValue();
    const roles = valor.roles
      .split(',')
      .map((rol) => rol.trim().toUpperCase())
      .filter(Boolean);
    const editando = this.editando();
    const solicitud = editando
      ? this.api.actualizarUsuario(editando.id, {
          nombres: valor.nombres,
          apellidos: valor.apellidos,
          correo: valor.correo,
          roles,
        })
      : this.api.crearUsuario({ ...valor, roles });
    solicitud.subscribe({
      next: () => {
        const mensaje = editando
          ? 'Usuario actualizado correctamente.'
          : 'Usuario creado correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.cerrarModal();
        this.cargar();
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible guardar el usuario');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.guardando.set(false);
      },
      complete: () => this.guardando.set(false),
    });
  }

  alternarEstado(usuario: Usuario) {
    if (this.idCambiandoEstado()) return;
    this.limpiarAvisos();
    this.idCambiandoEstado.set(usuario.id);
    const estado = usuario.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.api.cambiarEstadoUsuario(usuario.id, estado).subscribe({
      next: () => {
        const mensaje = `Usuario ${estado.toLowerCase()} correctamente.`;
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.cargar();
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cambiar el estado');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.idCambiandoEstado.set(null);
      },
      complete: () => this.idCambiandoEstado.set(null),
    });
  }

  campoConError(nombre: 'nombres' | 'apellidos' | 'correo' | 'clave' | 'roles') {
    const control = this.formulario.controls[nombre];
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  mensajeCampo(nombre: 'nombres' | 'apellidos' | 'correo' | 'clave' | 'roles') {
    const control = this.formulario.controls[nombre];
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('email')) return 'El correo no tiene formato valido.';
    if (control.hasError('maxlength')) return 'La clave no debe superar 72 caracteres.';
    if (control.hasError('claveSegura')) return mensajeClaveInvalida(control.value);
    return '';
  }

  private mensajeFormularioInvalido() {
    const orden: Array<'nombres' | 'apellidos' | 'correo' | 'clave' | 'roles'> = [
      'nombres',
      'apellidos',
      'correo',
      'clave',
      'roles',
    ];
    const campo = orden.find((nombre) => this.formulario.controls[nombre].invalid);
    return campo ? this.mensajeCampo(campo) : 'Revisa los datos del usuario.';
  }

  private limpiarAvisos() {
    this.error.set('');
    this.mensaje.set('');
  }
}
