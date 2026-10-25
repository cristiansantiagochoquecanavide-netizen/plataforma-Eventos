import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import {
  Evento,
  GuardarEventoSolicitud,
  ResponsableEvento,
} from '../modelos/evento.modelo';

@Injectable({ providedIn: 'root' })
export class EventosService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  listarEventos(termino = '', categoria = '') {
    let params = new HttpParams();
    if (termino.trim()) params = params.set('termino', termino.trim());
    if (categoria) params = params.set('categoria', categoria);
    return this.http.get<Evento[]>(`${this.url}/eventos`, { params });
  }

  obtenerEvento(id: number) {
    return this.http.get<Evento>(`${this.url}/eventos/${id}`);
  }

  crearEvento(datos: GuardarEventoSolicitud) {
    return this.http.post<Evento>(`${this.url}/eventos`, datos);
  }

  actualizarEvento(id: number, datos: GuardarEventoSolicitud) {
    return this.http.put<Evento>(`${this.url}/eventos/${id}`, datos);
  }

  asignarResponsablesEvento(id: number, responsables: number[]) {
    return this.http.put<Evento>(`${this.url}/eventos/${id}/responsables`, { responsables });
  }

  listarResponsablesDisponiblesEvento() {
    return this.http.get<ResponsableEvento[]>(`${this.url}/eventos/responsables-disponibles`);
  }
}
