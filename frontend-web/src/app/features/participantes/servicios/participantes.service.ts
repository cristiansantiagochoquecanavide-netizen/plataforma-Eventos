import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ParticipanteInscrito } from '../modelos/participante.modelo';

@Injectable({ providedIn: 'root' })
export class ParticipantesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/inscripciones/participantes`;

  listarParticipantes(idEvento?: number) {
    let params = new HttpParams();
    if (idEvento && idEvento > 0) {
      params = params.set('idEvento', idEvento.toString());
    }
    return this.http.get<ParticipanteInscrito[]>(this.url, { params });
  }
}
