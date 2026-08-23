import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { SesionService } from '../core/sesion.service';

@Component({
  selector: 'app-perfil',
  imports: [ReactiveFormsModule],
  templateUrl: './perfil.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PerfilComponent implements OnInit {
  readonly mensaje = signal('');
  readonly error = signal('');
  readonly perfil = new FormBuilder().nonNullable.group({
    nombres: ['', Validators.required],
    apellidos: ['', Validators.required],
    correo: ['', [Validators.required, Validators.email]],
  });
  readonly clave = new FormBuilder().nonNullable.group({
    claveActual: ['', Validators.required],
    nuevaClave: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
      ],
    ],
  });

  constructor(
    private readonly api: ApiService,
    private readonly sesion: SesionService,
  ) {}

  ngOnInit() {
    this.api.obtenerPerfil().subscribe({
      next: (usuario) => this.perfil.patchValue(usuario),
      error: (error) => this.error.set(error.error?.message ?? 'No fue posible cargar el perfil'),
    });
  }

  guardarPerfil() {
    if (this.perfil.invalid) return;
    this.limpiarAvisos();
    this.api.actualizarPerfil(this.perfil.getRawValue()).subscribe({
      next: (usuario) => {
        this.sesion.usuario.set(usuario);
        this.mensaje.set('Datos del perfil actualizados.');
      },
      error: (error) =>
        this.error.set(error.error?.message ?? 'No fue posible actualizar el perfil'),
    });
  }

  cambiarClave() {
    if (this.clave.invalid) return;
    this.limpiarAvisos();
    const { claveActual, nuevaClave } = this.clave.getRawValue();
    this.api.cambiarClave(claveActual, nuevaClave).subscribe({
      next: () => {
        this.clave.reset();
        this.mensaje.set('Clave actualizada. Inicia sesion nuevamente en tus otros dispositivos.');
      },
      error: (error) =>
        this.error.set(error.error?.message ?? 'No fue posible actualizar la clave'),
    });
  }

  private limpiarAvisos() {
    this.mensaje.set('');
    this.error.set('');
  }
}
