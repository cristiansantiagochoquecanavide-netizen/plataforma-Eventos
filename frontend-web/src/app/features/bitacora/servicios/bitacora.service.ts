import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { FiltrosBitacora, PaginaBitacora } from '../modelos/bitacora.modelo';

@Injectable({ providedIn: 'root' })
export class BitacoraService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;
  listar(filtros: FiltrosBitacora) {
    let params = new HttpParams().set('page', filtros.page).set('size', filtros.size);
    for (const [key, value] of Object.entries(filtros)) if (key !== 'page' && key !== 'size' && value !== '' && value !== undefined) params = params.set(key, String(value));
    return this.http.get<PaginaBitacora>(`${this.url}/admin/bitacora`, { params });
  }
}
