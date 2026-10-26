import { Injectable, inject, signal, OnDestroy } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Client } from '@stomp/stompjs';
import { Subject, firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SesionService } from '../../core/sesion.service';

export interface FiltroAforo { eventoId?: number; salaId?: number; categoria?: string; desde?: string; hasta?: string; }
export interface EventoAforo { id: number; nombre: string; salaId: number | null; sala: string | null; }
export interface ResumenAforo {
  eventoId: number; evento: string; salaId: number | null; sala: string | null; capacidad: number | null;
  inscritos: number; presentes: number; ausentes: number; ingresosFranja: number; ingresosSala: number;
  porcentajeAsistencia: number; porcentajeAforo: number | null; alerta: string;
}
export interface FilaAforo { inscripcionId: number; nombre: string; correo: string; evento: string; sala: string | null;
  categoria: string | null; ingreso: string | null; staff: string | null; }
export interface PanelAforo { resumen: ResumenAforo[]; nomina: { filas: FilaAforo[]; total: number; pagina: number; tamano: number }; actualizado: string; }

@Injectable()
export class AforoService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly sesion = inject(SesionService);
  private readonly base = `${environment.apiUrl}/aforo`;
  readonly estado = signal('Desconectado');
  readonly cambios = new Subject<void>();
  private client?: Client;
  private renovacion?: ReturnType<typeof setTimeout>;
  private generacion = 0;
  opciones() { return this.http.get<{ eventos: EventoAforo[]; categorias: string[] }>(`${this.base}/opciones`); }
  private params(f: FiltroAforo) {
    let params = new HttpParams();
    for (const [k, v] of Object.entries(f)) if (v !== undefined && v !== null) params = params.set(k, String(v));
    return params;
  }
  consultar(f: FiltroAforo, pagina: number) { return this.http.get<PanelAforo>(this.base, { params: this.params(f).set('pagina', pagina) }); }
  exportar(f: FiltroAforo, formato: string) { return this.http.get(`${this.base}/exportar`, { params: this.params(f).set('formato', formato), responseType: 'blob' }); }
  clasificar(id: number, categoria: string) { return this.http.patch<void>(`${this.base}/inscripciones/${id}/categoria`, { categoria }); }
  conectar(org: number) {
    this.desconectar();
    const generacion = this.generacion;
    const url = new URL(`${environment.apiUrl}/ws-aforo`, window.location.origin);
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    const client = new Client({ brokerURL: url.toString(), reconnectDelay: 4000, connectionTimeout: 10000,
      heartbeatIncoming: 10000, heartbeatOutgoing: 10000 });
    this.client = client;
    client.beforeConnect = async () => {
      this.estado.set('Conectando');
      try {
        // REST renueva el JWT mediante el interceptor existente si ya expiró.
        await firstValueFrom(this.opciones());
        if (generacion !== this.generacion || this.sesion.organizacionActivaId() !== org) { void client.deactivate(); return; }
        client.connectHeaders = { Authorization: `Bearer ${this.sesion.tokenAcceso()}`, 'X-Organization-Id': String(org) };
      } catch (error) {
        if (generacion !== this.generacion) { void client.deactivate(); return; }
        if (!this.sesion.tokenAcceso() || (error instanceof HttpErrorResponse && [401,403].includes(error.status))) {
          this.estado.set('Sin autorización'); void client.deactivate();
        } else {
          this.estado.set('Sin conexión · reintentando');
          client.connectHeaders = { Authorization: `Bearer ${this.sesion.tokenAcceso()}`, 'X-Organization-Id': String(org) };
        }
      }
    };
    client.onConnect = () => {
      if (generacion !== this.generacion) return;
      const receipt = `aforo-${generacion}`;
      client.watchForReceipt(receipt, () => {
        this.estado.set('En vivo'); this.cambios.next();
      });
      client.subscribe(`/topic/aforo/${org}`, () => this.cambios.next(), { receipt });
      clearTimeout(this.renovacion);
      try {
        const payload = JSON.parse(atob(this.sesion.tokenAcceso()!.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
        const delay = Math.max(1000, payload.exp * 1000 - Date.now() + 250);
        this.renovacion = setTimeout(() => { if (generacion === this.generacion) this.conectar(org); }, delay);
      } catch { /* El servidor sigue validando el JWT en cada envío. */ }
    };
    client.onWebSocketClose = () => { if (generacion === this.generacion) this.estado.set('Desconectado · reconectando'); };
    client.onStompError = () => { if (generacion === this.generacion) this.estado.set('Conexión rechazada'); };
    client.activate();
  }
  desconectar() { this.generacion++; clearTimeout(this.renovacion); if (this.client) void this.client.deactivate(); this.client = undefined; this.estado.set('Desconectado'); }
  ngOnDestroy() { this.desconectar(); this.cambios.complete(); }
}
