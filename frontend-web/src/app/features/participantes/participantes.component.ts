import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParticipantesService } from './servicios/participantes.service';
import { EventosService } from '../eventos/servicios/eventos.service';
import { Evento } from '../eventos/modelos/evento.modelo';
import {
  ParticipanteDetalle,
  ParticipanteResumen,
  ColumnaReporteParticipantes,
  FormatoReporteParticipantes,
  VistaPreviaReporteParticipantes,
} from './modelos/participante.modelo';
import { ToastService } from '../../core/toast.service';
import { SesionService } from '../../core/sesion.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { UiPrivilegeEditableDirective, UiPrivilegeEnabledDirective, UiPrivilegeVisibleDirective } from '../../core/ui-privilege.directive';

@Component({
  selector: 'app-participantes',
  standalone: true,
  imports: [CommonModule, FormsModule, UiPrivilegeVisibleDirective, UiPrivilegeEnabledDirective, UiPrivilegeEditableDirective],
  templateUrl: './participantes.component.html',
  styleUrls: ['./participantes.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ParticipantesComponent implements OnInit {
  readonly columnasDisponibles: { id: ColumnaReporteParticipantes; nombre: string }[] = [
    { id: 'ID', nombre: 'Identificador' },
    { id: 'NOMBRES', nombre: 'Nombres' },
    { id: 'APELLIDOS', nombre: 'Apellidos' },
    { id: 'CORREO', nombre: 'Correo' },
    { id: 'ESTADO', nombre: 'Estado' },
    { id: 'TOTAL_INSCRIPCIONES', nombre: 'Total de inscripciones' },
    { id: 'INSCRIPCIONES_CONFIRMADAS', nombre: 'Inscripciones confirmadas' },
    { id: 'INSCRIPCIONES_CANCELADAS', nombre: 'Inscripciones canceladas' },
    { id: 'ULTIMA_INSCRIPCION', nombre: 'Última inscripción' },
    { id: 'FECHA_REGISTRO', nombre: 'Fecha de registro' },
  ];
  readonly columnasReporte = signal<ColumnaReporteParticipantes[]>([
    'NOMBRES', 'APELLIDOS', 'CORREO', 'ESTADO', 'TOTAL_INSCRIPCIONES', 'FECHA_REGISTRO',
  ]);
  readonly ordenarReporte = signal<ColumnaReporteParticipantes>('APELLIDOS');
  readonly ascendenteReporte = signal(true);
  readonly formatoReporte = signal<FormatoReporteParticipantes>('CSV');
  readonly correoReporte = signal('');
  readonly vistaPreviaReporte = signal<VistaPreviaReporteParticipantes | null>(null);
  readonly procesandoReporte = signal(false);
  readonly participantes = signal<ParticipanteResumen[]>([]);
  readonly eventos = signal<Evento[]>([]);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');

  // Filtros y paginación
  readonly busqueda = signal('');
  readonly estadoFiltro = signal('TODOS');
  readonly eventoFiltro = signal<number | null>(null);
  readonly pagina = signal(0);
  readonly tamano = signal(10);
  readonly totalElementos = signal(0);
  readonly totalPaginas = signal(0);

  // Modal detalle
  readonly modalDetalleAbierto = signal(false);
  readonly cargandoDetalle = signal(false);
  readonly detalle = signal<ParticipanteDetalle | null>(null);

  // Estados de acción
  readonly idCambiandoEstado = signal<number | null>(null);
  readonly exportando = signal(false);

  constructor(
    private readonly participantesService: ParticipantesService,
    private readonly eventosService: EventosService,
    private readonly toast: ToastService,
    public readonly sesion: SesionService
  ) {}

  ngOnInit() {
    this.cargarEventos();
    this.cargarParticipantes();
  }

  cargarEventos() {
    this.eventosService.listarEventos().subscribe({
      next: (lista) => this.eventos.set(lista),
      error: () => {},
    });
  }

  cargarParticipantes() {
    this.cargando.set(true);
    this.error.set('');

    this.participantesService
      .listar(
        this.eventoFiltro(),
        this.busqueda(),
        this.estadoFiltro(),
        this.pagina(),
        this.tamano()
      )
      .subscribe({
        next: (res) => {
          this.participantes.set(res.elementos);
          this.totalElementos.set(res.totalElementos);
          this.totalPaginas.set(res.totalPaginas);
          this.cargando.set(false);
        },
        error: (err) => {
          const msg = obtenerMensajeError(
            err,
            'No fue posible cargar el padrón de participantes'
          );
          this.error.set(msg);
          this.toast.error(msg);
          this.cargando.set(false);
        },
      });
  }

  aplicarFiltros() {
    this.pagina.set(0);
    this.vistaPreviaReporte.set(null);
    this.cargarParticipantes();
  }

  limpiarFiltros() {
    this.busqueda.set('');
    this.estadoFiltro.set('TODOS');
    this.eventoFiltro.set(null);
    this.pagina.set(0);
    this.vistaPreviaReporte.set(null);
    this.cargarParticipantes();
  }

  cambiarPagina(nuevaPagina: number) {
    if (nuevaPagina >= 0 && nuevaPagina < this.totalPaginas()) {
      this.pagina.set(nuevaPagina);
      this.cargarParticipantes();
    }
  }

  verDetalle(participante: ParticipanteResumen) {
    this.cargandoDetalle.set(true);
    this.modalDetalleAbierto.set(true);
    this.detalle.set(null);

    this.participantesService.obtenerDetalle(participante.idUsuario).subscribe({
      next: (res) => {
        this.detalle.set(res);
        this.cargandoDetalle.set(false);
      },
      error: (err) => {
        const msg = obtenerMensajeError(
          err,
          'No se pudo consultar el detalle del participante'
        );
        this.toast.error(msg);
        this.cargandoDetalle.set(false);
        this.modalDetalleAbierto.set(false);
      },
    });
  }

  cerrarModalDetalle() {
    this.modalDetalleAbierto.set(false);
    this.detalle.set(null);
  }

  alternarEstado(participante: ParticipanteResumen) {
    const nuevoEstado = participante.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.idCambiandoEstado.set(participante.idUsuario);

    this.participantesService
      .cambiarEstado(participante.idUsuario, nuevoEstado)
      .subscribe({
        next: (actualizado) => {
          this.participantes.update((lista) =>
            lista.map((p) =>
              p.idUsuario === actualizado.idUsuario ? actualizado : p
            )
          );
          const msg = `Participante ${actualizado.nombres} ahora está ${nuevoEstado}`;
          this.toast.exito(msg);
          this.idCambiandoEstado.set(null);
        },
        error: (err) => {
          const msg = obtenerMensajeError(err, 'No fue posible cambiar el estado');
          this.toast.error(msg);
          this.idCambiandoEstado.set(null);
        },
      });
  }

  exportarCsv() {
    this.exportando.set(true);
    this.participantesService
      .exportarCsv(this.eventoFiltro(), this.busqueda(), this.estadoFiltro())
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `participantes-${new Date().toISOString().slice(0, 10)}.csv`;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
          this.toast.exito('Descarga de participantes iniciada exitosamente');
          this.exportando.set(false);
        },
        error: (err) => {
          const msg = obtenerMensajeError(err, 'Error al exportar participantes a CSV');
          this.toast.error(msg);
          this.exportando.set(false);
        },
      });
  }

  alternarColumnaReporte(columna: ColumnaReporteParticipantes, marcada: boolean) {
    this.columnasReporte.update((actuales) => marcada
      ? [...new Set([...actuales, columna])]
      : actuales.filter((actual) => actual !== columna));
    this.vistaPreviaReporte.set(null);
  }

  private solicitudReporte() {
    return {
      eventoId: this.eventoFiltro(),
      busqueda: this.busqueda(),
      estado: this.estadoFiltro() === 'TODOS' ? null : this.estadoFiltro() as 'ACTIVO' | 'INACTIVO',
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
    this.procesandoReporte.set(true);
    this.participantesService.exportarReporte(this.solicitudReporte(), this.formatoReporte()).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `participantes-${new Date().toISOString().slice(0, 10)}.${this.formatoReporte().toLowerCase()}`;
        enlace.click();
        window.URL.revokeObjectURL(url);
        this.toast.exito('Reporte descargado.');
        this.procesandoReporte.set(false);
      },
      error: (err) => {
        this.toast.error(obtenerMensajeError(err, 'No fue posible exportar el reporte.'));
        this.procesandoReporte.set(false);
      },
    });
  }

  enviarReporte() {
    this.procesandoReporte.set(true);
    this.participantesService.enviarReporte(
      this.correoReporte().trim(), this.solicitudReporte(), this.formatoReporte()
    ).subscribe({
      next: () => {
        this.toast.exito('Reporte enviado al correo indicado.');
        this.procesandoReporte.set(false);
      },
      error: (err) => {
        this.toast.error(obtenerMensajeError(err, 'No fue posible enviar el reporte.'));
        this.procesandoReporte.set(false);
      },
    });
  }
}
