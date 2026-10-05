import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParticipantesService } from './servicios/participantes.service';
import { ParticipanteInscrito } from './modelos/participante.modelo';
import { EventosService } from '../eventos/servicios/eventos.service';
import { Evento } from '../eventos/modelos/evento.modelo';
import { ToastService } from '../../core/toast.service';
import { obtenerMensajeError } from '../../core/mensajes-error';

@Component({
  selector: 'app-participantes',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './participantes.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ParticipantesComponent implements OnInit {
  private readonly participantesService = inject(ParticipantesService);
  private readonly eventosService = inject(EventosService);
  private readonly toast = inject(ToastService);

  readonly participantes = signal<ParticipanteInscrito[]>([]);
  readonly eventos = signal<Evento[]>([]);
  readonly cargando = signal(false);
  readonly error = signal('');

  // Filtros
  readonly idEventoSeleccionado = signal<number>(0);
  readonly filtroEstado = signal<string>('TODAS');
  readonly terminoBusqueda = signal<string>('');

  readonly participantesFiltrados = computed(() => {
    const listado = this.participantes();
    const estado = this.filtroEstado();
    const busqueda = this.terminoBusqueda().toLowerCase().trim();

    return listado.filter((p) => {
      const coincideEstado = estado === 'TODAS' || p.estado === estado;
      const coincideBusqueda =
        !busqueda ||
        p.nombres.toLowerCase().includes(busqueda) ||
        p.apellidos.toLowerCase().includes(busqueda) ||
        p.correo.toLowerCase().includes(busqueda) ||
        p.tituloEvento.toLowerCase().includes(busqueda) ||
        p.codigoQr.toLowerCase().includes(busqueda);
      return coincideEstado && coincideBusqueda;
    });
  });

  ngOnInit() {
    this.cargarEventos();
    this.cargarParticipantes();
  }

  cargarEventos() {
    this.eventosService.listarEventos().subscribe({
      next: (eventos) => this.eventos.set(eventos),
      error: () => {},
    });
  }

  cargarParticipantes() {
    this.cargando.set(true);
    this.error.set('');
    const idEvento = this.idEventoSeleccionado();
    this.participantesService.listarParticipantes(idEvento > 0 ? idEvento : undefined).subscribe({
      next: (datos) => {
        this.participantes.set(datos);
        this.cargando.set(false);
      },
      error: (err) => {
        const msg = obtenerMensajeError(err, 'No fue posible cargar los participantes');
        this.error.set(msg);
        this.cargando.set(false);
        this.toast.error(msg);
      },
    });
  }

  alCambiarEvento(id: number) {
    this.idEventoSeleccionado.set(id);
    this.cargarParticipantes();
  }
}
