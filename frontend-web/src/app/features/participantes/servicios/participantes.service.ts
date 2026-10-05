import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  PaginaParticipantes,
  ParticipanteDetalle,
  ParticipanteResumen,
} from '../modelos/participante.modelo';

@Injectable({ providedIn: 'root' })
export class ParticipantesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/participantes`;

  listar(
    eventoId?: number | null,
    busqueda = '',
    estado = '',
    pagina = 0,
    tamano = 20
  ): Observable<PaginaParticipantes> {
    let params = new HttpParams()
      .set('pagina', pagina.toString())
      .set('tamano', tamano.toString());

    if (eventoId) {
      params = params.set('eventoId', eventoId.toString());
    }
    if (busqueda.trim()) {
      params = params.set('busqueda', busqueda.trim());
    }
    if (estado && estado !== 'TODOS') {
      params = params.set('estado', estado);
    }

    return this.http.get<PaginaParticipantes>(this.url, { params });
  }

  obtenerDetalle(id: number): Observable<ParticipanteDetalle> {
    return this.http.get<ParticipanteDetalle>(`${this.url}/${id}`);
  }

  cambiarEstado(id: number, estado: 'ACTIVO' | 'INACTIVO'): Observable<ParticipanteResumen> {
    return this.http.patch<ParticipanteResumen>(`${this.url}/${id}/estado`, { estado });
  }

  exportarCsv(
    eventoId?: number | null,
    busqueda = '',
    estado = ''
  ): Observable<Blob> {
    let params = new HttpParams();
    if (eventoId) {
      params = params.set('eventoId', eventoId.toString());
    }
    if (busqueda.trim()) {
      params = params.set('busqueda', busqueda.trim());
    }
    if (estado && estado !== 'TODOS') {
      params = params.set('estado', estado);
    }

    return this.http.get(`${this.url}/exportar`, {
      params,
      responseType: 'blob',
    });
  }
}
