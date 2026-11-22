import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EventosService } from './servicios/eventos.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { Evento } from './modelos/evento.modelo';

@Component({ selector: 'app-consulta-eventos', imports: [DatePipe, DecimalPipe, FormsModule], templateUrl: './consulta-eventos.component.html', changeDetection: ChangeDetectionStrategy.OnPush })
export class ConsultaEventosComponent implements OnInit {
  readonly eventos = signal<Evento[]>([]);
  readonly categorias = signal<string[]>([]);
  readonly detalle = signal<Evento | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly consultado = signal(false);
  termino = '';
  categoria = '';

  constructor(private readonly eventosService: EventosService) {}
  ngOnInit() {}

  buscar() {
    this.consultado.set(true);
    this.cargando.set(true);
    this.eventosService.listarEventos(this.termino, this.categoria).subscribe({
      next: (eventos) => {
        this.eventos.set(eventos);
        this.categorias.set(
          Array.from(new Set(eventos.map((evento) => this.nombreCategoria(evento)))).sort(),
        );
        this.cargando.set(false);
      },
      error: (error) => { this.error.set(obtenerMensajeError(error, 'No fue posible consultar los eventos')); this.eventos.set([]); this.cargando.set(false); },
    });
  }
  limpiar() {
    this.termino = '';
    this.categoria = '';
    this.eventos.set([]);
    this.error.set('');
    this.consultado.set(false);
  }
  verDetalle(evento: Evento) { this.detalle.set(evento); }
  cerrarDetalle() { this.detalle.set(null); }
  nombreCategoria(evento: Evento) { return evento.categoria?.nombre ?? evento.categoriaNombre ?? 'GENERAL'; }
}
