import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import {
  ActualizarRecursoSolicitud,
  AsignacionRecurso,
  CrearAsignacionSolicitud,
  CrearRecursoSolicitud,
  EstadoAsignacion,
  EstadoRecurso,
  Recurso,
  TipoRecurso,
} from '../modelos/recurso.modelo';

@Injectable({ providedIn: 'root' })
export class RecursosService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

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
}
