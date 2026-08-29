import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import {
  ActualizarRecursoSolicitud,
  AsignacionRecurso,
  Convocatoria,
  CrearAsignacionSolicitud,
  CrearConvocatoriaSolicitud,
  CrearRecursoSolicitud,
  EstadoAsignacion,
  EstadoConvocatoria,
  EstadoRecurso,
  Evento,
  PaginaUsuarios,
  Permiso,
  Recurso,
  Rol,
  RolPermisos,
  SesionRespuesta,
  TipoRecurso,
  TipoPersonalConvocatoria,
  Usuario,
} from './modelos';

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

  actualizarPerfil(datos: object) {
    return this.http.put<Usuario>(`${this.url}/perfil`, datos);
  }

  cambiarClave(claveActual: string, nuevaClave: string) {
    return this.http.patch<void>(`${this.url}/perfil/clave`, { claveActual, nuevaClave });
  }

  listarUsuarios(pagina = 0, tamano = 10, termino = '') {
    const params = {
      pagina: pagina.toString(),
      tamano: tamano.toString(),
      ...(termino.trim() ? { termino: termino.trim() } : {}),
    };
    return this.http.get<PaginaUsuarios>(`${this.url}/usuarios`, { params });
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

  // --- CU19: Gestión y Consulta de Recursos ---
  listarRecursos(termino?: string, tipo?: TipoRecurso | '', estado?: EstadoRecurso | '') {
    let params = new HttpParams();
    if (termino && termino.trim()) {
      params = params.set('termino', termino.trim());
    }
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get<Recurso[]>(`${this.url}/recursos`, { params });
  }

  obtenerRecurso(id: number) {
    return this.http.get<Recurso>(`${this.url}/recursos/${id}`);
  }

  crearRecurso(datos: CrearRecursoSolicitud) {
    return this.http.post<Recurso>(`${this.url}/recursos`, datos);
  }

  actualizarRecurso(id: number, datos: ActualizarRecursoSolicitud) {
    return this.http.put<Recurso>(`${this.url}/recursos/${id}`, datos);
  }

  cambiarEstadoRecurso(id: number, estado: EstadoRecurso) {
    return this.http.patch<Recurso>(`${this.url}/recursos/${id}/estado`, { estado });
  }

  // --- CU20: Asignación de Recursos a Eventos y Salas ---
  listarEventos() {
    return this.http.get<Evento[]>(`${this.url}/eventos`);
  }

  listarAsignaciones(idEvento?: number, idRecurso?: number, estado?: EstadoAsignacion | '') {
    let params = new HttpParams();
    if (idEvento) {
      params = params.set('idEvento', idEvento.toString());
    }
    if (idRecurso) {
      params = params.set('idRecurso', idRecurso.toString());
    }
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get<AsignacionRecurso[]>(`${this.url}/asignaciones-recursos`, { params });
  }

  crearAsignacion(datos: CrearAsignacionSolicitud) {
    return this.http.post<AsignacionRecurso>(`${this.url}/asignaciones-recursos`, datos);
  }

  liberarAsignacion(id: number) {
    return this.http.patch<AsignacionRecurso>(`${this.url}/asignaciones-recursos/${id}/liberar`, {});
  }

  // --- CU15: Gestion de Convocatorias de Staff y Voluntarios ---
  // Consulta con filtros, registra necesidades de personal y ejecuta las dos
  // transiciones finales permitidas por el caso de uso: cerrar y cancelar.
  listarConvocatorias(
    idEvento?: number,
    tipoPersonal?: TipoPersonalConvocatoria | '',
    estado?: EstadoConvocatoria | '',
    termino?: string,
  ) {
    let params = new HttpParams();
    if (idEvento) params = params.set('idEvento', idEvento.toString());
    if (tipoPersonal) params = params.set('tipoPersonal', tipoPersonal);
    if (estado) params = params.set('estado', estado);
    if (termino?.trim()) params = params.set('termino', termino.trim());
    return this.http.get<Convocatoria[]>(`${this.url}/convocatorias`, { params });
  }

  crearConvocatoria(datos: CrearConvocatoriaSolicitud) {
    return this.http.post<Convocatoria>(`${this.url}/convocatorias`, datos);
  }

  cerrarConvocatoria(id: number) {
    return this.http.patch<Convocatoria>(`${this.url}/convocatorias/${id}/cerrar`, {});
  }

  cancelarConvocatoria(id: number) {
    return this.http.patch<Convocatoria>(`${this.url}/convocatorias/${id}/cancelar`, {});
  }
}
