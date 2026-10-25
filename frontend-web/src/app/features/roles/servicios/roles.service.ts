import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Permiso, Rol, RolPermisos } from '../modelos/rol.modelo';

@Injectable({ providedIn: 'root' })
export class RolesService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

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
}
