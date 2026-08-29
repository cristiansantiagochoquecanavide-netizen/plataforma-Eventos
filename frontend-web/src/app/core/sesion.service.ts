import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { ApiService } from './api.service';
import { SesionRespuesta, Usuario } from './modelos';

const CLAVE_SESION = 'plataforma-eventos.sesion';

@Injectable({ providedIn: 'root' })
export class SesionService {
  readonly usuario = signal<Usuario | null>(null);
  private sesion: SesionRespuesta | null = this.leerSesion();

  constructor(
    private readonly api: ApiService,
    private readonly router: Router,
  ) {}

  iniciar(correo: string, clave: string): Observable<Usuario> {
    return this.api.iniciarSesion(correo, clave).pipe(
      tap((sesion) => this.guardarSesion(sesion)),
      switchMap(() => this.api.usuarioActual()),
      tap((usuario) => this.usuario.set(usuario)),
    );
  }

  restaurar(): Observable<boolean> {
    if (!this.sesion) return of(false);
    return this.api.usuarioActual().pipe(
      tap((usuario) => this.usuario.set(usuario)),
      map(() => true),
      catchError(() => {
        this.limpiarSesion();
        return of(false);
      }),
    );
  }

  renovar(): Observable<SesionRespuesta> {
    if (!this.sesion) throw new Error('No existe una sesion para renovar');
    return this.api
      .refrescar(this.sesion.refreshToken)
      .pipe(tap((sesion) => this.guardarSesion(sesion)));
  }

  tokenAcceso() {
    return this.sesion?.accessToken ?? null;
  }

  esAdministrador() {
    return this.usuario()?.roles.includes('ADMINISTRADOR') ?? false;
  }

  tieneAccesoRecursos() {
    const roles = this.usuario()?.roles ?? [];
    return roles.includes('ADMINISTRADOR') || roles.includes('ORGANIZADOR');
  }

  cerrar() {
    const token = this.sesion?.refreshToken;
    this.limpiarSesion();
    if (token)
      this.api
        .cerrarSesion(token)
        .pipe(catchError(() => of(undefined)))
        .subscribe();
    this.router.navigateByUrl('/iniciar-sesion');
  }

  private guardarSesion(sesion: SesionRespuesta) {
    this.sesion = sesion;
    localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
  }

  private limpiarSesion() {
    this.sesion = null;
    this.usuario.set(null);
    localStorage.removeItem(CLAVE_SESION);
  }

  private leerSesion(): SesionRespuesta | null {
    const valor = localStorage.getItem(CLAVE_SESION);
    if (!valor) return null;
    try {
      return JSON.parse(valor) as SesionRespuesta;
    } catch {
      localStorage.removeItem(CLAVE_SESION);
      return null;
    }
  }
}
