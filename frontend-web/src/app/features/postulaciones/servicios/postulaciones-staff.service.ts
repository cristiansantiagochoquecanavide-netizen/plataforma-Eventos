import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { CrearPostulacionStaffSolicitud, PostulacionStaff } from '../modelos/postulacion-staff.modelo';

@Injectable({ providedIn: 'root' })
export class PostulacionesStaffService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  crear(idConvocatoria: number, solicitud: CrearPostulacionStaffSolicitud) {
    return this.http.post<PostulacionStaff>(`${this.url}/postulaciones-staff/convocatorias/${idConvocatoria}`, solicitud);
  }
  listarMias() { return this.http.get<PostulacionStaff[]>(`${this.url}/postulaciones-staff/mias`); }
  listarPorConvocatoria(id: number) { return this.http.get<PostulacionStaff[]>(`${this.url}/postulaciones-staff/convocatorias/${id}`); }
  aceptar(id: number) { return this.http.patch<PostulacionStaff>(`${this.url}/postulaciones-staff/${id}/aceptar`, {}); }
  rechazar(id: number) { return this.http.patch<PostulacionStaff>(`${this.url}/postulaciones-staff/${id}/rechazar`, {}); }
}
