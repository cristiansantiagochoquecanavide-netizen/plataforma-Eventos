import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import {
  Convocatoria,
  CrearConvocatoriaSolicitud,
  EstadoConvocatoria,
  TipoPersonalConvocatoria,
} from '../modelos/convocatoria.modelo';
import { PostulanteConvocatoria } from '../modelos/postulante-convocatoria.modelo';

@Injectable({ providedIn: 'root' })
export class ConvocatoriasService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  listarConvocatorias(
    idEvento?: number,
    tipoPersonal?: TipoPersonalConvocatoria | '',
    estado?: EstadoConvocatoria | '',
    termino?: string,
  ) {
    let params = new HttpParams();
    if (idEvento) params = params.set('idEvento', idEvento.toString());
    if (tipoPersonal) params = params.set('tipoPersonal', tipoPersonal);
    if (estado) params = params.set('estado', estado);
    if (termino?.trim()) params = params.set('termino', termino.trim());
    return this.http.get<Convocatoria[]>(`${this.url}/convocatorias`, { params });
  }

  crearConvocatoria(datos: CrearConvocatoriaSolicitud) {
    return this.http.post<Convocatoria>(`${this.url}/convocatorias`, datos);
  }

  cerrarConvocatoria(id: number) {
    return this.http.patch<Convocatoria>(`${this.url}/convocatorias/${id}/cerrar`, {});
  }

  cancelarConvocatoria(id: number) {
    return this.http.patch<Convocatoria>(`${this.url}/convocatorias/${id}/cancelar`, {});
  }

  listarPostulantes(idConvocatoria: number) {
    return this.http.get<PostulanteConvocatoria[]>(
      `${this.url}/convocatorias/${idConvocatoria}/postulaciones`,
    );
  }

  seleccionarPostulante(idPostulacion: number) {
    return this.http.patch<PostulanteConvocatoria>(
      `${this.url}/postulaciones/${idPostulacion}/seleccionar`,
      {},
    );
  }

  rechazarPostulante(idPostulacion: number) {
    return this.http.patch<PostulanteConvocatoria>(
      `${this.url}/postulaciones/${idPostulacion}/rechazar`,
      {},
    );
  }
}
