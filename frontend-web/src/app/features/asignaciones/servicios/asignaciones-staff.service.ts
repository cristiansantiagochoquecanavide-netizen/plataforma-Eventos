import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { AsignacionStaff, CrearAsignacionStaffSolicitud } from '../modelos/asignacion-staff.modelo';

@Injectable({ providedIn: 'root' })
export class AsignacionesStaffService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;
  listar() { return this.http.get<AsignacionStaff[]>(`${this.url}/asignaciones-staff`); }
  listarMias() { return this.http.get<AsignacionStaff[]>(`${this.url}/asignaciones-staff/mias`); }
  crear(solicitud: CrearAsignacionStaffSolicitud) { return this.http.post<AsignacionStaff>(`${this.url}/asignaciones-staff`, solicitud); }
  cancelar(id: number) { return this.http.patch<AsignacionStaff>(`${this.url}/asignaciones-staff/${id}/cancelar`, {}); }
}
