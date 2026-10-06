import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export type CodigoPlanSaas = 'BASICO' | 'PROFESIONAL' | 'EMPRESARIAL';

export interface PlanSaas {
  codigo: CodigoPlanSaas;
  nombre: string;
  precioMensualBob: number;
  gratuito: boolean;
  usuariosInternos: number;
  eventosActivos: number;
  inscripcionesMes: number;
  reportes: string[];
}

export interface CrearAltaSaas {
  nombreEmpresa: string;
  slugSugerido: string;
  nombresContacto: string;
  apellidosContacto: string;
  correoContacto: string;
  plan: CodigoPlanSaas;
}

export interface AltaSaasIniciada {
  folio: string;
  mensaje: string;
}

export interface VerificarAltaSaas {
  checkoutRequerido: boolean;
  checkoutUrl: string | null;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class AltaSaasService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/saas`;

  planes() {
    return this.http.get<PlanSaas[]>(`${this.url}/planes`);
  }

  iniciar(solicitud: CrearAltaSaas) {
    return this.http.post<AltaSaasIniciada>(`${this.url}/altas`, solicitud);
  }

  verificar(token: string) {
    return this.http.post<VerificarAltaSaas>(`${this.url}/altas/verificar-email`, { token });
  }

  activar(token: string, nuevaClave: string) {
    return this.http.post<void>(`${this.url}/altas/activar`, { token, nuevaClave });
  }
}
