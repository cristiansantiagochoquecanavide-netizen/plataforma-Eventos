import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { HistorialParticipacion } from '../modelos/historial-participacion.modelo';

@Injectable({ providedIn: 'root' })
export class HistorialParticipacionService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/inscripciones/mi-historial`;

  listar(): Observable<HistorialParticipacion[]> {
    return this.http.get<HistorialParticipacion[]>(this.url);
  }
}
