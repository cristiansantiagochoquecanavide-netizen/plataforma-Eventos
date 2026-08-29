import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SesionService } from '../../core/sesion.service';

@Component({
  selector: 'app-panel',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanelComponent {
  constructor(readonly sesion: SesionService) {}
}
