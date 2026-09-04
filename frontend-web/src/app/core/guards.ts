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

export const eventosGuard: CanActivateFn = recursosGuard;

export const espaciosGuard: CanActivateFn = recursosGuard;

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
