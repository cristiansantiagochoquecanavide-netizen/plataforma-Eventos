import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { GrupoUsuario, GrupoUsuarioFormulario, RolGrupo } from '../modelos/grupo-usuario.modelo';

@Injectable({ providedIn: 'root' })
export class GruposService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  listarGrupos() {
    return this.http.get<GrupoUsuario[]>(`${this.url}/grupos`);
  }

  obtenerGrupo(id: number) {
    return this.http.get<GrupoUsuario>(`${this.url}/grupos/${id}`);
  }

  crearGrupo(datos: GrupoUsuarioFormulario) {
    return this.http.post<GrupoUsuario>(`${this.url}/grupos`, datos);
  }

  actualizarGrupo(id: number, datos: GrupoUsuarioFormulario) {
    return this.http.put<GrupoUsuario>(`${this.url}/grupos/${id}`, datos);
  }

  desactivarGrupo(id: number) {
    return this.http.patch<GrupoUsuario>(`${this.url}/grupos/${id}/desactivar`, {});
  }

  activarGrupo(id: number) {
    return this.http.patch<GrupoUsuario>(`${this.url}/grupos/${id}/activar`, {});
  }

  asignarMiembros(id: number, idsUsuarios: number[]) {
    return this.http.put<GrupoUsuario>(`${this.url}/grupos/${id}/miembros`, { idsUsuarios });
  }

  asignarRoles(id: number, idsRoles: number[]) {
    return this.http.put<GrupoUsuario>(`${this.url}/grupos/${id}/roles`, { idsRoles });
  }

  listarRolesDisponibles() {
    return this.http.get<RolGrupo[]>(`${this.url}/grupos/roles-disponibles`);
  }
}
