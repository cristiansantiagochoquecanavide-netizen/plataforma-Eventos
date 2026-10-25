import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { catchError, map, of } from 'rxjs';
import { SesionService } from './sesion.service';

export const autenticacionGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion
    .restaurar()
    .pipe(map((autenticado) => autenticado || router.createUrlTree(['/iniciar-sesion'])));
};

export const administradorGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.restaurar().pipe(
    map((autenticado) =>
      autenticado && sesion.esAdministrador() ? true : router.createUrlTree(['/perfil']),
    ),
    catchError(() => of(router.createUrlTree(['/iniciar-sesion']))),
  );
};

export const usuariosGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.restaurar().pipe(
    map((autenticado) =>
      autenticado && sesion.tieneAccesoUsuarios() ? true : router.createUrlTree(['/perfil']),
    ),
    catchError(() => of(router.createUrlTree(['/iniciar-sesion']))),
  );
};

export const rolesGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.restaurar().pipe(
    map((autenticado) =>
      autenticado && sesion.esAdministradorSaas() ? true : router.createUrlTree(['/perfil']),
    ),
    catchError(() => of(router.createUrlTree(['/iniciar-sesion']))),
  );
};

export const recursosGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.restaurar().pipe(
    map((autenticado) =>
      autenticado && sesion.tieneAccesoRecursos() ? true : router.createUrlTree(['/perfil']),
    ),
    catchError(() => of(router.createUrlTree(['/iniciar-sesion']))),
  );
};

export const eventosGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.restaurar().pipe(
    map((autenticado) =>
      autenticado && sesion.tieneAccesoEventos() ? true : router.createUrlTree(['/perfil']),
    ),
    catchError(() => of(router.createUrlTree(['/iniciar-sesion']))),
  );
};

export const consultaEventosGuard = permisoGuard((s) => s.puedeVerEventos());

export const espaciosGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.restaurar().pipe(
    map((autenticado) =>
      autenticado && sesion.tieneAccesoEspacios() ? true : router.createUrlTree(['/perfil']),
    ),
    catchError(() => of(router.createUrlTree(['/iniciar-sesion']))),
  );
};

export const convocatoriasGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.restaurar().pipe(
    map((autenticado) =>
      autenticado && sesion.tieneAccesoConvocatorias()
        ? true
        : router.createUrlTree(['/perfil']),
    ),
    catchError(() => of(router.createUrlTree(['/iniciar-sesion']))),
  );
};

function permisoGuard(autorizado: (sesion: SesionService) => boolean): CanActivateFn {
  return () => {
    const sesion = inject(SesionService);
    const router = inject(Router);
    return sesion.restaurar().pipe(map((autenticado) => autenticado && autorizado(sesion) ? true : router.createUrlTree(['/perfil'])), catchError(() => of(router.createUrlTree(['/iniciar-sesion']))));
  };
}

export const postulacionesGuard = permisoGuard((s) => s.puedeCrearPostulacionesStaff() || s.puedeGestionarPostulacionesStaff());
export const asignacionesGuard = permisoGuard((s) => s.puedeVerAsignacionesStaff());
export const bitacoraGuard = permisoGuard((s) => s.puedeVerBitacora());
export const miembrosGuard = permisoGuard((s) => s.puedeGestionarMiembros());
export const participantesGuard = permisoGuard((s) => s.puedeVerParticipantes());
export const asistenciasGuard = permisoGuard((s) => s.puedeRegistrarAsistencia());
export const reportesGuard = permisoGuard((s) => s.tieneAccesoEventos() || s.puedeVerParticipantes() || s.esAdministrador());
