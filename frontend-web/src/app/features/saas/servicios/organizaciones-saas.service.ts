import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { EstadoOrganizacion, PaginaOrganizacionesSaas } from '../modelos/organizacion-saas.modelo';

@Injectable({ providedIn: 'root' })
export class OrganizacionesSaasService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/saas/organizaciones`;

  listar(filtros: { pagina: number; tamano: number; busqueda: string; estado: EstadoOrganizacion | null }) {
    let params = new HttpParams()
      .set('pagina', filtros.pagina)
      .set('tamano', filtros.tamano);
    if (filtros.busqueda.trim()) params = params.set('busqueda', filtros.busqueda.trim());
    if (filtros.estado) params = params.set('estado', filtros.estado);
    return this.http.get<PaginaOrganizacionesSaas>(this.url, { params });
  }

  actualizarEstado(id: number, estado: EstadoOrganizacion) {
    return this.http.patch<void>(`${this.url}/${id}/estado`, { estado });
  }
}
