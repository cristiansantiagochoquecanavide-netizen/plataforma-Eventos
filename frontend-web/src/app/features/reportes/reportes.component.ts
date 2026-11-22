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
import { EspaciosService } from '../espacios/servicios/espacios.service';
import { RecursosService } from '../recursos/servicios/recursos.service';
import { Evento } from '../eventos/modelos/evento.modelo';
import { Convocatoria } from '../convocatorias/modelos/convocatoria.modelo';
import { Espacio } from '../espacios/modelos/espacio.modelo';
import { Recurso } from '../recursos/modelos/recurso.modelo';
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
  ReporteEjecutivoIa,
  ExportarReporteDinamicoSolicitud,
  KpiReporte,
  ResumenFinancieroRespuesta,
  EventoFinancieroItem,
} from '../reportes-ia/reportes-ia.service';
import { ReconocimientoVozService } from '../reportes-ia/reconocimiento-voz.service';

type ModoVista = 'ia' | 'analitico' | 'catalogo';
type TipoCatalogo = 'finanzas' | 'eventos' | 'participantes' | 'convocatorias' | 'espacios' | 'recursos';
type PerfilColumnas = 'basico' | 'inscripciones' | 'completo';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportesComponent implements OnInit {
  // Navegación principal simplificada: Voz/IA o Catálogo
  readonly modoActivo = signal<ModoVista>('ia');
  readonly catalogoActivo = signal<TipoCatalogo>('eventos');

  // Datos base de la organización
  readonly eventos = signal<Evento[]>([]);
  readonly convocatorias = signal<Convocatoria[]>([]);
  readonly espacios = signal<Espacio[]>([]);
  readonly recursos = signal<Recurso[]>([]);
  readonly totalParticipantes = signal(0);
  readonly cargandoResumen = signal(false);

  // --- GENERADOR POR VOZ / IA ---
  consultaTextoIa = '';
  readonly generandoReporteIa = signal(false);
  readonly reporteIa = signal<ReporteEjecutivoIa | null>(null);
  readonly exportando = signal<'PDF' | 'XLSX' | null>(null);

  readonly consultasSugeridas = [
    'Balance financiero: ¿Cuánto ganamos y qué evento dejó pérdidas?',
    'Balance ejecutivo general de la organización',
    'Rentabilidad y venta de entradas por evento',
    'Reporte de eventos publicados y porcentaje de ocupación',
    'Reporte de participantes activos e inscripciones confirmadas',
    'Inventario de recursos y disponibilidad actual',
  ];

  // --- CATÁLOGO Y DASHBOARD ANALÍTICO: FINANZAS Y RADIOGRAFÍA 360° POR CONCIERTO ---
  readonly resumenFinanciero = signal<ResumenFinancieroRespuesta | null>(null);
  readonly cargandoFinanzas = signal(false);
  readonly eventoDesgloseAbierto = signal<number | null>(null);
  readonly eventoAnaliticoId = signal<number | null>(null);
  readonly submodoAnalitico = signal<'concierto' | 'comparador'>('concierto');

  cambiarSubmodoAnalitico(submodo: 'concierto' | 'comparador') {
    this.submodoAnalitico.set(submodo);
  }

  toggleDesgloseEvento(idEvento: number) {
    this.eventoDesgloseAbierto.set(this.eventoDesgloseAbierto() === idEvento ? null : idEvento);
  }

  seleccionarEventoAnalitico(valor: string | number | null) {
    if (valor === null || valor === '' || valor === 'NINGUNO') {
      this.eventoAnaliticoId.set(null);
      return;
    }
    const id = Number(valor);
    this.eventoAnaliticoId.set(Number.isFinite(id) && id > 0 ? id : null);
    if (this.eventoAnaliticoId()) {
      this.submodoAnalitico.set('concierto');
    }
  }

  private claseColorCategoria(nombre: string): string {
    const n = (nombre || '').toLowerCase();
    if (n.includes('super')) return 'tier-svip';
    if (n.includes('vip')) return 'tier-vip';
    if (n.includes('general')) return 'tier-general';
    return 'tier-otro';
  }

  private colorHexCategoria(nombre: string): string {
    const n = (nombre || '').toLowerCase();
    if (n.includes('super')) return '#f59e0b';
    if (n.includes('vip')) return '#6366f1';
    if (n.includes('general')) return '#10b981';
    return '#06b6d4';
  }

  formatearCompacto(valor: number, conSigno = false): string {
    const num = Number(valor) || 0;
    const abs = Math.abs(num);
    const signo = num < 0 ? '-' : conSigno && num > 0 ? '+' : '';
    if (abs >= 1_000_000) {
      return `${signo}${(abs / 1_000_000).toFixed(2)}M`;
    }
    if (abs >= 10_000) {
      return `${signo}${(abs / 1_000).toFixed(1)}k`;
    }
    return `${signo}${abs.toLocaleString('es-ES', { maximumFractionDigits: 0 })}`;
  }

  readonly radiografiaEvento = computed(() => {
    const idBuscado = this.eventoAnaliticoId();
    if (!idBuscado) return null;
    const ev = (this.resumenFinanciero()?.detalleEventos ?? []).find((e) => e.idEvento === idBuscado);
    if (!ev) return null;

    const vendidos = ev.inscripcionesConfirmadas || 0;
    const aforo = ev.aforo || 0;
    const ingresos = ev.ingresos || 0;
    const costosTotales = ev.costosTotales || 0;
    const balanceNeto = ev.balanceNeto || 0;

    // 1. Termómetro Financiero y Punto de Equilibrio (Break-Even)
    const ticketPromedio = vendidos > 0 ? ingresos / vendidos : 0;
    const boletosEquilibrio = ticketPromedio > 0 ? Math.ceil(costosTotales / ticketPromedio) : 0;
    const diferenciaBoletosEquilibrio = vendidos - boletosEquilibrio;
    const coberturaCostosPct = costosTotales > 0 ? Math.round((ingresos / costosTotales) * 1000) / 10 : 100;
    const maxFin = Math.max(ingresos, costosTotales, Math.abs(balanceNeto), 1);
    const anchoIngresosPct = Math.max(4, Math.round((ingresos / maxFin) * 100));
    const anchoCostosPct = Math.max(4, Math.round((costosTotales / maxFin) * 100));
    const anchoBalancePct = Math.max(4, Math.round((Math.abs(balanceNeto) / maxFin) * 100));

    // Cascada Financiera (Waterfall SVG): se adapta a Superávit y Déficit dentro del área [30..165]
    let wfZeroY = 205;
    let wfIngresosH = 0;
    let wfIngresosY = 205;
    let wfEspacioH = 0;
    let wfEspacioY = 205;
    let wfRecursosH = 0;
    let wfRecursosY = 205;
    let wfNetoH = 0;
    let wfNetoY = 205;

    if (balanceNeto >= 0) {
      wfZeroY = 205;
      const wfScale = 165 / Math.max(ingresos, costosTotales, 1);
      wfIngresosH = ingresos > 0 ? Math.max(14, Math.round(ingresos * wfScale)) : 3;
      wfIngresosY = wfZeroY - wfIngresosH;
      wfEspacioH = ev.costoEspacio > 0 ? Math.max(8, Math.round(ev.costoEspacio * wfScale)) : 0;
      wfEspacioY = wfIngresosY;
      wfRecursosH = ev.costoRecursos > 0 ? Math.max(8, Math.round(ev.costoRecursos * wfScale)) : 0;
      wfRecursosY = wfEspacioY + wfEspacioH;
      wfNetoH = balanceNeto > 0 ? Math.max(14, Math.round(balanceNeto * wfScale)) : 3;
      wfNetoY = wfZeroY - wfNetoH;
    } else {
      const totalSpan = Math.max(ingresos, 0) + Math.abs(balanceNeto);
      const wfScale = 155 / Math.max(totalSpan, 1);
      wfIngresosH = ingresos > 0 ? Math.max(10, Math.round(ingresos * wfScale)) : 0;
      wfZeroY = 36 + wfIngresosH;
      wfIngresosY = 36;
      wfEspacioH = ev.costoEspacio > 0 ? Math.max(8, Math.round(ev.costoEspacio * wfScale)) : 0;
      wfEspacioY = 36;
      wfRecursosH = ev.costoRecursos > 0 ? Math.max(8, Math.round(ev.costoRecursos * wfScale)) : 0;
      wfRecursosY = 36 + wfEspacioH;
      wfNetoH = Math.max(14, Math.round(Math.abs(balanceNeto) * wfScale));
      wfNetoY = wfZeroY;
    }

    const cascadaFinanciera = {
      wfZeroY,
      wfIngresosH,
      wfIngresosY,
      wfEspacioH,
      wfEspacioY,
      wfRecursosH,
      wfRecursosY,
      wfNetoH,
      wfNetoY,
      ingresosTxt: this.formatearCompacto(ingresos, true),
      espacioTxt: ev.costoEspacio > 0 ? `-${this.formatearCompacto(ev.costoEspacio)}` : '0',
      recursosTxt: ev.costoRecursos > 0 ? `-${this.formatearCompacto(ev.costoRecursos)}` : '0',
      netoTxt: this.formatearCompacto(balanceNeto, true),
      netoLabelY: balanceNeto >= 0 ? wfNetoY - 9 : wfNetoY + wfNetoH + 16,
    };

    // 2. Boletería por Categoría (General, VIP, Super VIP)
    const rawTiers = ev.desgloseEntradas ?? [];
    const maxCupoGlobalTier = Math.max(
      ...rawTiers.map((t) => Math.max(t.cupoMaximo || 0, t.cantidadVendida || 0)),
      1
    );

    const tiers = rawTiers.map((t) => {
      const cupo = t.cupoMaximo && t.cupoMaximo > 0 ? t.cupoMaximo : t.cantidadVendida;
      const pctCupoAgotado = cupo > 0 ? Math.min(100, Math.round((t.cantidadVendida / cupo) * 1000) / 10) : 0;
      const alturaCupoPct = Math.max(10, Math.round((cupo / maxCupoGlobalTier) * 100));
      const alturaVendidaPct = Math.max(8, Math.round((t.cantidadVendida / maxCupoGlobalTier) * 100));

      return {
        categoria: t.categoria,
        colorClase: this.claseColorCategoria(t.categoria),
        colorHex: this.colorHexCategoria(t.categoria),
        precioUnitario: t.precioUnitario,
        cantidadVendida: t.cantidadVendida,
        cupoMaximo: cupo,
        cuposDisponibles: Math.max(0, cupo - t.cantidadVendida),
        subtotal: t.subtotal,
        pctCupoAgotado,
        alturaCupoPct,
        alturaVendidaPct,
        pctBoletosEvento: vendidos > 0 ? Math.round((t.cantidadVendida / vendidos) * 1000) / 10 : 0,
        pctIngresosEvento: ingresos > 0 ? Math.round((t.subtotal / ingresos) * 1000) / 10 : 0,
      };
    });

    // 3. Radiografía de Costos y Recursos Técnicos Usados
    const nombreEspacio = ev.nombreEspacio || 'Recinto principal';
    const pctEspacio = costosTotales > 0 ? Math.round((ev.costoEspacio / costosTotales) * 1000) / 10 : 0;
    const pctRecursos = costosTotales > 0 ? Math.round((ev.costoRecursos / costosTotales) * 1000) / 10 : 0;

    const itemsCosto: {
      tipoItem: 'ESPACIO' | 'RECURSO';
      codigo: string;
      nombre: string;
      detalleCantidad: string;
      subtotal: number;
      pctPresupuesto: number;
    }[] = [];

    if (ev.costoEspacio > 0) {
      itemsCosto.push({
        tipoItem: 'ESPACIO',
        codigo: 'RECINTO',
        nombre: `Alquiler de Espacio: ${nombreEspacio}`,
        detalleCantidad: `Aforo autoriz.: ${aforo.toLocaleString('es-ES')} pers.`,
        subtotal: ev.costoEspacio,
        pctPresupuesto: pctEspacio,
      });
    }

    for (const r of ev.desgloseRecursos ?? []) {
      itemsCosto.push({
        tipoItem: 'RECURSO',
        codigo: r.codigo,
        nombre: r.nombre,
        detalleCantidad: `${r.cantidadAsignada} u. × ${r.costoUnitario.toLocaleString('es-ES', { minimumFractionDigits: 2 })} Bs`,
        subtotal: r.subtotal,
        pctPresupuesto: costosTotales > 0 ? Math.round((r.subtotal / costosTotales) * 1000) / 10 : 0,
      });
    }

    const paletaCostos = ['#f59e0b', '#6366f1', '#06b6d4', '#ec4899', '#10b981', '#84cc16', '#3b82f6'];
    const itemsCostoConColor = itemsCosto.map((item, idx) => ({
      ...item,
      color: paletaCostos[idx % paletaCostos.length],
    }));

    // Dona de Costos SVG: circunferencia con r=68 es ~427.25
    const circDona = 427.25;
    let offsetAcumulado = 0;
    const segmentosDona = itemsCostoConColor.map((item) => {
      const longitud = Math.round((item.pctPresupuesto / 100) * circDona);
      const dasharray = `${longitud} ${Math.round(circDona)}`;
      const dashoffset = -Math.round(offsetAcumulado);
      offsetAcumulado += longitud;
      return {
        ...item,
        dasharray,
        dashoffset,
      };
    });

    // 4. Embudo de Aforo, Venta y Asistencia Real (Check-in QR vs. No-Show)
    const ocupacionPct = aforo > 0 ? Math.min(100, Math.round((vendidos / aforo) * 1000) / 10) : 0;
    const boletosDisponibles = Math.max(0, aforo - vendidos);
    const asistentesReales = ev.asistentesReales ?? 0;
    const tieneCheckIn = asistentesReales > 0 || ev.estado === 'FINALIZADO';
    const noShows = tieneCheckIn ? Math.max(0, vendidos - asistentesReales) : 0;
    const tasaShowUpPct = tieneCheckIn && vendidos > 0 ? Math.round((asistentesReales / vendidos) * 1000) / 10 : 0;
    const noShowPct = tieneCheckIn && vendidos > 0 ? Math.round((noShows / vendidos) * 1000) / 10 : 0;
    const asistenciaSobreAforoPct = aforo > 0 ? Math.min(100, Math.round((asistentesReales / aforo) * 1000) / 10) : 0;

    // Escala del embudo sobre ancho base 420px
    const aforoBase = Math.max(aforo, vendidos, 1);
    const funnelAforoAncho = 420;
    const funnelVendidosAncho = vendidos > 0 ? Math.max(12, Math.round((vendidos / aforoBase) * 420)) : 0;
    const funnelAsistentesAncho = asistentesReales > 0 ? Math.max(12, Math.round((asistentesReales / aforoBase) * 420)) : 0;
    const funnelNoShowAncho = noShows > 0 ? Math.max(10, Math.round((noShows / aforoBase) * 420)) : 0;

    // 5. Cobertura Operativa y Personal (Staff / Voluntarios del Concierto)
    const convocatoriasEvento = this.convocatorias().filter(
      (c) => c.idEvento === ev.idEvento || c.nombreEvento === ev.nombre
    );
    const totalStaffRequerido = convocatoriasEvento.reduce((acc, c) => acc + (c.cantidadRequerida || 0), 0);

    return {
      ...ev,
      nombreEspacio,
      ticketPromedio,
      boletosEquilibrio,
      diferenciaBoletosEquilibrio,
      coberturaCostosPct,
      anchoIngresosPct,
      anchoCostosPct,
      anchoBalancePct,
      cascadaFinanciera,
      tiers,
      pctEspacio,
      pctRecursos,
      costosCompacto: this.formatearCompacto(costosTotales),
      itemsCosto: itemsCostoConColor,
      segmentosDona,
      ocupacionPct,
      boletosDisponibles,
      asistentesReales,
      tieneCheckIn,
      noShows,
      tasaShowUpPct,
      noShowPct,
      asistenciaSobreAforoPct,
      funnelAforoAncho,
      funnelVendidosAncho,
      funnelAsistentesAncho,
      funnelNoShowAncho,
      convocatoriasEvento,
      totalStaffRequerido,
    };
  });

  readonly barrasGraficoIa = computed(() => {
    const rep = this.reporteIa();
    const datos = rep?.grafico_datos ?? [];
    if (!datos.length) return [];
    const maxVal = Math.max(...datos.map((d) => Math.abs(Number(d.valor) || 0)), 1);
    return datos.slice(0, 8).map((d) => {
      const val = Number(d.valor) || 0;
      return {
        etiqueta: d.etiqueta,
        valor: val,
        negativo: val < 0,
        extra: d.extra || String(d.valor),
        anchoPct: Math.max(6, Math.round((Math.abs(val) / maxVal) * 100)),
      };
    });
  });

  // --- COMPARADOR ESTRATÉGICO DE EVENTOS, CONCIERTOS Y GÉNEROS ---
  readonly comparadorConciertos = computed(() => {
    const eventos = this.resumenFinanciero()?.detalleEventos ?? [];
    if (!eventos.length) return null;

    const clasificarSegmento = (nombre: string): { genero: string; color: string } => {
      const n = (nombre || '').toLowerCase();
      if (n.includes('tomorrow') || n.includes('rave') || n.includes('odyssey') || n.includes('electronic')) {
        return { genero: 'Electrónica (EDM)', color: '#10b981' };
      }
      if (n.includes('perreo') || n.includes('urban') || n.includes('latin') || n.includes('flow')) {
        return { genero: 'Urbano / Reggaeton', color: '#06b6d4' };
      }
      if (n.includes('indie') || n.includes('rock')) {
        return { genero: 'Indie & Rock', color: '#ef4444' };
      }
      if (n.includes('congreso') || n.includes('conferencia') || n.includes('foro')) {
        return { genero: 'Congresos & Foros', color: '#6366f1' };
      }
      if (n.includes('seminario') || n.includes('taller') || n.includes('capacita')) {
        return { genero: 'Seminarios & Talleres', color: '#0ea5e9' };
      }
      if (n.includes('hackathon') || n.includes('innovaci')) {
        return { genero: 'Hackathons & Tech', color: '#f59e0b' };
      }
      return { genero: 'Eventos Generales', color: '#8b5cf6' };
    };

    // Escalas dinámicas según el tenant activo
    const maxBoletosRaw = Math.max(...eventos.map((e) => e.inscripcionesConfirmadas || 0), 10);
    const xMax = Math.ceil(maxBoletosRaw * 1.15);

    const maxPosRaw = Math.max(...eventos.map((e) => (e.balanceNeto > 0 ? e.balanceNeto : 0)), 100);
    const yMaxPos = Math.ceil(maxPosRaw * 1.15);

    const maxNegRaw = Math.max(...eventos.map((e) => (e.balanceNeto < 0 ? Math.abs(e.balanceNeto) : 0)), 0);
    const tieneNegativos = maxNegRaw > 0;
    const yMaxNeg = tieneNegativos ? Math.ceil(maxNegRaw * 1.2) : 0;

    const maxIngresos = Math.max(...eventos.map((e) => e.ingresos || 0), 100);

    // Línea Cero de flotación: deja espacio abajo solo si hay eventos en déficit
    const yZero = tieneNegativos ? 172 : 208;
    const alturaPos = yZero - 36; // De y=36 hasta yZero
    const alturaNeg = 222 - yZero; // De yZero hasta y=222

    const rawItems = eventos.map((ev) => {
      const g = clasificarSegmento(ev.nombre);
      const boletos = ev.inscripcionesConfirmadas || 0;
      const balance = ev.balanceNeto || 0;
      const ingresos = ev.ingresos || 0;
      const roi = ev.roi || 0;

      // Eje X en viewBox 820x265: de 88 a 760 px
      const cx = Math.min(760, Math.max(88, Math.round(85 + (boletos / xMax) * 675)));

      // Eje Y dinámico
      let cy = yZero;
      if (balance >= 0) {
        cy = Math.max(36, Math.round(yZero - (balance / yMaxPos) * alturaPos));
      } else {
        cy = Math.min(222, Math.round(yZero + (Math.abs(balance) / Math.max(yMaxNeg, 1)) * alturaNeg));
      }

      const radio = Math.max(13, Math.min(24, Math.round((ingresos / maxIngresos) * 13) + 11));
      const nombreLimpio = (ev.nombre || '').trim();
      const nombreCorto = nombreLimpio.length > 22 ? `${nombreLimpio.slice(0, 21).trim()}…` : nombreLimpio;
      const pillW = Math.max(64, Math.round(nombreCorto.length * 5.3 + 14));

      let veredicto = 'Equilibrio';
      let veredictoClase = 'verdict-growth';
      if (balance < 0) {
        veredicto = 'Déficit (Rediseñar)';
        veredictoClase = 'verdict-danger';
      } else if (roi >= 250) {
        veredicto = 'Pilar Oro (Cash Cow)';
        veredictoClase = 'verdict-cash-cow';
      } else if (roi >= 40) {
        veredicto = 'Rentable';
        veredictoClase = 'verdict-growth';
      }

      return {
        ...ev,
        genero: g.genero,
        colorGenero: g.color,
        cx,
        cy,
        radio,
        nombreCorto,
        pillW,
        labelX: cx,
        labelY: cy - radio - 8,
        pillX: cx - Math.round(pillW / 2),
        pillY: cy - radio - 20,
        balanceCompacto: this.formatearCompacto(balance, true),
        veredicto,
        veredictoClase,
      };
    });

    // 1. Separación mínima entre burbujas para evitar que dos círculos se tapen entre sí
    const ordenadosPorX = [...rawItems].sort((a, b) => a.cx - b.cx || a.cy - b.cy);
    for (let pass = 0; pass < 3; pass++) {
      for (let i = 0; i < ordenadosPorX.length; i++) {
        for (let j = i + 1; j < ordenadosPorX.length; j++) {
          const a = ordenadosPorX[i];
          const b = ordenadosPorX[j];
          const dx = b.cx - a.cx;
          const dy = b.cy - a.cy;
          const dist = Math.hypot(dx, dy);
          const minDist = a.radio + b.radio + 8;
          if (dist < minDist) {
            const empuje = Math.ceil(minDist - Math.abs(dy) * 0.65);
            b.cx = Math.min(772, Math.max(88, a.cx + Math.max(empuje, a.radio + b.radio + 6)));
          }
        }
      }
    }

    // 2. Algoritmo de posicionamiento anti-colisión para las etiquetas de texto
    const cajasOcupadas: Array<{ x1: number; y1: number; x2: number; y2: number }> = [];
    for (const item of ordenadosPorX) {
      const r = item.radio;
      const pw = item.pillW;
      const halfW = Math.round(pw / 2);

      const candidatos = [
        { lx: item.cx, ly: item.cy - r - 9 },                          // Arriba centro
        { lx: item.cx, ly: item.cy + r + 17 },                         // Abajo centro
        { lx: item.cx + r + halfW + 8, ly: item.cy + 4 },              // Derecha
        { lx: item.cx - r - halfW - 8, ly: item.cy + 4 },              // Izquierda
        { lx: item.cx + halfW - 6, ly: item.cy - r - 10 },             // Arriba derecha
        { lx: item.cx - halfW + 6, ly: item.cy - r - 10 },             // Arriba izquierda
        { lx: item.cx + halfW - 6, ly: item.cy + r + 18 },             // Abajo derecha
        { lx: item.cx - halfW + 6, ly: item.cy + r + 18 },             // Abajo izquierda
        { lx: item.cx, ly: item.cy - r - 27 },                         // Arriba lejos
        { lx: item.cx, ly: item.cy + r + 34 },                         // Abajo lejos
      ];

      let mejor = candidatos[0];
      let menorPenalidad = Number.POSITIVE_INFINITY;

      for (const cand of candidatos) {
        const lx = Math.min(808 - halfW, Math.max(68 + halfW, Math.round(cand.lx)));
        const ly = Math.min(230, Math.max(20, Math.round(cand.ly)));
        const box = { x1: lx - halfW - 2, y1: ly - 12, x2: lx + halfW + 2, y2: ly + 4 };

        let penalidad = 0;
        if (cand.ly < 22 || cand.ly > 230) penalidad += 400;
        if (cand.lx - halfW < 66 || cand.lx + halfW > 810) penalidad += 250;

        // Evitar que la etiqueta tape cualquier burbuja
        for (const otro of ordenadosPorX) {
          const cxClamped = Math.max(box.x1, Math.min(otro.cx, box.x2));
          const cyClamped = Math.max(box.y1, Math.min(otro.cy, box.y2));
          const distCirculo = Math.hypot(otro.cx - cxClamped, otro.cy - cyClamped);
          if (distCirculo < otro.radio + 3) {
            penalidad += 1000 + (otro.radio + 3 - distCirculo) * 50;
          }
        }

        // Evitar que dos etiquetas se superpongan entre sí
        for (const ocupada of cajasOcupadas) {
          const overlapX = Math.min(box.x2, ocupada.x2) - Math.max(box.x1, ocupada.x1);
          const overlapY = Math.min(box.y2, ocupada.y2) - Math.max(box.y1, ocupada.y1);
          if (overlapX > 0 && overlapY > 0) {
            penalidad += 1500 + overlapX * overlapY * 10;
          }
        }

        if (penalidad < menorPenalidad) {
          menorPenalidad = penalidad;
          mejor = { lx, ly };
          if (penalidad === 0) break;
        }
      }

      item.labelX = mejor.lx;
      item.labelY = mejor.ly;
      item.pillX = mejor.lx - halfW;
      item.pillY = mejor.ly - 11;
      cajasOcupadas.push({
        x1: item.pillX - 2,
        y1: item.pillY - 1,
        x2: item.pillX + pw + 2,
        y2: item.pillY + 17,
      });
    }

    const items = rawItems;

    // Agrupación por segmento/género
    const grupos: Record<
      string,
      { totalBoletos: number; totalNeto: number; totalCostos: number; totalIngresos: number; count: number; color: string }
    > = {};

    for (const it of items) {
      if (!grupos[it.genero]) {
        grupos[it.genero] = {
          totalBoletos: 0,
          totalNeto: 0,
          totalCostos: 0,
          totalIngresos: 0,
          count: 0,
          color: it.colorGenero,
        };
      }
      grupos[it.genero].totalBoletos += it.inscripcionesConfirmadas || 0;
      grupos[it.genero].totalNeto += it.balanceNeto || 0;
      grupos[it.genero].totalCostos += it.costosTotales || 0;
      grupos[it.genero].totalIngresos += it.ingresos || 0;
      grupos[it.genero].count += 1;
    }

    const kpisGeneros = Object.entries(grupos).map(([genero, data]) => {
      const roiProm =
        data.totalCostos > 0
          ? (data.totalNeto / data.totalCostos) * 100
          : data.totalNeto > 0
            ? 100
            : 0;
      return {
        genero,
        color: data.color,
        totalBoletos: data.totalBoletos,
        totalNeto: data.totalNeto,
        roiProm: Math.round(roiProm * 10) / 10,
        count: data.count,
      };
    });

    const ejes = {
      yZero,
      tieneNegativos,
      yMaxLabel: `+${this.formatearCompacto(yMaxPos)}`,
      yMidLabel: `+${this.formatearCompacto(yMaxPos / 2)}`,
      yMidY: Math.round(36 + alturaPos / 2),
      yNegLabel: tieneNegativos ? `-${this.formatearCompacto(yMaxNeg)}` : '',
      xTicks: [
        { x: 254, label: this.formatearCompacto(Math.round(xMax * 0.25)) },
        { x: 423, label: this.formatearCompacto(Math.round(xMax * 0.5)) },
        { x: 591, label: this.formatearCompacto(Math.round(xMax * 0.75)) },
        { x: 760, label: `${this.formatearCompacto(xMax)} bol.` },
      ],
    };

    return { items, kpisGeneros, ejes };
  });

  // --- CATÁLOGO: PARTICIPANTES ---
  readonly busquedaParticipante = signal('');
  readonly estadoParticipante = signal('TODOS');
  readonly eventoParticipante = signal<number | null>(null);
  readonly perfilColumnas = signal<PerfilColumnas>('basico');
  readonly mostrarColumnasAvanzadas = signal(false);
  readonly mostrarEnvioCorreo = signal(false);
  readonly correoDestino = signal('');
  readonly formatoCorreo = signal<FormatoReporteParticipantes>('PDF');
  readonly enviandoCorreo = signal(false);
  readonly vistaPreviaParticipantes = signal<VistaPreviaReporteParticipantes | null>(null);
  readonly cargandoParticipantes = signal(false);

  readonly columnasDisponibles: { id: ColumnaReporteParticipantes; nombre: string }[] = [
    { id: 'ID', nombre: 'ID' },
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

  readonly columnasParticipantes = signal<ColumnaReporteParticipantes[]>([
    'NOMBRES',
    'APELLIDOS',
    'CORREO',
    'ESTADO',
    'TOTAL_INSCRIPCIONES',
  ]);

  // --- CATÁLOGO: FILTROS SIMPLES ---
  readonly filtroEstadoEvento = signal('TODOS');
  readonly eventosFiltrados = computed(() => {
    const f = this.filtroEstadoEvento();
    return f === 'TODOS' ? this.eventos() : this.eventos().filter((e) => e.estado === f);
  });

  readonly filtroEstadoConvocatoria = signal('TODOS');
  readonly convocatoriasFiltradas = computed(() => {
    const f = this.filtroEstadoConvocatoria();
    return f === 'TODOS' ? this.convocatorias() : this.convocatorias().filter((c) => c.estado === f);
  });

  readonly filtroEstadoRecurso = signal('TODOS');
  readonly recursosFiltrados = computed(() => {
    const f = this.filtroEstadoRecurso();
    return f === 'TODOS' ? this.recursos() : this.recursos().filter((r) => r.estado === f);
  });

  readonly totalCapacidadEspacios = computed(() =>
    this.espacios().reduce((acc, e) => acc + (e.capacidad || 0), 0)
  );

  constructor(
    private readonly participantesService: ParticipantesService,
    private readonly eventosService: EventosService,
    private readonly convocatoriasService: ConvocatoriasService,
    private readonly espaciosService: EspaciosService,
    private readonly recursosService: RecursosService,
    private readonly toast: ToastService,
    public readonly sesion: SesionService,
    private readonly reportesIaService: ReportesIaService,
    public readonly vozService: ReconocimientoVozService
  ) {
    effect(() => {
      const detectado = this.vozService.textoDetectado();
      if (this.vozService.escuchando() && detectado) {
        this.consultaTextoIa = detectado;
      }
    });

    effect(() => {
      const err = this.vozService.error();
      if (err) {
        this.toast.error(err);
      }
    });
  }

  ngOnInit() {
    this.cargarDatosGenerales();
    this.generarReportePorVoz('Balance ejecutivo general de la organización', true);
  }

  cargarDatosGenerales() {
    this.cargandoResumen.set(true);

    this.eventosService.listarEventos().subscribe({
      next: (lista) => this.eventos.set(lista),
      error: () => {},
    });

    this.participantesService.listar(null, '', '', 0, 1).subscribe({
      next: (res) => this.totalParticipantes.set(res.totalElementos),
      error: () => {},
    });

    this.convocatoriasService.listarConvocatorias().subscribe({
      next: (lista) => this.convocatorias.set(lista),
      error: () => {},
    });

    this.espaciosService.listarEspacios().subscribe({
      next: (lista) => this.espacios.set(lista),
      error: () => {},
    });

    this.recursosService.listarRecursos().subscribe({
      next: (lista) => this.recursos.set(lista),
      error: () => {},
      complete: () => this.cargandoResumen.set(false),
    });
  }

  cambiarModo(modo: ModoVista) {
    this.modoActivo.set(modo);
    if ((modo === 'analitico' || (modo === 'catalogo' && this.catalogoActivo() === 'finanzas')) && !this.resumenFinanciero()) {
      this.cargarFinanzas();
    }
    if (modo === 'catalogo' && this.catalogoActivo() === 'participantes' && !this.vistaPreviaParticipantes()) {
      this.cargarVistaPreviaParticipantes();
    }
  }

  seleccionarCatalogo(tipo: TipoCatalogo) {
    this.catalogoActivo.set(tipo);
    this.modoActivo.set('catalogo');
    if (tipo === 'finanzas' && !this.resumenFinanciero()) {
      this.cargarFinanzas();
    }
    if (tipo === 'participantes' && !this.vistaPreviaParticipantes()) {
      this.cargarVistaPreviaParticipantes();
    }
  }

  cargarFinanzas() {
    this.cargandoFinanzas.set(true);
    this.reportesIaService.obtenerBalanceFinanciero().subscribe({
      next: (data) => {
        this.resumenFinanciero.set(data);
        this.cargandoFinanzas.set(false);
      },
      error: (err) => {
        this.toast.error('No se pudo cargar el balance financiero');
        this.cargandoFinanzas.set(false);
      },
    });
  }

  exportarFinanzas(formato: 'PDF' | 'XLSX') {
    const fin = this.resumenFinanciero();
    if (!fin) return;
    this.exportando.set(formato);
    const kpis: KpiReporte[] = [
      { etiqueta: 'Ingresos Totales', valor: `${fin.ingresosTotales.toLocaleString('es-ES', { minimumFractionDigits: 2 })} Bs`, detalle: 'Venta de entradas confirmadas' },
      { etiqueta: 'Costos Operativos', valor: `${fin.costosTotales.toLocaleString('es-ES', { minimumFractionDigits: 2 })} Bs`, detalle: 'Salones y recursos asignados' },
      { etiqueta: 'Balance Neto', valor: `${fin.balanceNetoGlobal.toLocaleString('es-ES', { minimumFractionDigits: 2 })} Bs`, detalle: 'Utilidad o déficit global' },
      { etiqueta: 'ROI Promedio', valor: `${fin.roiGlobal.toFixed(1)}%`, detalle: 'Rendimiento sobre costo' },
    ];
    const columnas = ['Evento', 'Boletos', 'Desglose Entradas', 'Ingresos (Bs)', 'Costo Espacio', 'Costo Recursos', 'Costos Totales', 'Balance Neto', 'ROI', 'Situación'];
    const filas = fin.detalleEventos.map((e) => {
      const desglose = (e.desgloseEntradas ?? []).length > 0
        ? e.desgloseEntradas!
            .map((d) => `${d.categoria}: ${d.cantidadVendida.toLocaleString('es-ES')} x ${d.precioUnitario.toFixed(0)} Bs`)
            .join(' | ')
        : 'Sin ventas';
      return [
        e.nombre,
        String(e.inscripcionesConfirmadas),
        desglose,
        e.ingresos.toFixed(2),
        e.costoEspacio.toFixed(2),
        e.costoRecursos.toFixed(2),
        e.costosTotales.toFixed(2),
        e.balanceNeto.toFixed(2),
        `${e.roi.toFixed(1)}%`,
        e.situacion,
      ];
    });
    const solicitud: ExportarReporteDinamicoSolicitud = {
      titulo: 'Reporte de Balance Financiero y Rentabilidad',
      subtitulo: 'Desglose económico integral de ingresos por venta de entradas vs. costos de espacios y recursos',
      periodo: 'Histórico consolidado',
      resumenEjecutivo: `La organización registra un balance neto de ${fin.balanceNetoGlobal.toFixed(2)} Bs con un ROI consolidado de ${fin.roiGlobal.toFixed(1)}%. Se contabilizan ${fin.eventosConGanancia} eventos con superávit y ${fin.eventosConPerdida} eventos con déficit.`,
      kpis,
      columnas,
      filas,
      conclusiones: [
        `Ingresos totales recaudados: ${fin.ingresosTotales.toFixed(2)} Bs.`,
        `Costos operativos totales ejecutados: ${fin.costosTotales.toFixed(2)} Bs.`,
        `Margen global obtenido: ${fin.roiGlobal.toFixed(1)}%.`
      ],
      recomendaciones: [
        'Ajustar tarifas en eventos con margen negativo o sumar auspiciadores para cubrir el alquiler de espacios.',
        'Priorizar paquetes VIP con mayor rentabilidad unitaria.'
      ]
    };
    this.reportesIaService.exportarDinamico(solicitud, formato).subscribe({
      next: (blob: Blob) => {
        this.descargarBlob(blob, `balance-financiero-${new Date().toISOString().slice(0, 10)}.${formato.toLowerCase()}`);
        this.exportando.set(null);
        this.toast.exito(`Balance financiero descargado en formato ${formato}.`);
      },
      error: () => {
        this.toast.error('Error al exportar balance');
        this.exportando.set(null);
      }
    });
  }

  exportarRadiografiaEvento(formato: 'PDF' | 'XLSX') {
    const rad = this.radiografiaEvento();
    if (!rad) return;
    this.exportando.set(formato);

    const kpis: KpiReporte[] = [
      {
        etiqueta: 'Ingresos del Evento',
        valor: `${rad.ingresos.toLocaleString('es-ES', { minimumFractionDigits: 2 })} Bs`,
        detalle: `Ticket promedio: ${rad.ticketPromedio.toFixed(2)} Bs`,
      },
      {
        etiqueta: 'Costos de Producción',
        valor: `${rad.costosTotales.toLocaleString('es-ES', { minimumFractionDigits: 2 })} Bs`,
        detalle: `Recinto + ${rad.itemsCosto.length - (rad.costoEspacio > 0 ? 1 : 0)} recursos`,
      },
      {
        etiqueta: 'Resultado Neto',
        valor: `${(rad.balanceNeto >= 0 ? '+' : '')}${rad.balanceNeto.toLocaleString('es-ES', { minimumFractionDigits: 2 })} Bs`,
        detalle: `${rad.situacion} (ROI: ${rad.roi.toFixed(1)}%)`,
      },
      {
        etiqueta: 'Punto de Equilibrio',
        valor: `${rad.boletosEquilibrio.toLocaleString('es-ES')} boletos`,
        detalle: `${rad.diferenciaBoletosEquilibrio >= 0 ? '+' : ''}${rad.diferenciaBoletosEquilibrio.toLocaleString('es-ES')} sobre equilibrio`,
      },
    ];

    const columnas = ['Concepto / Ítem', 'Tipo', 'Detalle Operativo', 'Monto (Bs)', 'Participación'];
    const filas: string[][] = [];

    for (const t of rad.tiers) {
      filas.push([
        `Boletería: ${t.categoria}`,
        'INGRESO',
        `${t.cantidadVendida.toLocaleString('es-ES')} / ${t.cupoMaximo.toLocaleString('es-ES')} vendidos (${t.precioUnitario.toFixed(2)} Bs c/u)`,
        `+${t.subtotal.toFixed(2)} Bs`,
        `${t.pctIngresosEvento}% del ingreso`,
      ]);
    }

    for (const c of rad.itemsCosto) {
      filas.push([
        c.nombre,
        c.tipoItem === 'ESPACIO' ? 'COSTO RECINTO' : 'COSTO RECURSO',
        c.detalleCantidad,
        `-${c.subtotal.toFixed(2)} Bs`,
        `${c.pctPresupuesto}% del costo`,
      ]);
    }

    const solicitud: ExportarReporteDinamicoSolicitud = {
      titulo: `Radiografía 360° de Rentabilidad y Operación: ${rad.nombre}`,
      subtitulo: `Recinto: ${rad.nombreEspacio} · Estado: ${rad.estado} · Aforo: ${rad.aforo.toLocaleString('es-ES')} personas`,
      periodo: `Evento ID #${rad.idEvento}`,
      resumenEjecutivo: `El evento "${rad.nombre}" registra ${rad.inscripcionesConfirmadas.toLocaleString('es-ES')} entradas vendidas (${rad.ocupacionPct}% del aforo), generando ingresos por ${rad.ingresos.toFixed(2)} Bs frente a costos operativos de ${rad.costosTotales.toFixed(2)} Bs, con un resultado neto de ${rad.balanceNeto.toFixed(2)} Bs (${rad.situacion}, ROI ${rad.roi.toFixed(1)}%).`,
      kpis,
      columnas,
      filas,
      conclusiones: [
        `Punto de equilibrio alcanzado en ${rad.boletosEquilibrio.toLocaleString('es-ES')} boletos (${rad.coberturaCostosPct}% de cobertura de costos).`,
        rad.tieneCheckIn
          ? `Asistencia real en puerta (Check-in QR): ${rad.asistentesReales.toLocaleString('es-ES')} personas (${rad.tasaShowUpPct}% de asistencia efectiva, ${rad.noShows.toLocaleString('es-ES')} ausentes / No-Show).`
          : `Evento en preventa activa (${rad.inscripcionesConfirmadas.toLocaleString('es-ES')} boletos vendidos; apertura de puertas pendiente).`,
        `Personal operativo requerido en convocatorias: ${rad.totalStaffRequerido} integrantes en ${rad.convocatoriasEvento.length} convocatorias.`,
      ],
      recomendaciones: [
        rad.balanceNeto >= 0
          ? 'Mantener la estructura de precios escalonada (General, VIP, Super VIP) que permitió superar ampliamente el punto de equilibrio.'
          : 'Revisar el dimensionamiento técnico de recursos y alquiler del recinto o incorporar patrocinadores para cubrir los costos fijos.',
        'Monitorear la tasa de No-Show por categoría para optimizar el dimensionamiento de accesos QR, barras y seguridad.',
      ],
    };

    this.reportesIaService.exportarDinamico(solicitud, formato).subscribe({
      next: (blob: Blob) => {
        this.descargarBlob(blob, `radiografia-evento-${rad.idEvento}.${formato.toLowerCase()}`);
        this.exportando.set(null);
        this.toast.exito(`Radiografía del evento descargada en ${formato}.`);
      },
      error: () => {
        this.toast.error('Error al exportar la radiografía del evento');
        this.exportando.set(null);
      }
    });
  }

  private descargarBlob(blob: Blob, nombre: string) {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = nombre;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  // --- VOZ & GENERADOR DE REPORTES IA ---
  toggleMicrofono() {
    if (!this.vozService.soportado()) {
      this.toast.error('Tu navegador no soporta dictado por voz. Usá Chrome o Edge.');
      return;
    }
    if (this.vozService.escuchando()) {
      this.vozService.detener();
      const detectado = this.vozService.textoDetectado().trim();
      if (detectado) {
        this.consultaTextoIa = detectado;
        this.generarReportePorVoz(detectado);
      }
    } else {
      this.toast.info('Micrófono activo. Dictá qué reporte necesitás...');
      this.vozService.iniciar((textoFinal) => {
        if (textoFinal) {
          this.consultaTextoIa = textoFinal;
          this.generarReportePorVoz(textoFinal);
        }
      });
    }
  }

  seleccionarSugerencia(sugerencia: string) {
    this.consultaTextoIa = sugerencia;
    this.generarReportePorVoz(sugerencia);
  }

  generarReportePorVoz(textoOpcional?: string, silencioso = false) {
    const consulta = (textoOpcional ?? this.consultaTextoIa ?? this.vozService.textoDetectado()).trim();
    if (!consulta) {
      this.toast.info('Escribí o dictá qué reporte querés generar.');
      return;
    }
    if (!silencioso) {
      this.consultaTextoIa = consulta;
    }
    this.modoActivo.set('ia');
    this.generandoReporteIa.set(true);

    this.reportesIaService.generarReporteIa(consulta).subscribe({
      next: (reporte) => {
        this.reporteIa.set(reporte);
        this.generandoReporteIa.set(false);
        if (!silencioso) {
          this.toast.exito('Informe ejecutivo generado.');
        }
      },
      error: (err) => {
        this.generandoReporteIa.set(false);
        if (!silencioso) {
          this.toast.error(obtenerMensajeError(err, 'No se pudo generar el reporte inteligente.'));
        }
      },
    });
  }

  exportarReporteIa(formato: 'PDF' | 'XLSX') {
    const rep = this.reporteIa();
    if (!rep) return;

    const solicitud: ExportarReporteDinamicoSolicitud = {
      titulo: rep.titulo,
      subtitulo: rep.subtitulo,
      periodo: rep.periodo,
      resumenEjecutivo: rep.resumen_ejecutivo,
      kpis: rep.kpis,
      columnas: rep.columnas,
      filas: rep.filas,
      conclusiones: rep.conclusiones,
      recomendaciones: rep.recomendaciones,
    };
    this.ejecutarDescargaDinamica(solicitud, formato, 'reporte-ia');
  }

  // --- PARTICIPANTES: PRESETS Y VISTA PREVIA ---
  aplicarPerfilColumnas(perfil: PerfilColumnas) {
    this.perfilColumnas.set(perfil);
    if (perfil === 'basico') {
      this.columnasParticipantes.set(['NOMBRES', 'APELLIDOS', 'CORREO', 'ESTADO', 'TOTAL_INSCRIPCIONES']);
    } else if (perfil === 'inscripciones') {
      this.columnasParticipantes.set([
        'NOMBRES',
        'APELLIDOS',
        'CORREO',
        'TOTAL_INSCRIPCIONES',
        'INSCRIPCIONES_CONFIRMADAS',
        'INSCRIPCIONES_CANCELADAS',
        'ULTIMA_INSCRIPCION',
      ]);
    } else {
      this.columnasParticipantes.set(this.columnasDisponibles.map((c) => c.id));
    }
    this.cargarVistaPreviaParticipantes();
  }

  alternarColumna(columna: ColumnaReporteParticipantes, marcada: boolean) {
    this.columnasParticipantes.update((actuales) =>
      marcada ? [...new Set([...actuales, columna])] : actuales.filter((c) => c !== columna)
    );
    this.cargarVistaPreviaParticipantes();
  }

  private solicitudParticipantes() {
    return {
      eventoId: this.eventoParticipante(),
      busqueda: this.busquedaParticipante(),
      estado:
        this.estadoParticipante() === 'TODOS'
          ? null
          : (this.estadoParticipante() as 'ACTIVO' | 'INACTIVO'),
      columnas: this.columnasParticipantes(),
      ordenarPor: 'APELLIDOS' as ColumnaReporteParticipantes,
      ascendente: true,
    };
  }

  cargarVistaPreviaParticipantes() {
    if (!this.columnasParticipantes().length) {
      this.toast.error('Seleccioná al menos una columna.');
      return;
    }
    this.cargandoParticipantes.set(true);
    this.participantesService.vistaPreviaReporte(this.solicitudParticipantes()).subscribe({
      next: (preview) => {
        this.vistaPreviaParticipantes.set(preview);
        this.cargandoParticipantes.set(false);
      },
      error: (err) => {
        this.toast.error(obtenerMensajeError(err, 'No se pudo cargar el reporte de participantes.'));
        this.cargandoParticipantes.set(false);
      },
    });
  }

  exportarParticipantes(formato: 'PDF' | 'XLSX') {
    const preview = this.vistaPreviaParticipantes();
    if (!preview) {
      this.cargarVistaPreviaParticipantes();
      return;
    }
    const eventoNombre = this.eventoParticipante()
      ? this.eventos().find((e) => e.id === this.eventoParticipante())?.nombre || 'Evento seleccionado'
      : 'Todos los eventos';

    const solicitud: ExportarReporteDinamicoSolicitud = {
      titulo: 'Reporte de Participantes e Inscripciones',
      subtitulo: `Filtro: ${eventoNombre} · Estado: ${this.estadoParticipante()}`,
      periodo: 'Datos vigentes',
      resumenEjecutivo: `Se incluyen ${preview.totalFilas} participantes correspondientes al filtro "${eventoNombre}" con estado ${this.estadoParticipante()}.`,
      kpis: [
        { etiqueta: 'Total Registros', valor: String(preview.totalFilas), detalle: 'Participantes filtrados' },
        { etiqueta: 'Columnas', valor: String(preview.columnas.length), detalle: 'Campos incluidos' },
        { etiqueta: 'Alcance', valor: eventoNombre.slice(0, 22), detalle: 'Filtro de evento' },
      ],
      columnas: preview.columnas,
      filas: preview.filas,
    };
    this.ejecutarDescargaDinamica(solicitud, formato, 'reporte-participantes');
  }

  enviarParticipantesPorCorreo() {
    const correo = this.correoDestino().trim();
    if (!correo) {
      this.toast.error('Ingresá un correo destinatario válido.');
      return;
    }
    this.enviandoCorreo.set(true);
    this.participantesService
      .enviarReporte(correo, this.solicitudParticipantes(), this.formatoCorreo())
      .subscribe({
        next: () => {
          this.toast.exito(`Reporte enviado en ${this.formatoCorreo()} a ${correo}.`);
          this.enviandoCorreo.set(false);
          this.correoDestino.set('');
        },
        error: (err) => {
          this.toast.error(obtenerMensajeError(err, 'No fue posible enviar el correo.'));
          this.enviandoCorreo.set(false);
        },
      });
  }

  // --- EXPORTACIÓN UNIVERSAL PDF / XLSX PARA TODO EL CATÁLOGO ---
  exportarEventos(formato: 'PDF' | 'XLSX') {
    const lista = this.eventosFiltrados();
    const capTotal = lista.reduce((a, e) => a + (e.aforoMaximo || 0), 0);
    const publicados = lista.filter((e) => e.estado === 'PUBLICADO').length;

    const solicitud: ExportarReporteDinamicoSolicitud = {
      titulo: 'Reporte de Eventos y Capacidad',
      subtitulo: `Filtro de estado: ${this.filtroEstadoEvento()}`,
      periodo: 'Programación vigente',
      resumenEjecutivo: `Se detallan ${lista.length} eventos (${publicados} publicados) con una capacidad acumulada de ${capTotal} plazas.`,
      kpis: [
        { etiqueta: 'Eventos', valor: String(lista.length), detalle: 'En el filtro actual' },
        { etiqueta: 'Publicados', valor: String(publicados), detalle: 'Activos para inscripción' },
        { etiqueta: 'Capacidad Total', valor: String(capTotal), detalle: 'Plazas habilitadas' },
      ],
      columnas: ['ID', 'Evento', 'Estado', 'Fecha Inicio', 'Aforo Máximo', 'Ubicación'],
      filas: lista.map((e) => [
        String(e.id),
        e.nombre || '-',
        e.estado || '-',
        e.fechaInicio ? e.fechaInicio.slice(0, 10) : '-',
        e.aforoMaximo != null ? String(e.aforoMaximo) : 'Ilimitado',
        e.lugarPrincipal || '-',
      ]),
    };
    this.ejecutarDescargaDinamica(solicitud, formato, 'reporte-eventos');
  }

  exportarConvocatorias(formato: 'PDF' | 'XLSX') {
    const lista = this.convocatoriasFiltradas();
    const abiertas = lista.filter((c) => c.estado === 'ABIERTA').length;
    const cupos = lista.reduce((a, c) => a + (c.cantidadRequerida || 0), 0);

    const solicitud: ExportarReporteDinamicoSolicitud = {
      titulo: 'Reporte de Convocatorias y Personal (Staff)',
      subtitulo: `Filtro de estado: ${this.filtroEstadoConvocatoria()}`,
      periodo: 'Datos vigentes',
      resumenEjecutivo: `Se registran ${lista.length} convocatorias de personal (${abiertas} abiertas) con un requerimiento acumulado de ${cupos} cupos.`,
      kpis: [
        { etiqueta: 'Convocatorias', valor: String(lista.length), detalle: 'Registros filtrados' },
        { etiqueta: 'Abiertas', valor: String(abiertas), detalle: 'Vigentes' },
        { etiqueta: 'Cupos Totales', valor: String(cupos), detalle: 'Personal requerido' },
      ],
      columnas: ['ID', 'Evento', 'Tipo Personal', 'Estado', 'Cupos', 'Fecha Límite'],
      filas: lista.map((c) => [
        String(c.id),
        c.nombreEvento || '-',
        c.tipoPersonal || '-',
        c.estado || '-',
        String(c.cantidadRequerida ?? 0),
        c.fechaLimite || '-',
      ]),
    };
    this.ejecutarDescargaDinamica(solicitud, formato, 'reporte-convocatorias');
  }

  exportarEspacios(formato: 'PDF' | 'XLSX') {
    const lista = this.espacios();
    const capTotal = this.totalCapacidadEspacios();

    const solicitud: ExportarReporteDinamicoSolicitud = {
      titulo: 'Reporte de Espacios, Salas y Capacidad Instalada',
      subtitulo: 'Infraestructura disponible en la organización',
      periodo: 'Catálogo vigente',
      resumenEjecutivo: `La organización cuenta con ${lista.length} espacios físicos que suman una capacidad instalada total de ${capTotal} personas.`,
      kpis: [
        { etiqueta: 'Espacios / Salas', valor: String(lista.length), detalle: 'Ambientes registrados' },
        { etiqueta: 'Aforo Acumulado', valor: String(capTotal), detalle: 'Personas en total' },
      ],
      columnas: ['ID', 'Nombre del Espacio', 'Ubicación', 'Capacidad Máxima'],
      filas: lista.map((e) => [
        String(e.id),
        e.nombre || '-',
        e.ubicacion || '-',
        String(e.capacidad ?? 0),
      ]),
    };
    this.ejecutarDescargaDinamica(solicitud, formato, 'reporte-espacios');
  }

  exportarRecursos(formato: 'PDF' | 'XLSX') {
    const lista = this.recursosFiltrados();
    const tot = lista.reduce((a, r) => a + (r.cantidadTotal || 0), 0);
    const disp = lista.reduce((a, r) => a + (r.cantidadDisponible || 0), 0);

    const solicitud: ExportarReporteDinamicoSolicitud = {
      titulo: 'Reporte de Inventario y Disponibilidad de Recursos',
      subtitulo: `Filtro de estado: ${this.filtroEstadoRecurso()}`,
      periodo: 'Inventario actual',
      resumenEjecutivo: `El inventario filtrado abarca ${lista.length} ítems con ${tot} unidades totales y ${disp} disponibles para préstamo o asignación.`,
      kpis: [
        { etiqueta: 'Ítems', valor: String(lista.length), detalle: 'Tipos de recursos' },
        { etiqueta: 'Stock Total', valor: String(tot), detalle: 'Unidades registradas' },
        { etiqueta: 'Disponibles', valor: String(disp), detalle: 'Listas para asignar' },
      ],
      columnas: ['Código', 'Recurso', 'Tipo', 'Total', 'Disponible', 'Estado', 'Ubicación'],
      filas: lista.map((r) => [
        r.codigo || '-',
        r.nombre || '-',
        r.tipo || '-',
        String(r.cantidadTotal ?? 0),
        String(r.cantidadDisponible ?? 0),
        r.estado || '-',
        r.ubicacion || '-',
      ]),
    };
    this.ejecutarDescargaDinamica(solicitud, formato, 'reporte-recursos');
  }

  private ejecutarDescargaDinamica(
    solicitud: ExportarReporteDinamicoSolicitud,
    formato: 'PDF' | 'XLSX',
    prefijo: string
  ) {
    if (this.exportando()) return;
    this.exportando.set(formato);
    this.reportesIaService.exportarDinamico(solicitud, formato).subscribe({
      next: (blob) => {
        const ext = formato.toLowerCase();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${prefijo}-${new Date().toISOString().slice(0, 10)}.${ext}`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.toast.exito(`Reporte descargado en formato ${formato}.`);
        this.exportando.set(null);
      },
      error: (err) => {
        this.toast.error(obtenerMensajeError(err, `No fue posible exportar el archivo ${formato}.`));
        this.exportando.set(null);
      },
    });
  }
}
