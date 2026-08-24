import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { obtenerRequisitosClave } from '../core/validacion-clave';

@Component({
  selector: 'app-requisitos-clave',
  template: `
    <ul class="password-checklist" aria-live="polite">
      @for (requisito of requisitos(); track requisito.id) {
        <li [class.ok]="requisito.valido">
          <span aria-hidden="true">{{ requisito.valido ? '✓' : 'x' }}</span>
          {{ requisito.texto }}
        </li>
      }
    </ul>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RequisitosClaveComponent {
  readonly clave = input('');

  requisitos() {
    return obtenerRequisitosClave(this.clave());
  }
}
