import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { obtenerMensajeError } from '../core/mensajes-error';
import { SesionService } from '../core/sesion.service';
import { ToastService } from '../core/toast.service';
import { mensajeClaveInvalida, validadorClaveSegura } from '../core/validacion-clave';
import { RequisitosClaveComponent } from '../shared/requisitos-clave.component';

@Component({
  selector: 'app-perfil',
  imports: [ReactiveFormsModule, RequisitosClaveComponent],
  templateUrl: './perfil.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PerfilComponent implements OnInit {
  readonly mensaje = signal('');
  readonly error = signal('');
  readonly guardandoPerfil = signal(false);
  readonly actualizandoClave = signal(false);
  readonly intentoPerfil = signal(false);
  readonly intentoClave = signal(false);
  readonly perfil = new FormBuilder().nonNullable.group({
    nombres: ['', Validators.required],
    apellidos: ['', Validators.required],
    correo: ['', [Validators.required, Validators.email]],
  });
  readonly clave = new FormBuilder().nonNullable.group({
    claveActual: ['', Validators.required],
    nuevaClave: [
      '',
      [Validators.required, Validators.maxLength(72), validadorClaveSegura()],
    ],
  });

  constructor(
    private readonly api: ApiService,
    private readonly sesion: SesionService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.api.obtenerPerfil().subscribe({
      next: (usuario) => this.perfil.patchValue(usuario),
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar el perfil');
        this.error.set(mensaje);
        this.toast.error(mensaje);
      },
    });
  }

  guardarPerfil() {
    this.intentoPerfil.set(true);
    if (this.guardandoPerfil()) return;
    if (this.perfil.invalid) {
      this.perfil.markAllAsTouched();
      const mensaje = this.mensajePerfilInvalido();
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    this.limpiarAvisos();
    this.guardandoPerfil.set(true);
    this.api.actualizarPerfil(this.perfil.getRawValue()).subscribe({
      next: (usuario) => {
        this.sesion.usuario.set(usuario);
        const mensaje = 'Perfil actualizado correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible actualizar el perfil');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.guardandoPerfil.set(false);
      },
      complete: () => this.guardandoPerfil.set(false),
    });
  }

  cambiarClave() {
    this.intentoClave.set(true);
    if (this.actualizandoClave()) return;
    if (this.clave.invalid) {
      this.clave.markAllAsTouched();
      const mensaje = this.mensajeFormularioClaveInvalido();
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    this.limpiarAvisos();
    this.actualizandoClave.set(true);
    const { claveActual, nuevaClave } = this.clave.getRawValue();
    this.api.cambiarClave(claveActual, nuevaClave).subscribe({
      next: () => {
        this.clave.reset();
        const mensaje = 'Contrasena actualizada correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible actualizar la clave');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.actualizandoClave.set(false);
      },
      complete: () => this.actualizandoClave.set(false),
    });
  }

  campoPerfilConError(nombre: 'nombres' | 'apellidos' | 'correo') {
    const control = this.perfil.controls[nombre];
    return control.invalid && (control.touched || this.intentoPerfil());
  }

  claveActualConError() {
    const control = this.clave.controls.claveActual;
    return control.invalid && (control.touched || this.intentoClave());
  }

  nuevaClaveConError() {
    const control = this.clave.controls.nuevaClave;
    return control.invalid && (control.touched || this.intentoClave());
  }

  mensajeCampoPerfil(nombre: 'nombres' | 'apellidos' | 'correo') {
    const control = this.perfil.controls[nombre];
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('email')) return 'El correo no tiene formato valido.';
    return '';
  }

  mensajeNuevaClave() {
    const control = this.clave.controls.nuevaClave;
    if (control.hasError('required')) return 'La nueva clave es obligatoria.';
    if (control.hasError('maxlength')) return 'La nueva clave no debe superar 72 caracteres.';
    if (control.hasError('claveSegura')) return mensajeClaveInvalida(control.value);
    return '';
  }

  private mensajePerfilInvalido() {
    if (this.perfil.controls.correo.invalid) return this.mensajeCampoPerfil('correo');
    if (this.perfil.controls.nombres.invalid) return this.mensajeCampoPerfil('nombres');
    if (this.perfil.controls.apellidos.invalid) return this.mensajeCampoPerfil('apellidos');
    return 'Revisa los datos del perfil.';
  }

  private mensajeFormularioClaveInvalido() {
    if (this.clave.controls.claveActual.invalid) return 'La clave actual es obligatoria.';
    if (this.clave.controls.nuevaClave.invalid) return this.mensajeNuevaClave();
    return 'Revisa la nueva clave.';
  }

  private limpiarAvisos() {
    this.mensaje.set('');
    this.error.set('');
  }
}
