import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HistorialParticipacionService } from './servicios/historial-participacion.service';
import { HistorialParticipacion } from './modelos/historial-participacion.modelo';
import { obtenerMensajeError } from '../../core/mensajes-error';

@Component({
  selector: 'app-historial-participacion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './historial-participacion.component.html',
  styleUrls: ['./historial-participacion.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistorialParticipacionComponent implements OnInit {
  readonly historial = signal<HistorialParticipacion[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');

  constructor(private readonly servicio: HistorialParticipacionService) {}

  ngOnInit() { this.cargar(); }

  cargar() {
    this.cargando.set(true);
    this.error.set('');
    this.servicio.listar().subscribe({
      next: (historial) => { this.historial.set(historial); this.cargando.set(false); },
      error: (error) => { this.error.set(obtenerMensajeError(error, 'No fue posible cargar tu historial.')); this.cargando.set(false); },
    });
  }

  contarAsistencias() { return this.historial().filter((item) => item.asistio).length; }
  contarConfirmadas() { return this.historial().filter((item) => item.estadoInscripcion === 'CONFIRMADA').length; }
}
