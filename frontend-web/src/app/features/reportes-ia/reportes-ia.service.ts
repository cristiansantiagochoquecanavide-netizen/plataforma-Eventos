import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EventoMetricaItem {
  idEvento: number;
  nombre: string;
  categoria: string;
  estado: string;
  fechaInicio: string;
  aforoMaximo: number;
  inscritos: number;
  porcentajeLlenado: number;
}

export interface CategoriaMetricaItem {
  categoria: string;
  cantidadEventos: number;
}

export interface ReporteMetricasRespuesta {
  totalEventos: number;
  eventosPublicados: number;
  eventosFinalizados: number;
  totalInscripciones: number;
  inscripcionesConfirmadas: number;
  capacidadTotalEspacios: number;
  tasaOcupacionPromedio: number;
  detalleEventos: EventoMetricaItem[];
  distribucionCategorias: CategoriaMetricaItem[];
  inscripcionesPorMes: { [key: string]: number };
}

export interface InterpretacionIaRespuesta {
  intencion: string;
  parametros: {
    rango_dias: number | null;
    estado_evento: string | null;
    categoria: string | null;
    tipo_metrica: string;
    tipo_grafico_recomendado: string;
  };
  mensaje_asistente: string;
}

export interface InsightsIaRespuesta {
  resumen_ejecutivo: string;
  puntos_clave: string[];
  recomendaciones: string[];
}

@Injectable({
  providedIn: 'root',
})
export class ReportesIaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/reportes/personalizados`;

  obtenerMetricas(
    rangoDias?: number | null,
    estado?: string | null,
    categoria?: string | null
  ): Observable<ReporteMetricasRespuesta> {
    let params = new HttpParams();
    if (rangoDias !== undefined && rangoDias !== null) {
      params = params.set('rangoDias', rangoDias.toString());
    }
    if (estado) {
      params = params.set('estado', estado);
    }
    if (categoria) {
      params = params.set('categoria', categoria);
    }
    return this.http.get<ReporteMetricasRespuesta>(`${this.apiUrl}/metricas`, { params });
  }

  interpretarConsulta(consulta: string, contextoFiltros?: any): Observable<InterpretacionIaRespuesta> {
    return this.http.post<InterpretacionIaRespuesta>(`${this.apiUrl}/interpretar-ia`, {
      consulta,
      contextoFiltros: contextoFiltros || {},
    });
  }

  generarInsights(consulta: string, metricas: ReporteMetricasRespuesta): Observable<InsightsIaRespuesta> {
    const params = new HttpParams().set('consulta', consulta);
    return this.http.post<InsightsIaRespuesta>(`${this.apiUrl}/insights-ia`, metricas, { params });
  }
}
