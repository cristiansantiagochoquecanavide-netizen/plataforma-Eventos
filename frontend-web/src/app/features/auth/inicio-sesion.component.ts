import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-inicio-sesion',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './inicio-sesion.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioSesionComponent {
  readonly error = signal('');
  readonly enviando = signal(false);
  readonly intentoEnvio = signal(false);
  readonly formulario = new FormBuilder().nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    clave: ['', [Validators.required]],
  });

  constructor(
    private readonly sesion: SesionService,
    private readonly router: Router,
    private readonly toast: ToastService,
  ) {}

  enviar() {
    this.intentoEnvio.set(true);
    if (this.enviando()) return;
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      const mensaje = this.mensajeFormularioInvalido();
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    this.error.set('');
    this.enviando.set(true);
    const { correo, clave } = this.formulario.getRawValue();
    this.sesion.iniciar(correo, clave).subscribe({
      next: (usuario) => {
        this.toast.exito('Inicio de sesion exitoso.');
        this.router.navigateByUrl(usuario.roles.includes('SAAS_ADMINISTRADOR') ? '/saas/organizaciones' : '/perfil');
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible iniciar sesion');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.enviando.set(false);
      },
      complete: () => this.enviando.set(false),
    });
  }

  correoConError() {
    const control = this.formulario.controls.correo;
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  claveConError() {
    const control = this.formulario.controls.clave;
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  mensajeCorreo() {
    const control = this.formulario.controls.correo;
    if (control.hasError('required')) return 'El correo es obligatorio.';
    if (control.hasError('email')) return 'El correo no tiene formato valido.';
    return '';
  }

  mensajeClave() {
    return this.formulario.controls.clave.hasError('required') ? 'La clave es obligatoria.' : '';
  }

  private mensajeFormularioInvalido() {
    if (this.formulario.controls.correo.invalid) return this.mensajeCorreo();
    if (this.formulario.controls.clave.invalid) return this.mensajeClave();
    return 'Completa los campos obligatorios para continuar.';
  }
}
