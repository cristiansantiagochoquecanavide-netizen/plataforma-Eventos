import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ReportesIaService, ReporteMetricasRespuesta } from '../reportes-ia/reportes-ia.service';
import { SesionService } from '../../core/sesion.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, DecimalPipe],
  template: `
    <div class="dashboard-contenedor">
      <!-- Encabezado del Dashboard -->
      <div class="dashboard-header">
        <div>
          <h1>Dashboard de Indicadores</h1>
          <p class="subtitulo">Métricas clave, aforo, inscripciones y rendimiento general de la organización en tiempo real.</p>
        </div>
        <div class="acciones-header">
          <select [ngModel]="rangoDiasSeleccionado()" (ngModelChange)="cambiarRango($event)" class="selector-rango">
            <option [ngValue]="null">Todo el período</option>
            <option [ngValue]="7">Últimos 7 días</option>
            <option [ngValue]="30">Últimos 30 días</option>
            <option [ngValue]="90">Último trimestre</option>
            <option [ngValue]="365">Este año</option>
          </select>
          <button class="btn-recargar" (click)="cargarMetricas()">
            <span>🔄</span> Actualizar
          </button>
        </div>
      </div>

      <!-- Estado de carga -->
      <div *ngIf="cargando()" class="estado-cargando">
        <div class="spinner"></div>
        <p>Consolidando indicadores del sistema...</p>
      </div>

      <!-- Alerta de error -->
      <div *ngIf="!cargando() && error()" class="alerta-error">
        {{ error() }}
      </div>

      <!-- Contenido del Dashboard -->
      <div *ngIf="!cargando() && metricas()" class="dashboard-contenido">
        <!-- 4 Tarjetas KPI Clave -->
        <div class="kpi-grid">
          <div class="kpi-card kpi-azul">
            <div class="kpi-icon">📅</div>
            <div class="kpi-info">
              <span class="kpi-label">Total Eventos</span>
              <span class="kpi-valor">{{ metricas()?.totalEventos || 0 }}</span>
              <span class="kpi-badge badge-azul">{{ metricas()?.eventosPublicados || 0 }} activos</span>
            </div>
          </div>

          <div class="kpi-card kpi-verde">
            <div class="kpi-icon">👥</div>
            <div class="kpi-info">
              <span class="kpi-label">Inscripciones</span>
              <span class="kpi-valor">{{ metricas()?.totalInscripciones || 0 }}</span>
              <span class="kpi-badge badge-verde">{{ metricas()?.inscripcionesConfirmadas || 0 }} confirmadas</span>
            </div>
          </div>

          <div class="kpi-card kpi-purpura">
            <div class="kpi-icon">📊</div>
            <div class="kpi-info">
              <span class="kpi-label">Tasa Ocupación</span>
              <span class="kpi-valor">{{ (metricas()?.tasaOcupacionPromedio || 0) | number:'1.0-1' }}%</span>
              <span class="kpi-sublabel">Aforo promedio de salas</span>
            </div>
          </div>

          <div class="kpi-card kpi-ambar">
            <div class="kpi-icon">🏛️</div>
            <div class="kpi-info">
              <span class="kpi-label">Capacidad de Espacios</span>
              <span class="kpi-valor">{{ metricas()?.capacidadTotalEspacios || 0 }}</span>
              <span class="kpi-sublabel">Asientos totales habilitados</span>
            </div>
          </div>
        </div>

        <!-- Fila de Gráficos e Indicadores -->
        <div class="graficos-grid">
          <!-- Ocupación de Eventos (Barras) -->
          <div class="panel-grafico">
            <div class="panel-cabecera">
              <h3>🏆 Eventos con Mayor Ocupación</h3>
              <a routerLink="/reportes" class="enlace-detalle">Ver reportes detallados →</a>
            </div>
            <div *ngIf="metricas()?.detalleEventos?.length === 0" class="grafico-vacio">
              No hay eventos con inscripciones registradas en este período.
            </div>
            <div *ngIf="(metricas()?.detalleEventos?.length || 0) > 0" class="lista-barras">
              <div *ngFor="let ev of metricas()?.detalleEventos?.slice(0, 5)" class="item-barra">
                <div class="info-evento-barra">
                  <span class="nombre-ev">{{ ev.nombre }}</span>
                  <span class="porcentaje-ev">{{ ev.porcentajeLlenado }}% ocupado ({{ ev.inscritos }}/{{ ev.aforoMaximo || '∞' }})</span>
                </div>
                <div class="barra-fondo">
                  <div class="barra-progreso" [style.width.%]="ev.porcentajeLlenado"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Donut Categorías -->
          <div class="panel-grafico">
            <div class="panel-cabecera">
              <h3>🏷️ Distribución por Categoría</h3>
            </div>
            <div *ngIf="datosDonut().length === 0" class="grafico-vacio">
              Sin datos de categorías en los eventos activos.
            </div>
            <div *ngIf="datosDonut().length > 0" class="contenedor-donut">
              <svg viewBox="0 0 200 200" class="donut-svg">
                <circle cx="100" cy="100" r="70" class="donut-bg" />
                <circle 
                  *ngFor="let item of datosDonut()" 
                  cx="100" cy="100" r="70" 
                  class="donut-segmento"
                  [attr.stroke]="item.color"
                  [attr.stroke-dasharray]="item.dasharray"
                  [attr.stroke-dashoffset]="item.dashoffset"
                />
                <text x="100" y="95" text-anchor="middle" class="donut-centro-total">{{ metricas()?.totalEventos }}</text>
                <text x="100" y="115" text-anchor="middle" class="donut-centro-sub">Eventos</text>
              </svg>
              <div class="leyenda-donut">
                <div *ngFor="let item of datosDonut()" class="item-leyenda">
                  <span class="punto-color" [style.background-color]="item.color"></span>
                  <span class="label-cat">{{ item.categoria }}:</span>
                  <strong class="pct-cat">{{ item.cantidad }} ({{ item.porcentaje }}%)</strong>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Accesos Directos Operativos -->
        <div class="accesos-directos-seccion">
          <h3>Módulos de Gestión Rápida</h3>
          <div class="accesos-grid">
            <a routerLink="/consulta-eventos" class="tarjeta-acceso">
              <span class="acceso-icon">📅</span>
              <div>
                <h4>Gestión de Eventos</h4>
                <p>Consultar, publicar y configurar aforo de salas.</p>
              </div>
            </a>
            <a routerLink="/reportes" class="tarjeta-acceso">
              <span class="acceso-icon">📈</span>
              <div>
                <h4>Reportes Analíticos & IA</h4>
                <p>Consultas por voz, insights ejecutivos y exportación.</p>
              </div>
            </a>
            <a routerLink="/mis-certificados" class="tarjeta-acceso">
              <span class="acceso-icon">🎓</span>
              <div>
                <h4>Certificados Digitales</h4>
                <p>Verificación pública y descarga oficial con QR.</p>
              </div>
            </a>
            <a routerLink="/bitacora" class="tarjeta-acceso">
              <span class="acceso-icon">🛡️</span>
              <div>
                <h4>Auditoría y Bitácora</h4>
                <p>Trazas de seguridad cifradas con KMS Envelope.</p>
              </div>
            </a>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-contenedor {
      max-width: 1200px;
      margin: 0 auto;
      padding: 1.5rem;
      font-family: system-ui, -apple-system, sans-serif;
    }
    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
      border-bottom: 1px solid #e2e8f0;
      padding-bottom: 1.25rem;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .dashboard-header h1 {
      font-size: 1.75rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 0.25rem 0;
    }
    .subtitulo {
      color: #64748b;
      font-size: 0.95rem;
      margin: 0;
    }
    .acciones-header {
      display: flex;
      gap: 0.75rem;
      align-items: center;
    }
    .selector-rango {
      padding: 0.6rem 1rem;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
      font-size: 0.9rem;
      background: white;
      color: #334155;
      cursor: pointer;
    }
    .btn-recargar {
      background: #f8fafc;
      border: 1px solid #cbd5e1;
      padding: 0.6rem 1rem;
      border-radius: 8px;
      font-size: 0.9rem;
      cursor: pointer;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 0.35rem;
    }
    .btn-recargar:hover {
      background: #f1f5f9;
    }
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1.25rem;
      margin-bottom: 2rem;
    }
    .kpi-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      border: 1px solid #e2e8f0;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
      display: flex;
      align-items: center;
      gap: 1.25rem;
    }
    .kpi-azul { border-left: 5px solid #2563eb; }
    .kpi-verde { border-left: 5px solid #16a34a; }
    .kpi-purpura { border-left: 5px solid #8b5cf6; }
    .kpi-ambar { border-left: 5px solid #f59e0b; }
    .kpi-icon {
      font-size: 2.2rem;
      background: #f8fafc;
      width: 56px;
      height: 56px;
      display: flex;
      justify-content: center;
      align-items: center;
      border-radius: 12px;
    }
    .kpi-info {
      display: flex;
      flex-direction: column;
    }
    .kpi-label {
      font-size: 0.8rem;
      font-weight: 600;
      text-transform: uppercase;
      color: #64748b;
      margin-bottom: 0.25rem;
    }
    .kpi-valor {
      font-size: 1.85rem;
      font-weight: 800;
      color: #0f172a;
      line-height: 1.1;
      margin-bottom: 0.35rem;
    }
    .kpi-badge {
      font-size: 0.75rem;
      font-weight: 600;
      padding: 0.2rem 0.5rem;
      border-radius: 9999px;
      align-self: flex-start;
    }
    .badge-azul { background: #eff6ff; color: #1d4ed8; }
    .badge-verde { background: #f0fdf4; color: #15803d; }
    .kpi-sublabel {
      font-size: 0.75rem;
      color: #94a3b8;
    }
    .graficos-grid {
      display: grid;
      grid-template-columns: 3fr 2fr;
      gap: 1.5rem;
      margin-bottom: 2.5rem;
    }
    @media (max-width: 900px) {
      .graficos-grid { grid-template-columns: 1fr; }
    }
    .panel-grafico {
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 1.5rem;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
    }
    .panel-cabecera {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.25rem;
      border-bottom: 1px solid #f1f5f9;
      padding-bottom: 0.75rem;
    }
    .panel-cabecera h3 {
      font-size: 1.1rem;
      color: #1e293b;
      margin: 0;
      font-weight: 700;
    }
    .enlace-detalle {
      color: #2563eb;
      font-size: 0.85rem;
      text-decoration: none;
      font-weight: 500;
    }
    .enlace-detalle:hover {
      text-decoration: underline;
    }
    .lista-barras {
      display: flex;
      flex-direction: column;
      gap: 1.15rem;
    }
    .item-barra {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }
    .info-evento-barra {
      display: flex;
      justify-content: space-between;
      font-size: 0.85rem;
    }
    .nombre-ev {
      font-weight: 600;
      color: #334155;
    }
    .porcentaje-ev {
      color: #64748b;
    }
    .barra-fondo {
      background: #f1f5f9;
      height: 10px;
      border-radius: 5px;
      overflow: hidden;
    }
    .barra-progreso {
      background: linear-gradient(90deg, #3b82f6 0%, #1d4ed8 100%);
      height: 100%;
      border-radius: 5px;
      transition: width 0.6s ease;
    }
    .contenedor-donut {
      display: flex;
      align-items: center;
      gap: 1.5rem;
      justify-content: center;
      flex-wrap: wrap;
    }
    .donut-svg {
      width: 170px;
      height: 170px;
      transform: rotate(-90deg);
    }
    .donut-bg {
      fill: none;
      stroke: #f1f5f9;
      stroke-width: 24;
    }
    .donut-segmento {
      fill: none;
      stroke-width: 24;
      transition: stroke-dasharray 0.5s ease;
    }
    .donut-centro-total {
      transform: rotate(90deg);
      transform-origin: center;
      font-size: 24px;
      font-weight: 800;
      fill: #0f172a;
    }
    .donut-centro-sub {
      transform: rotate(90deg);
      transform-origin: center;
      font-size: 11px;
      fill: #64748b;
    }
    .leyenda-donut {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      font-size: 0.85rem;
    }
    .item-leyenda {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .punto-color {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      display: inline-block;
    }
    .grafico-vacio {
      padding: 2.5rem 1rem;
      text-align: center;
      color: #94a3b8;
      font-size: 0.9rem;
    }
    .accesos-directos-seccion h3 {
      font-size: 1.15rem;
      color: #1e293b;
      margin-bottom: 1rem;
      font-weight: 700;
    }
    .accesos-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1rem;
    }
    .tarjeta-acceso {
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 10px;
      padding: 1.25rem;
      display: flex;
      gap: 1rem;
      align-items: center;
      text-decoration: none;
      transition: all 0.2s;
    }
    .tarjeta-acceso:hover {
      border-color: #3b82f6;
      box-shadow: 0 6px 12px -2px rgba(59, 130, 246, 0.1);
      transform: translateY(-2px);
    }
    .acceso-icon {
      font-size: 2rem;
    }
    .tarjeta-acceso h4 {
      margin: 0 0 0.25rem 0;
      font-size: 0.95rem;
      color: #0f172a;
    }
    .tarjeta-acceso p {
      margin: 0;
      font-size: 0.8rem;
      color: #64748b;
    }
    .estado-cargando {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 3rem;
      color: #64748b;
    }
    .spinner {
      border: 3px solid #f3f3f3;
      border-top: 3px solid #2563eb;
      border-radius: 50%;
      width: 28px;
      height: 28px;
      animation: spin 1s linear infinite;
      margin-bottom: 0.75rem;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    .alerta-error {
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #b91c1c;
      padding: 1rem;
      border-radius: 8px;
      margin-bottom: 1.5rem;
    }
  `]
})
export class DashboardComponent implements OnInit {
  private readonly reportesIa = inject(ReportesIaService);
  readonly sesion = inject(SesionService);

