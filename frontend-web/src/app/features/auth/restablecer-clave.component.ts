import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { ToastService } from '../../core/toast.service';
import { validadorClaveSegura } from '../../core/validacion-clave';
import { RequisitosClaveComponent } from '../../shared/requisitos-clave.component';

@Component({
  selector: 'app-restablecer-clave',
  imports: [ReactiveFormsModule, RouterLink, RequisitosClaveComponent],
  templateUrl: './restablecer-clave.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RestablecerClaveComponent implements OnInit {
  private readonly mensajeClaveDebil = 'La contrase\u00f1a no cumple los requisitos.';
  private readonly mensajeClavesDiferentes = 'Las contrase\u00f1as no coinciden.';

  readonly error = signal('');
  readonly enviando = signal(false);
  readonly intentoEnvio = signal(false);
  readonly tokenDesdeUrl = signal(false);
  readonly formulario = new FormBuilder().nonNullable.group(
    {
      token: ['', Validators.required],
      nuevaClave: ['', [Validators.required, Validators.maxLength(72), validadorClaveSegura()]],
      confirmarClave: ['', Validators.required],
    },
    { validators: clavesCoinciden },
  );

  constructor(
    private readonly api: ApiService,
    private readonly ruta: ActivatedRoute,
    private readonly router: Router,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    const token = this.ruta.snapshot.queryParamMap.get('token')?.trim() ?? '';
    this.tokenDesdeUrl.set(!!token);
    this.formulario.patchValue({ token });
  }

  restablecer() {
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
    const { token, nuevaClave } = this.formulario.getRawValue();
    this.api.restablecerClave(token, nuevaClave).subscribe({
      next: () => {
        this.toast.exito('Contrase\u00f1a restablecida correctamente.');
        this.router.navigateByUrl('/iniciar-sesion');
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible restablecer la clave');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.enviando.set(false);
      },
      complete: () => this.enviando.set(false),
    });
  }

  tokenConError() {
    const control = this.formulario.controls.token;
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  nuevaClaveConError() {
    const control = this.formulario.controls.nuevaClave;
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  confirmacionConError() {
    const control = this.formulario.controls.confirmarClave;
    return (
      (control.invalid || this.formulario.hasError('clavesDiferentes')) &&
      (control.touched || this.intentoEnvio())
    );
  }

  mensajeToken() {
    return 'Ingresa el codigo de recuperacion.';
  }

  mensajeNuevaClave() {
    const control = this.formulario.controls.nuevaClave;
    if (control.hasError('required')) return 'La nueva contrase\u00f1a es obligatoria.';
    if (control.hasError('maxlength')) return 'La nueva contrase\u00f1a no debe superar 72 caracteres.';
    if (control.hasError('claveSegura')) return this.mensajeClaveDebil;
    return '';
  }

  mensajeConfirmacion() {
    const control = this.formulario.controls.confirmarClave;
    if (control.hasError('required')) return 'Confirma la nueva contrase\u00f1a.';
    if (this.formulario.hasError('clavesDiferentes')) return this.mensajeClavesDiferentes;
    return '';
  }

  private mensajeFormularioInvalido() {
    if (this.formulario.controls.token.invalid) return this.mensajeToken();
    if (this.formulario.controls.nuevaClave.invalid) return this.mensajeNuevaClave();
    if (
      this.formulario.controls.confirmarClave.invalid ||
      this.formulario.hasError('clavesDiferentes')
    ) {
      return this.mensajeConfirmacion();
    }
    return 'Revisa la nueva contrase\u00f1a.';
  }
}

function clavesCoinciden(control: AbstractControl): ValidationErrors | null {
  const nuevaClave = control.get('nuevaClave')?.value;
  const confirmarClave = control.get('confirmarClave')?.value;

  if (!nuevaClave || !confirmarClave) return null;
  return nuevaClave === confirmarClave ? null : { clavesDiferentes: true };
}
