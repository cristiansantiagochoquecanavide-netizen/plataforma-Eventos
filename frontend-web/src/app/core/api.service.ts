import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Evento, PaginaUsuarios, Permiso, Rol, RolPermisos, SesionRespuesta, Usuario } from './modelos';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  iniciarSesion(correo: string, clave: string) {
    return this.http.post<SesionRespuesta>(`${this.url}/autenticacion/iniciar-sesion`, {
      correo,
      clave,
    });
  }

  recuperarClave(correo: string) {
    return this.http.post<void>(`${this.url}/autenticacion/solicitar-recuperacion-clave`, { correo });
  }

  restablecerClave(token: string, nuevaClave: string) {
    return this.http.post<void>(`${this.url}/autenticacion/restablecer-clave`, {
      token,
      nuevaClave,
    });
  }

  refrescar(tokenRefresco: string) {
    return this.http.post<SesionRespuesta>(`${this.url}/autenticacion/refrescar`, {
      tokenRefresco,
    });
  }

  cerrarSesion(tokenRefresco: string) {
    return this.http.post<void>(`${this.url}/autenticacion/cerrar-sesion`, { tokenRefresco });
  }

  usuarioActual() {
    return this.http.get<Usuario>(`${this.url}/autenticacion/usuario-actual`);
  }

  obtenerPerfil() {
    return this.http.get<Usuario>(`${this.url}/perfil`);
  }

  actualizarPerfil(datos: Pick<Usuario, 'nombres' | 'apellidos' | 'correo'>) {
    return this.http.put<Usuario>(`${this.url}/perfil`, datos);
  }

  cambiarClave(claveActual: string, nuevaClave: string) {
    return this.http.patch<void>(`${this.url}/perfil/clave`, { claveActual, nuevaClave });
  }

  listarUsuarios(pagina = 0, tamano = 20) {
    return this.http.get<PaginaUsuarios>(`${this.url}/usuarios`, { params: { pagina, tamano } });
  }

  crearUsuario(datos: object) {
    return this.http.post<Usuario>(`${this.url}/usuarios`, datos);
  }

  actualizarUsuario(id: number, datos: object) {
    return this.http.put<Usuario>(`${this.url}/usuarios/${id}`, datos);
  }

  cambiarEstadoUsuario(id: number, estado: string) {
    return this.http.patch<Usuario>(`${this.url}/usuarios/${id}/estado`, { estado });
  }

  listarRoles() {
    return this.http.get<Rol[]>(`${this.url}/roles`);
  }

  crearRol(datos: object) {
    return this.http.post<Rol>(`${this.url}/roles`, datos);
  }

  actualizarRol(id: number, datos: object) {
    return this.http.put<Rol>(`${this.url}/roles/${id}`, datos);
  }

  listarPermisos() {
    return this.http.get<Permiso[]>(`${this.url}/permisos`);
  }

  permisosRol(idRol: number) {
    return this.http.get<RolPermisos>(`${this.url}/roles/${idRol}/permisos`);
  }

  asignarPermisos(idRol: number, permisos: string[]) {
    return this.http.put<RolPermisos>(`${this.url}/roles/${idRol}/permisos`, { permisos });
  }

  listarEventos() {
    return this.http.get<Evento[]>(`${this.url}/eventos`);
  }

  crearEvento(datos: object) {
    return this.http.post<Evento>(`${this.url}/eventos`, datos);
  }

  actualizarEvento(id: number, datos: object) {
    return this.http.put<Evento>(`${this.url}/eventos/${id}`, datos);
  }

  asignarResponsablesEvento(id: number, responsables: number[]) {
    return this.http.put<Evento>(`${this.url}/eventos/${id}/responsables`, { responsables });
  }
}
