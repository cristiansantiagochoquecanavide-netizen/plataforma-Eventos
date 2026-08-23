import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { SesionService } from '../core/sesion.service';

@Component({
  selector: 'app-inicio-sesion',
  imports: [ReactiveFormsModule],
  templateUrl: './inicio-sesion.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InicioSesionComponent {
  readonly error = signal('');
  readonly enviando = signal(false);
  readonly formulario = new FormBuilder().nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    clave: ['', [Validators.required]],
  });

  constructor(
    private readonly sesion: SesionService,
    private readonly router: Router,
  ) {}

  enviar() {
    if (this.formulario.invalid || this.enviando()) return;
    this.error.set('');
    this.enviando.set(true);
    const { correo, clave } = this.formulario.getRawValue();
    this.sesion.iniciar(correo, clave).subscribe({
      next: () => this.router.navigateByUrl('/perfil'),
      error: (error) => {
        this.error.set(error.error?.message ?? 'No fue posible iniciar sesion');
        this.enviando.set(false);
      },
      complete: () => this.enviando.set(false),
    });
  }
}
