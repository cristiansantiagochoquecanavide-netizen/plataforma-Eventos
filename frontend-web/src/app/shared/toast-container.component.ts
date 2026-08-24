import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-toast-container',
  template: `
    <section class="toast-stack" aria-live="polite" aria-label="Notificaciones">
      @for (mensaje of toast.mensajes(); track mensaje.id) {
        <article class="toast" [class]="mensaje.tipo">
          <div>
            <strong>{{ mensaje.titulo }}</strong>
            <p>{{ mensaje.mensaje }}</p>
          </div>
          <button type="button" (click)="toast.cerrar(mensaje.id)" aria-label="Cerrar mensaje">
            x
          </button>
        </article>
      }
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastContainerComponent {
  constructor(readonly toast: ToastService) {}
}
