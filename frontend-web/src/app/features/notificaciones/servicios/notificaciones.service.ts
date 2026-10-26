import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import {
  NotificacionEvento,
  RecordatorioMasivoRespuesta,
  RecordatorioMasivoSolicitud,
} from '../modelos/notificacion-evento.modelo';

@Injectable({ providedIn: 'root' })
export class NotificacionesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/notificaciones/eventos`;

  listarPorEvento(idEvento: number) {
    return this.http.get<NotificacionEvento[]>(`${this.url}/${idEvento}`);
  }

  programarRecordatorio(idEvento: number, solicitud: RecordatorioMasivoSolicitud) {
    return this.http.post<RecordatorioMasivoRespuesta>(
      `${this.url}/${idEvento}/recordatorios`,
      solicitud,
    );
  }

  reintentar(idEvento: number, idNotificacion: number) {
    return this.http.post<NotificacionEvento>(
      `${this.url}/${idEvento}/notificaciones/${idNotificacion}/reintentar`,
      {},
    );
  }
}
