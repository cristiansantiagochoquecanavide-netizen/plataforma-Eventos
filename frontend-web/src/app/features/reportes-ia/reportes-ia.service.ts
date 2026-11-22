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

export interface KpiReporte {
  etiqueta: string;
  valor: string;
  detalle?: string;
}

export interface GraficoItem {
  etiqueta: string;
  valor: number;
  extra?: string;
}

export interface ReporteEjecutivoIa {
  titulo: string;
  subtitulo: string;
  tipo_reporte: string;
  periodo: string;
  resumen_ejecutivo: string;
  kpis: KpiReporte[];
  columnas: string[];
  filas: string[][];
  conclusiones: string[];
  recomendaciones: string[];
  grafico_titulo?: string;
  grafico_datos?: GraficoItem[];
}

export interface ExportarReporteDinamicoSolicitud {
  titulo: string;
  subtitulo?: string;
  periodo?: string;
  resumenEjecutivo?: string;
  kpis?: KpiReporte[];
  columnas: string[];
  filas: string[][];
  conclusiones?: string[];
  recomendaciones?: string[];
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

export interface DesgloseEntradaFinanciero {
  categoria: string;
  precioUnitario: number;
  cantidadVendida: number;
  cupoMaximo?: number;
  subtotal: number;
}

export interface RecursoAsignadoFinanciero {
  codigo: string;
  nombre: string;
  tipo: string;
  cantidadAsignada: number;
  costoUnitario: number;
  subtotal: number;
}

export interface EventoFinancieroItem {
  idEvento: number;
  nombre: string;
  estado: string;
  nombreEspacio?: string;
  aforo: number;
  inscripcionesConfirmadas: number;
  asistentesReales?: number;
  ingresos: number;
  costoEspacio: number;
  costoRecursos: number;
  costosTotales: number;
  balanceNeto: number;
  roi: number;
  situacion: 'SUPERAVIT' | 'DEFICIT' | 'EQUILIBRIO';
  desgloseEntradas?: DesgloseEntradaFinanciero[];
  desgloseRecursos?: RecursoAsignadoFinanciero[];
}

export interface ResumenFinancieroRespuesta {
  ingresosTotales: number;
  costosTotales: number;
  balanceNetoGlobal: number;
  roiGlobal: number;
  eventosConGanancia: number;
  eventosConPerdida: number;
  eventosEnEquilibrio: number;
  detalleEventos: EventoFinancieroItem[];
}

@Injectable({
  providedIn: 'root',
})
export class ReportesIaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/reportes/personalizados`;

  obtenerBalanceFinanciero(): Observable<ResumenFinancieroRespuesta> {
    return this.http.get<ResumenFinancieroRespuesta>(`${this.apiUrl}/balance-financiero`);
  }

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

  generarReporteIa(consulta: string): Observable<ReporteEjecutivoIa> {
    return this.http.post<ReporteEjecutivoIa>(`${this.apiUrl}/generar-ia`, {
      consulta,
      contextoFiltros: {},
    });
  }

  exportarDinamico(
    solicitud: ExportarReporteDinamicoSolicitud,
    formato: 'PDF' | 'XLSX'
  ): Observable<Blob> {
    const params = new HttpParams().set('formato', formato);
    return this.http.post(`${this.apiUrl}/exportar-dinamico`, solicitud, {
      params,
      responseType: 'blob',
    });
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
