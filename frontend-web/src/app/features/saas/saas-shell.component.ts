import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, EMPTY, of, switchMap } from 'rxjs';
import { UiPrivilegesService } from '../../core/ui-privileges.service';
import { SesionService } from '../../core/sesion.service';
import { OrganizacionesService } from '../organizaciones/servicios/organizaciones.service';

@Component({
  selector: 'app-saas-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './saas-shell.component.html',
  styleUrl: './saas-shell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaasShellComponent implements OnInit {
  constructor(
    readonly sesion: SesionService,
    private readonly organizaciones: OrganizacionesService,
    private readonly uiPrivileges: UiPrivilegesService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.organizaciones.listarMias().pipe(
      switchMap((lista) => {
        const guardada = this.sesion.organizacionActivaId();
        const activa = lista.find((organizacion) => organizacion.id === guardada) ?? lista[0];
        if (!activa) return of(undefined);
        return this.sesion.seleccionarOrganizacion(activa.id).pipe(
          switchMap(() => this.uiPrivileges.loadForOrganization(activa.id)),
        );
      }),
      catchError(() => {
        this.uiPrivileges.clear();
        return EMPTY;
      }),
    ).subscribe();
  }

  cerrarSesion(): void {
    this.uiPrivileges.clear();
    this.sesion.cerrar();
  }

  abrirOrganizacion(id: number): void {
    this.sesion.seleccionarOrganizacion(id).subscribe({
      next: () => this.router.navigateByUrl('/perfil'),
      error: () => this.uiPrivileges.clear(),
    });
  }
}
