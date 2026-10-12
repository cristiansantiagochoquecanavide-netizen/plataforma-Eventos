import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParticipantesService } from '../participantes/servicios/participantes.service';
import { EventosService } from '../eventos/servicios/eventos.service';
import { ConvocatoriasService } from '../convocatorias/servicios/convocatorias.service';
import { Evento } from '../eventos/modelos/evento.modelo';
import { Convocatoria } from '../convocatorias/modelos/convocatoria.modelo';
import {
  ColumnaReporteParticipantes,
  FormatoReporteParticipantes,
  VistaPreviaReporteParticipantes,
} from '../participantes/modelos/participante.modelo';
import { ToastService } from '../../core/toast.service';
import { SesionService } from '../../core/sesion.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import {
  ReportesIaService,
  ReporteMetricasRespuesta,
  InterpretacionIaRespuesta,
  InsightsIaRespuesta,
} from '../reportes-ia/reportes-ia.service';
import { ReconocimientoVozService } from '../reportes-ia/reconocimiento-voz.service';

type PestanaReporte = 'participantes' | 'eventos' | 'convocatorias' | 'ia';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportesComponent implements OnInit {
  readonly pestanaActiva = signal<PestanaReporte>('participantes');

  // Datos base
  readonly eventos = signal<Evento[]>([]);
  readonly convocatorias = signal<Convocatoria[]>([]);
  readonly totalParticipantes = signal(0);
  readonly cargandoResumen = signal(false);

  // Filtros Reporte Participantes
  readonly busqueda = signal('');
  readonly estadoFiltro = signal('TODOS');
  readonly eventoFiltro = signal<number | null>(null);

  readonly columnasDisponibles: { id: ColumnaReporteParticipantes; nombre: string }[] = [
    { id: 'ID', nombre: 'Identificador' },
    { id: 'NOMBRES', nombre: 'Nombres' },
    { id: 'APELLIDOS', nombre: 'Apellidos' },
    { id: 'CORREO', nombre: 'Correo' },
    { id: 'ESTADO', nombre: 'Estado' },
    { id: 'TOTAL_INSCRIPCIONES', nombre: 'Total inscripciones' },
    { id: 'INSCRIPCIONES_CONFIRMADAS', nombre: 'Confirmadas' },
    { id: 'INSCRIPCIONES_CANCELADAS', nombre: 'Canceladas' },
    { id: 'ULTIMA_INSCRIPCION', nombre: 'Última inscripción' },
    { id: 'FECHA_REGISTRO', nombre: 'Fecha registro' },
  ];

  readonly columnasReporte = signal<ColumnaReporteParticipantes[]>([
    'NOMBRES', 'APELLIDOS', 'CORREO', 'ESTADO', 'TOTAL_INSCRIPCIONES', 'FECHA_REGISTRO',
  ]);
  readonly ordenarReporte = signal<ColumnaReporteParticipantes>('APELLIDOS');
  readonly ascendenteReporte = signal(true);
  readonly formatoReporte = signal<FormatoReporteParticipantes>('XLSX');
  readonly correoReporte = signal('');
  readonly vistaPreviaReporte = signal<VistaPreviaReporteParticipantes | null>(null);
  readonly procesandoReporte = signal(false);

  // Filtros Eventos Reporte
  readonly filtroEstadoEvento = signal('TODOS');
  readonly eventosFiltrados = computed(() => {
    const todos = this.eventos();
    const filtro = this.filtroEstadoEvento();
    if (filtro === 'TODOS') return todos;
    return todos.filter(e => e.estado === filtro);
  });

  // Filtros Convocatorias Reporte
  readonly filtroEstadoConvocatoria = signal('TODOS');
  readonly convocatoriasFiltradas = computed(() => {
    const todas = this.convocatorias();
    const filtro = this.filtroEstadoConvocatoria();
    if (filtro === 'TODOS') return todas;
    return todas.filter(c => c.estado === filtro);
  });

  // --- SECCIÓN IA Y VOZ ---
  readonly cargandoMetricasIa = signal(false);
  readonly procesandoIa = signal(false);
  readonly generandoInsights = signal(false);
  consultaTextoIa = '';
  readonly rangoDiasIa = signal<number | null>(null);
  readonly estadoIa = signal('');
  readonly categoriaIa = signal('');
  readonly tipoGraficoIa = signal('BAR_CHART');
  readonly metricasIa = signal<ReporteMetricasRespuesta | null>(null);
  readonly interpretacionIa = signal<InterpretacionIaRespuesta | null>(null);
  readonly insightsIa = signal<InsightsIaRespuesta | null>(null);

  readonly consultasSugeridas = [
    'Mostrar eventos del último mes con alta ocupación',
    'Reporte de inscripciones de la última semana',
    'Rendimiento general de eventos publicados',
    'Eventos finalizados y asistencia total',
  ];

  // Datos computados para Gráficos SVG Profesionales
  readonly datosGraficoBarras = computed(() => {
    const m = this.metricasIa();
    if (!m || !m.detalleEventos || m.detalleEventos.length === 0) return [];
    const maxInscritos = Math.max(...m.detalleEventos.map((e) => e.inscritos), 1);
    return m.detalleEventos.slice(0, 10).map((e) => ({
      ...e,
      alturaPct: Math.max(8, Math.round((e.inscritos / maxInscritos) * 100)),
    }));
  });

  readonly datosDonutCategorias = computed(() => {
    const m = this.metricasIa();
    if (!m || !m.distribucionCategorias || m.distribucionCategorias.length === 0) return [];
    const total = m.distribucionCategorias.reduce((acc, c) => acc + c.cantidadEventos, 0);
    if (total === 0) return [];

    const colores = ['#4f46e5', '#06b6d4', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6'];
    const circunferencia = 2 * Math.PI * 70; // r=70 -> ~439.82
    let offsetAcumulado = 0;

    return m.distribucionCategorias.map((c, i) => {
      const porcentaje = c.cantidadEventos / total;
      const longitudDash = porcentaje * circunferencia;
      const dasharray = `${longitudDash} ${circunferencia}`;
      const dashoffset = -offsetAcumulado;
      offsetAcumulado += longitudDash;
      return {
        categoria: c.categoria,
        cantidad: c.cantidadEventos,
        porcentaje: Math.round(porcentaje * 100),
        color: colores[i % colores.length],
        dasharray,
        dashoffset,
      };
    });
  });

  readonly datosGraficoTendencia = computed(() => {
    const m = this.metricasIa();
    if (!m || !m.inscripcionesPorMes) return { puntos: '', marcas: [] };
    const entradas = Object.entries(m.inscripcionesPorMes);
    if (entradas.length === 0) return { puntos: '', marcas: [] };

    const valores = entradas.map(([_, v]) => v);
    const maxVal = Math.max(...valores, 1);
    const anchoTotal = 500;
    const altoTotal = 160;
    const padding = 30;

    const puntosCalculados = entradas.map(([mes, valor], idx) => {
      const x = padding + (idx / Math.max(entradas.length - 1, 1)) * (anchoTotal - padding * 2);
      const y = altoTotal - padding - (valor / maxVal) * (altoTotal - padding * 2);
      return { x: Math.round(x), y: Math.round(y), mes, valor };
    });

    const strPuntos = puntosCalculados.map((p) => `${p.x},${p.y}`).join(' ');
    return { puntos: strPuntos, marcas: puntosCalculados };
  });

  constructor(
    private readonly participantesService: ParticipantesService,
    private readonly eventosService: EventosService,
    private readonly convocatoriasService: ConvocatoriasService,
    private readonly toast: ToastService,
    public readonly sesion: SesionService,
    private readonly reportesIaService: ReportesIaService,
    public readonly vozService: ReconocimientoVozService
  ) {
    // Sincronizar texto detectado en vivo por voz con el campo de texto
    effect(() => {
      const detectado = this.vozService.textoDetectado();
      if (this.vozService.escuchando() && detectado) {
        this.consultaTextoIa = detectado;
      }
    });

    // Notificar si ocurre un error con el micrófono
    effect(() => {
      const err = this.vozService.error();
      if (err) {
        this.toast.error(err);
      }
    });
  }

  ngOnInit() {
    this.cargarDatosGenerales();
    this.cargarVistaPreviaReporte();
  }

  cargarDatosGenerales() {
    this.cargandoResumen.set(true);

    // Eventos
    this.eventosService.listarEventos().subscribe({
      next: (lista) => this.eventos.set(lista),
      error: () => {},
    });

    // Participantes conteo
    this.participantesService.listar(null, '', '', 0, 1).subscribe({
      next: (res) => this.totalParticipantes.set(res.totalElementos),
      error: () => {},
    });

    // Convocatorias
    this.convocatoriasService.listarConvocatorias().subscribe({
      next: (lista) => this.convocatorias.set(lista),
      error: () => {},
      complete: () => this.cargandoResumen.set(false),
    });
  }

  cambiarPestana(pestana: PestanaReporte) {
    this.pestanaActiva.set(pestana);
    if (pestana === 'ia' && !this.metricasIa()) {
      this.cargarMetricasIa();
    }
  }

  // --- MÉTODOS DE REPORTE CON IA Y VOZ ---
  toggleMicrofono() {
    if (!this.vozService.soportado()) {
      this.toast.error('Tu navegador no soporta reconocimiento de voz nativo (Web Speech API). Usa Google Chrome o Microsoft Edge.');
      return;
    }
    if (this.vozService.escuchando()) {
      this.vozService.detener();
      const detectado = this.vozService.textoDetectado().trim();
      if (detectado) {
        this.consultaTextoIa = detectado;
        this.procesarConsultaIa();
      }
    } else {
      this.toast.info('🎙️ Micrófono activado. Habla con claridad...');
      this.vozService.iniciar((textoFinal) => {
        if (textoFinal) {
          this.consultaTextoIa = textoFinal;
          this.procesarConsultaIa();
        }
      });
    }
  }

  seleccionarSugerenciaIa(sug: string) {
    this.consultaTextoIa = sug;
    this.procesarConsultaIa();
  }

  procesarConsultaIa() {
    const texto = (this.consultaTextoIa || this.vozService.textoDetectado()).trim();
    if (!texto) {
      this.toast.info('Ingresa o dicta una consulta antes de continuar.');
      return;
    }
    this.consultaTextoIa = texto;

    this.procesandoIa.set(true);
    this.insightsIa.set(null);

    this.reportesIaService.interpretarConsulta(texto).subscribe({
      next: (resp) => {
        this.procesandoIa.set(false);
        this.interpretacionIa.set(resp);

        if (resp.parametros) {
          this.rangoDiasIa.set(resp.parametros.rango_dias);
          this.estadoIa.set(resp.parametros.estado_evento || '');
          this.categoriaIa.set(resp.parametros.categoria || '');
          if (resp.parametros.tipo_grafico_recomendado) {
            this.tipoGraficoIa.set(resp.parametros.tipo_grafico_recomendado);
          }
        }
        this.cargarMetricasIa(true);
      },
      error: () => {
        this.procesandoIa.set(false);
        this.cargarMetricasIa(false);
      },
    });
  }

  cargarMetricasIa(solicitarInsightsAuto = false) {
    this.cargandoMetricasIa.set(true);
    this.reportesIaService
      .obtenerMetricas(this.rangoDiasIa(), this.estadoIa(), this.categoriaIa())
      .subscribe({
        next: (data) => {
          this.cargandoMetricasIa.set(false);
          this.metricasIa.set(data);
          if (solicitarInsightsAuto) {
            this.generarInsightsEjecutivos(data);
          }
        },
        error: (err) => {
          this.cargandoMetricasIa.set(false);
          this.toast.error('No se pudieron obtener las métricas de la organización.');
        },
      });
  }

  generarInsightsEjecutivos(data?: ReporteMetricasRespuesta) {
    const datosActuales = data || this.metricasIa();
    if (!datosActuales) return;

    this.generandoInsights.set(true);
    const consulta = this.consultaTextoIa || 'Reporte analítico de organización';

    this.reportesIaService.generarInsights(consulta, datosActuales).subscribe({
      next: (res) => {
        this.generandoInsights.set(false);
        this.insightsIa.set(res);
      },
      error: (err) => {
        this.generandoInsights.set(false);
        console.error('Error al generar insights IA:', err);
      },
    });
  }

  limpiarFiltrosIa() {
    this.consultaTextoIa = '';
    this.rangoDiasIa.set(null);
    this.estadoIa.set('');
    this.categoriaIa.set('');
    this.interpretacionIa.set(null);
    this.insightsIa.set(null);
    this.cargarMetricasIa();
  }

  exportarReporteIaCsv() {
    const m = this.metricasIa();
    if (!m || !m.detalleEventos.length) return;

    const cabeceras = ['ID', 'Evento', 'Categoria', 'Estado', 'Fecha Inicio', 'Aforo', 'Inscritos', 'Ocupacion (%)'];
    const filas = m.detalleEventos.map((e) => [
      e.idEvento,
      `"${(e.nombre || '').replace(/"/g, '""')}"`,
      `"${(e.categoria || '').replace(/"/g, '""')}"`,
      e.estado,
      e.fechaInicio,
      e.aforoMaximo || 0,
      e.inscritos,
      `${e.porcentajeLlenado}%`,
    ]);

    const contenido = [cabeceras.join(','), ...filas.map((f) => f.join(','))].join('\r\n');
    this.descargarCsvBlob(contenido, `reporte-analitico-ia-${new Date().toISOString().slice(0, 10)}.csv`);
    this.toast.exito('Reporte analítico exportado a CSV.');
  }

  // --- MÉTODOS REPORTE PARTICIPANTES ---
  alternarColumnaReporte(columna: ColumnaReporteParticipantes, marcada: boolean) {
    this.columnasReporte.update((actuales) =>
      marcada ? [...new Set([...actuales, columna])] : actuales.filter((a) => a !== columna)
    );
    this.vistaPreviaReporte.set(null);
  }

  seleccionarTodasColumnas() {
    this.columnasReporte.set(this.columnasDisponibles.map((c) => c.id));
    this.vistaPreviaReporte.set(null);
  }

  restablecerColumnas() {
    this.columnasReporte.set([
      'NOMBRES', 'APELLIDOS', 'CORREO', 'ESTADO', 'TOTAL_INSCRIPCIONES', 'FECHA_REGISTRO',
    ]);
    this.vistaPreviaReporte.set(null);
  }

  private solicitudReporte() {
    return {
      eventoId: this.eventoFiltro(),
      busqueda: this.busqueda(),
      estado: this.estadoFiltro() === 'TODOS' ? null : (this.estadoFiltro() as 'ACTIVO' | 'INACTIVO'),
      columnas: this.columnasReporte(),
      ordenarPor: this.ordenarReporte(),
      ascendente: this.ascendenteReporte(),
    };
  }

  cargarVistaPreviaReporte() {
    if (!this.columnasReporte().length) {
      this.toast.error('Selecciona al menos una columna para el reporte.');
      return;
    }
    this.procesandoReporte.set(true);
    this.participantesService.vistaPreviaReporte(this.solicitudReporte()).subscribe({
      next: (preview) => {
        this.vistaPreviaReporte.set(preview);
        this.procesandoReporte.set(false);
      },
      error: (err) => {
        this.toast.error(obtenerMensajeError(err, 'No fue posible generar la vista previa.'));
        this.procesandoReporte.set(false);
      },
    });
  }

  descargarReporte() {
    if (!this.columnasReporte().length) {
      this.toast.error('Selecciona al menos una columna para el reporte.');
      return;
    }
    this.procesandoReporte.set(true);
    const formato = this.formatoReporte();
    this.participantesService.exportarReporte(this.solicitudReporte(), formato).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `reporte-participantes-${new Date().toISOString().slice(0, 10)}.${formato.toLowerCase()}`;
        enlace.click();
        window.URL.revokeObjectURL(url);
        this.toast.exito(`Reporte en formato ${formato} descargado con éxito.`);
        this.procesandoReporte.set(false);
      },
      error: (err) => {
        this.toast.error(obtenerMensajeError(err, 'No fue posible exportar el reporte.'));
        this.procesandoReporte.set(false);
      },
    });
  }

  enviarReporte() {
    const correo = this.correoReporte().trim();
    if (!correo) {
      this.toast.error('Ingresa una dirección de correo válida.');
      return;
    }
    if (!this.columnasReporte().length) {
      this.toast.error('Selecciona al menos una columna para el reporte.');
      return;
    }
    this.procesandoReporte.set(true);
    this.participantesService
      .enviarReporte(correo, this.solicitudReporte(), this.formatoReporte())
      .subscribe({
        next: () => {
          this.toast.exito(`Reporte enviado satisfactoriamente a ${correo}.`);
          this.procesandoReporte.set(false);
        },
        error: (err) => {
          this.toast.error(obtenerMensajeError(err, 'No fue posible enviar el reporte por correo.'));
          this.procesandoReporte.set(false);
        },
      });
  }

  // --- EXPORTACIONES RÁPIDAS CSV PARA EVENTOS Y CONVOCATORIAS ---
  exportarCsvEventos() {
    const lista = this.eventosFiltrados();
    if (lista.length === 0) {
      this.toast.error('No hay eventos para exportar con el filtro seleccionado.');
      return;
    }

    const cabeceras = ['ID', 'Nombre', 'Estado', 'Fecha Inicio', 'Fecha Fin', 'Aforo Maximo', 'Lugar'];
    const filas = lista.map((e) => [
      e.id,
      `"${(e.nombre || '').replace(/"/g, '""')}"`,
      e.estado,
      e.fechaInicio || '',
      e.fechaFin || '',
      e.aforoMaximo ?? 'Ilimitado',
      `"${(e.lugarPrincipal || '').replace(/"/g, '""')}"`,
    ]);

    const contenido = [cabeceras.join(','), ...filas.map((f) => f.join(','))].join('\r\n');
    this.descargarCsvBlob(contenido, `reporte-eventos-${new Date().toISOString().slice(0, 10)}.csv`);
    this.toast.exito('Reporte de eventos descargado.');
  }

  exportarCsvConvocatorias() {
    const lista = this.convocatoriasFiltradas();
    if (lista.length === 0) {
      this.toast.error('No hay convocatorias para exportar con el filtro seleccionado.');
      return;
    }

    const cabeceras = ['ID', 'Evento', 'Tipo Personal', 'Estado', 'Cantidad Requerida', 'Fecha Limite'];
    const filas = lista.map((c) => [
      c.id,
      `"${(c.nombreEvento || '').replace(/"/g, '""')}"`,
      c.tipoPersonal,
      c.estado,
      c.cantidadRequerida,
      c.fechaLimite || '',
    ]);

    const contenido = [cabeceras.join(','), ...filas.map((f) => f.join(','))].join('\r\n');
    this.descargarCsvBlob(contenido, `reporte-convocatorias-${new Date().toISOString().slice(0, 10)}.csv`);
    this.toast.exito('Reporte de convocatorias descargado.');
  }

  private descargarCsvBlob(contenido: string, nombreArchivo: string) {
    const blob = new Blob(['\ufeff' + contenido], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nombreArchivo;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
