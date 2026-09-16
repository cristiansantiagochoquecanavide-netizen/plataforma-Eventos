import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SesionService } from '../../core/sesion.service';
import { OrganizacionesService } from '../organizaciones/servicios/organizaciones.service';
import { Organizacion } from '../organizaciones/modelos/organizacion.modelo';

@Component({
  selector: 'app-panel',
  imports: [FormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanelComponent implements OnInit {
  organizaciones: Organizacion[] = [];

  constructor(
    readonly sesion: SesionService,
    private readonly organizacionesService: OrganizacionesService,
    private readonly router: Router,
  ) {}

  ngOnInit() {
    this.organizacionesService.listarMias().subscribe({
      next: (organizaciones) => {
        this.organizaciones = organizaciones;
        const guardada = this.sesion.organizacionActivaId();
        const valida = organizaciones.find((organizacion) => organizacion.id === guardada) ?? organizaciones[0];
        if (valida && valida.id !== guardada) this.sesion.seleccionarOrganizacion(valida.id);
        if (!valida) {
          this.sesion.organizacionActivaId.set(null);
          localStorage.removeItem('plataforma-eventos.organizacion-activa');
        }
      },
    });
  }

  seleccionarOrganizacion(id: number) {
    if (!this.organizaciones.some((organizacion) => organizacion.id === id)) return;
    if (id === this.sesion.organizacionActivaId()) return;
    this.sesion.seleccionarOrganizacion(id);
    this.router.navigateByUrl('/perfil');
  }
}