  readonly metricas = signal<ReporteMetricasRespuesta | null>(null);
  readonly cargando = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly rangoDiasSeleccionado = signal<number | null>(null);

  readonly datosDonut = computed(() => {
    const m = this.metricas();
    if (!m || !m.distribucionCategorias || m.distribucionCategorias.length === 0) return [];
    const total = m.distribucionCategorias.reduce((acc, c) => acc + c.cantidadEventos, 0);
    if (total === 0) return [];

    const colores = ['#2563eb', '#06b6d4', '#16a34a', '#f59e0b', '#8b5cf6', '#ec4899'];
    const circunferencia = 2 * Math.PI * 70;
    let offsetAcumulado = 0;

    return m.distribucionCategorias.map((c, i) => {
      const pct = c.cantidadEventos / total;
      const longitudDash = pct * circunferencia;
      const dasharray = `${longitudDash} ${circunferencia}`;
      const dashoffset = -offsetAcumulado;
      offsetAcumulado += longitudDash;
      return {
        categoria: c.categoria,
        cantidad: c.cantidadEventos,
        porcentaje: Math.round(pct * 100),
        color: colores[i % colores.length],
        dasharray,
        dashoffset,
      };
    });
  });

  ngOnInit(): void {
    this.cargarMetricas();
  }

  cambiarRango(nuevoRango: number | null): void {
    this.rangoDiasSeleccionado.set(nuevoRango);
    this.cargarMetricas();
  }

  cargarMetricas(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.reportesIa.obtenerMetricas(this.rangoDiasSeleccionado()).subscribe({
      next: (data) => {
        this.metricas.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No fue posible consolidar los indicadores. Intenta nuevamente.');
        this.cargando.set(false);
      }
    });
  }
}
