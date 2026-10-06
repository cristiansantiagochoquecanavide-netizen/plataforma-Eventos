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
    '/autenticacion/recuperar-clave',
    '/autenticacion/solicitar-recuperacion-clave',
    '/autenticacion/restablecer-clave',
    '/api/saas/',
  ].some((ruta) => solicitud.url.includes(ruta));
  const token = sesion.tokenAcceso();
  const headers: Record<string, string> = {};
  if (token && !esPublica) headers['Authorization'] = `Bearer ${token}`;
  const organizacionId = sesion.organizacionActivaId();
  if (organizacionId && !esPublica) headers['X-Organization-Id'] = String(organizacionId);
  const autorizada =
    Object.keys(headers).length > 0
      ? solicitud.clone({ setHeaders: headers })
      : solicitud;

  return siguiente(autorizada).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || esPublica || !token) return throwError(() => error);
      return sesion.renovar().pipe(
        switchMap((nuevaSesion) =>
          siguiente(
            solicitud.clone({
              setHeaders: {
                Authorization: `Bearer ${nuevaSesion.accessToken}`,
                ...(sesion.organizacionActivaId()
                  ? { 'X-Organization-Id': String(sesion.organizacionActivaId()) }
                  : {}),
              },
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
