import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Espacio, GuardarEspacioSolicitud } from '../modelos/espacio.modelo';

@Injectable({ providedIn: 'root' })
export class EspaciosService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  listarEspacios() {
    return this.http.get<Espacio[]>(`${this.url}/espacios`);
  }

  crearEspacio(datos: GuardarEspacioSolicitud) {
    return this.http.post<Espacio>(`${this.url}/espacios`, datos);
  }

  actualizarEspacio(id: number, datos: GuardarEspacioSolicitud) {
    return this.http.put<Espacio>(`${this.url}/espacios/${id}`, datos);
  }
}
