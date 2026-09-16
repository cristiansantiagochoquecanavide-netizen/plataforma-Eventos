import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { AgregarMembresiaSolicitud, Membresia, Organizacion } from '../modelos/organizacion.modelo';

@Injectable({ providedIn: 'root' })
export class OrganizacionesService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  listarMias() {
    return this.http.get<Organizacion[]>(`${this.url}/organizaciones/mias`);
  }

  listarMiembros() {
    return this.http.get<Membresia[]>(`${this.url}/organizaciones/activa/miembros`);
  }

  agregarMembresia(idOrganizacion: number, solicitud: AgregarMembresiaSolicitud) {
    return this.http.post<void>(`${this.url}/organizaciones/${idOrganizacion}/membresias`, solicitud);
  }

  cambiarEstadoMembresia(idOrganizacion: number, idUsuario: number, activo: boolean) {
    return this.http.patch<void>(
      `${this.url}/organizaciones/${idOrganizacion}/membresias/${idUsuario}/estado`,
      {},
      { params: { activo } },
    );
  }
}
