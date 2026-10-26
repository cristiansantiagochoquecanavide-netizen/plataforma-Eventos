import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../features/auth/servicios/auth.service';
import { SesionRespuesta } from '../features/auth/modelos/auth.modelo';
import { Usuario } from '../features/usuarios/modelos/usuario.modelo';

const CLAVE_SESION = 'plataforma-eventos.sesion';
const CLAVE_ORGANIZACION = 'plataforma-eventos.organizacion-activa';

@Injectable({ providedIn: 'root' })
export class SesionService {
  readonly usuario = signal<Usuario | null>(null);
  readonly organizacionActivaId = signal<number | null>(this.leerOrganizacionActiva());
  private sesion: SesionRespuesta | null = this.leerSesion();

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  iniciar(correo: string, clave: string): Observable<Usuario> {
    return this.auth.iniciarSesion(correo, clave).pipe(
      tap((sesion) => this.guardarSesion(sesion)),
      switchMap(() => this.auth.usuarioActual()),
      tap((usuario) => this.usuario.set(usuario)),
    );
  }

  restaurar(): Observable<boolean> {
    if (!this.sesion) return of(false);
    return this.auth.usuarioActual().pipe(
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
    return this.auth
      .refrescar(this.sesion.refreshToken)
      .pipe(tap((sesion) => this.guardarSesion(sesion)));
  }

  tokenAcceso() {
    return this.sesion?.accessToken ?? null;
  }

  seleccionarOrganizacion(id: number): Observable<Usuario | null> {
    this.organizacionActivaId.set(id);
    localStorage.setItem(CLAVE_ORGANIZACION, String(id));
    if (!this.sesion) return of(null);
    return this.auth.usuarioActual().pipe(tap((usuario) => this.usuario.set(usuario)));
  }

  esAdministrador() {
    return this.usuario()?.perfilOrganizacionActiva?.administrador ?? false;
  }

  esAdministradorSaas() {
    return this.usuario()?.roles.includes('SAAS_ADMINISTRADOR') ?? false;
  }

  rolParaCabecera(): string {
    const usuario = this.usuario();
    if (!usuario) return 'Sin rol';

    const rolesTenant = usuario.perfilOrganizacionActiva?.roles ?? [];
    if (usuario.roles.includes('SAAS_ADMINISTRADOR')) {
      return ['ADMINISTRADOR SAAS', ...rolesTenant].join(' · ');
    }

    return rolesTenant.length > 0 ? rolesTenant.join(' · ') : usuario.roles.join(' · ') || 'Sin rol';
  }

  tieneAccesoUsuarios() {
    return this.tieneAlgunPermiso([
      'USUARIO_VER',
      'USUARIO_CREAR',
      'USUARIO_EDITAR',
      'USUARIO_CAMBIAR_ESTADO',
    ]);
  }

  tieneAccesoRoles() {
    return this.esAdministradorSaas();
  }

  tieneAccesoEventos() {
    return (
      this.puedeVerEventos() ||
      this.puedeCrearEventos() ||
      this.puedeEditarEventos() ||
      this.puedeAsignarResponsablesEvento()
    );
  }

  puedeVerEventos() {
    return this.tienePermiso('EVENTO_VER');
  }

  puedeCrearEventos() {
    return this.tienePermiso('EVENTO_CREAR');
  }

  puedeEditarEventos() {
    return this.tienePermiso('EVENTO_EDITAR');
  }

  puedeAsignarResponsablesEvento() {
    return this.tienePermiso('EVENTO_ASIGNAR_RESPONSABLE');
  }

  tieneAccesoRecursos() {
    return this.tieneAlgunPermiso([
      'RECURSO_VER',
      'RECURSO_CREAR',
      'RECURSO_EDITAR',
      'RECURSO_CAMBIAR_ESTADO',
    ]);
  }

  tieneAccesoEspacios() {
    return this.tieneAlgunPermiso([
      'ESPACIO_VER',
      'ESPACIO_CREAR',
      'ESPACIO_EDITAR',
      'PROGRAMACION_VERIFICAR',
    ]);
  }

  tieneAccesoEventosProgramacionORecursos() {
    return this.tieneAccesoEventos() || this.tieneAccesoRecursos() || this.tieneAccesoEspacios();
  }

  tieneAccesoConvocatorias() {
    return this.tienePermiso('CONVOCATORIA_VER');
  }

  puedeCrearPostulacionesStaff() { return this.tienePermiso('POSTULACION_STAFF_CREAR'); }
  puedeGestionarPostulacionesStaff() { return this.tienePermiso('POSTULACION_STAFF_GESTIONAR'); }
  puedeGestionarAsignacionesStaff() { return this.tienePermiso('ASIGNACION_STAFF_GESTIONAR'); }
  puedeVerAsignacionesStaff() { return this.puedeGestionarAsignacionesStaff() || this.puedeCrearPostulacionesStaff(); }
  puedeRegistrarAsistencia() { return this.tienePermiso('ASISTENCIA_REGISTRAR'); }
  puedeConsultarAsistencia() { return this.esAdministradorSaas() || this.tienePermiso('ASISTENCIA_CONSULTAR'); }
  puedeVerBitacora() { return this.esAdministrador() || this.tienePermiso('BITACORA_VER'); }
  puedeGestionarMiembros() { return this.usuario()?.perfilOrganizacionActiva?.administrador ?? false; }
  puedeVerParticipantes() { return this.esAdministrador() || this.tieneAlgunPermiso(['PARTICIPANTE_VER', 'USUARIO_VER', 'INSCRIPCION_VER', 'INSCRIPCION_VER_TODAS']); }
  puedeVerCredencialesPropias() { return this.tienePermiso('INSCRIPCION_VER_PROPIAS'); }
  puedeVerHistorialParticipacion() { return this.usuario()?.perfilOrganizacionActiva?.roles?.includes('PARTICIPANTE') ?? false; }
  puedeGestionarParticipantes() { return this.esAdministrador() || this.tieneAlgunPermiso(['PARTICIPANTE_GESTIONAR', 'USUARIO_CAMBIAR_ESTADO']); }
  tieneAccesoParticipantes() { return this.puedeVerParticipantes(); }

  puedeGestionarConvocatorias() {
    return this.tieneAlgunPermiso([
      'CONVOCATORIA_CREAR',
      'CONVOCATORIA_CERRAR',
      'CONVOCATORIA_CANCELAR',
    ]);
  }

  cerrar() {
    const token = this.sesion?.refreshToken;
    this.limpiarSesion();
    if (token)
      this.auth
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
    localStorage.removeItem(CLAVE_ORGANIZACION);
  }

  private leerOrganizacionActiva(): number | null {
    const valor = localStorage.getItem(CLAVE_ORGANIZACION);
    if (!valor) return null;
    const id = Number(valor);
    return Number.isInteger(id) && id > 0 ? id : null;
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

  private tienePermiso(permiso: string) {
    return this.usuario()?.perfilOrganizacionActiva?.permisos?.includes(permiso) ?? false;
  }

  private tieneAlgunPermiso(permisos: string[]) {
    return permisos.some((permiso) => this.tienePermiso(permiso));
  }
}
