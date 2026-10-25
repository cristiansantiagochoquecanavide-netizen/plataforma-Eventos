import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Asistencia, InscripcionElegible } from '../modelos/asistencia.modelo';

@Injectable({ providedIn: 'root' })
export class AsistenciasService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  listarInscripciones() {
    return this.http.get<InscripcionElegible[]>(`${this.url}/asistencias/inscripciones-elegibles`);
  }

  registrar(idInscripcion: number) {
    return this.http.post<Asistencia>(`${this.url}/asistencias/check-in`, { idInscripcion });
  }
}
