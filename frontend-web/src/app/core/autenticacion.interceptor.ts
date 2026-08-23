import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { SesionService } from './sesion.service';

export const autenticacionInterceptor: HttpInterceptorFn = (solicitud, siguiente) => {
  const sesion = inject(SesionService);
  const esPublica = [
    '/autenticacion/registro',
    '/autenticacion/iniciar-sesion',
    '/autenticacion/refrescar',
    '/autenticacion/solicitar-recuperacion-clave',
    '/autenticacion/restablecer-clave',
  ].some((ruta) => solicitud.url.includes(ruta));
  const token = sesion.tokenAcceso();
  const autorizada =
    !esPublica && token
      ? solicitud.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : solicitud;

  return siguiente(autorizada).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || esPublica || !token) return throwError(() => error);
      return sesion.renovar().pipe(
        switchMap((nuevaSesion) =>
          siguiente(
            solicitud.clone({
              setHeaders: { Authorization: `Bearer ${nuevaSesion.accessToken}` },
            }),
          ),
        ),
        catchError((errorRenovacion) => {
          sesion.cerrar();
          return throwError(() => errorRenovacion);
        }),
      );
    }),
  );
};
