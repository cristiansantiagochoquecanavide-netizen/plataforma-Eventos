import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { obtenerMensajeError } from '../core/mensajes-error';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-recuperar-clave',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './recuperar-clave.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecuperarClaveComponent {
  private readonly mensajeCorreoInvalido = 'Ingresa un correo electr\u00f3nico v\u00e1lido.';
  private readonly mensajeSolicitudCorrecta = 'Se ha enviado la solicitud de recuperaci\u00f3n.';

  readonly error = signal('');
  readonly mensaje = signal('');
  readonly enviando = signal(false);
  readonly intentoEnvio = signal(false);
  readonly formulario = new FormBuilder().nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
  });

  constructor(
    private readonly api: ApiService,
    private readonly toast: ToastService,
  ) {}

  enviar() {
    this.intentoEnvio.set(true);
    if (this.enviando()) return;
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.error.set(this.mensajeCorreoInvalido);
      this.mensaje.set('');
      this.toast.error(this.mensajeCorreoInvalido);
      return;
    }

    this.error.set('');
    this.mensaje.set('');
    this.enviando.set(true);
    const { correo } = this.formulario.getRawValue();
    this.api.recuperarClave(correo).subscribe({
      next: () => {
        this.mensaje.set(this.mensajeSolicitudCorrecta);
        this.toast.exito(this.mensajeSolicitudCorrecta);
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible enviar la solicitud');
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

  mensajeCorreo() {
    return this.mensajeCorreoInvalido;
  }
}
