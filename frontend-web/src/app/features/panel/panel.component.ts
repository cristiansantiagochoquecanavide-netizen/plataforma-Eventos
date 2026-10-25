import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, EMPTY, Observable, of, switchMap } from 'rxjs';
import { SesionService } from '../../core/sesion.service';
import { UiPrivilegeVisibleDirective } from '../../core/ui-privilege.directive';
import { UiPrivilegesService } from '../../core/ui-privileges.service';
import { OrganizacionesService } from '../organizaciones/servicios/organizaciones.service';
import { Organizacion } from '../organizaciones/modelos/organizacion.modelo';

@Component({
  selector: 'app-panel',
  imports: [FormsModule, RouterLink, RouterLinkActive, RouterOutlet, UiPrivilegeVisibleDirective],
  templateUrl: './panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanelComponent implements OnInit {
  organizaciones: Organizacion[] = [];

  constructor(
    readonly sesion: SesionService,
    private readonly organizacionesService: OrganizacionesService,
    private readonly router: Router,
    private readonly uiPrivileges: UiPrivilegesService,
  ) {}

  ngOnInit() {
    this.organizacionesService.listarMias().subscribe({
      next: (organizaciones) => {
        this.organizaciones = organizaciones;
        this.uiPrivileges.clear();

        const guardada = this.sesion.organizacionActivaId();
        const valida = organizaciones.find((organizacion) => organizacion.id === guardada) ?? organizaciones[0];

        if (valida) {
          this.activarOrganizacion(valida.id, false).subscribe();
          return;
        }

        this.limpiarOrganizacionActiva();
      },
      error: () => this.uiPrivileges.clear(),
    });
  }

  seleccionarOrganizacion(id: number) {
    if (!this.organizaciones.some((organizacion) => organizacion.id === id)) return;
    if (id === this.sesion.organizacionActivaId()) return;

    this.activarOrganizacion(id, true).subscribe({
      next: () => this.router.navigateByUrl('/perfil'),
    });
  }

  cerrarSesion() {
    this.uiPrivileges.clear();
    this.sesion.cerrar();
  }

  private activarOrganizacion(id: number, clearPolicy: boolean): Observable<void> {
    if (clearPolicy) this.uiPrivileges.clear();

    return this.sesion.seleccionarOrganizacion(id).pipe(
      switchMap(() =>
        this.uiPrivileges.loadForOrganization(id).pipe(
          catchError(() => {
            this.uiPrivileges.clear();
            return of(undefined);
          }),
        ),
      ),
      catchError(() => {
        this.uiPrivileges.clear();
        return EMPTY;
      }),
    );
  }

  private limpiarOrganizacionActiva() {
    this.sesion.organizacionActivaId.set(null);
    localStorage.removeItem('plataforma-eventos.organizacion-activa');
    this.uiPrivileges.clear();
  }
}
