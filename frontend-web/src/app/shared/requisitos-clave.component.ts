import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { obtenerRequisitosClave } from '../core/validacion-clave';

@Component({
  selector: 'app-requisitos-clave',
  template: `
    <div class="password-checklist-wrapper" aria-live="polite">
      <div class="strength-track" aria-hidden="true">
        <div class="strength-segment" [class.active]="cumplidos() >= 1" [class.strong]="cumplidos() === 4"></div>
        <div class="strength-segment" [class.active]="cumplidos() >= 2" [class.strong]="cumplidos() === 4"></div>
        <div class="strength-segment" [class.active]="cumplidos() >= 3" [class.strong]="cumplidos() === 4"></div>
        <div class="strength-segment" [class.active]="cumplidos() >= 4" [class.strong]="cumplidos() === 4"></div>
      </div>

      <ul class="password-checklist">
        @for (requisito of requisitos(); track requisito.id) {
          <li [class.ok]="requisito.valido">
            <span class="indicator-icon" aria-hidden="true">
              @if (requisito.valido) {
                <svg viewBox="0 0 14 14" width="9" height="9" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="2.5 7.5 5.5 10.5 11.5 3.5" />
                </svg>
              }
            </span>
            <span class="req-label">{{ requisito.texto }}</span>
          </li>
        }
      </ul>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RequisitosClaveComponent {
  readonly clave = input('');

  readonly requisitos = computed(() => obtenerRequisitosClave(this.clave() || ''));

  readonly cumplidos = computed(() => this.requisitos().filter((r) => r.valido).length);
}
