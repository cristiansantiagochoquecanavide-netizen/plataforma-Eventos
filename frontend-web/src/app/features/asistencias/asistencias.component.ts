import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AsistenciasService } from './servicios/asistencias.service';
import { Asistencia, InscripcionElegible } from './modelos/asistencia.modelo';
import { obtenerMensajeError } from '../../core/mensajes-error';

@Component({
  selector: 'app-asistencias',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './asistencias.component.html',
  styleUrl: './asistencias.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AsistenciasComponent implements OnInit {
  readonly inscripciones = signal<InscripcionElegible[]>([]);
  readonly seleccionada = signal<number | null>(null);
  readonly inscripcionSeleccionada = computed(() =>
    this.inscripciones().find((item) => item.idInscripcion === this.seleccionada()) ?? null,
  );
  readonly asistencia = signal<Asistencia | null>(null);
  readonly cargando = signal(false);
  readonly enviando = signal(false);
  readonly error = signal('');
  readonly tieneAsistenciasRegistradas = () => this.inscripciones().some((item) => item.asistenciaRegistrada);

  constructor(private readonly servicio: AsistenciasService) {}

  ngOnInit(): void {
    this.cargando.set(true);
    this.servicio.listarInscripciones().subscribe({
      next: (items) => { this.inscripciones.set(items); this.cargando.set(false); },
      error: (err) => { this.error.set(this.mensaje(err)); this.cargando.set(false); },
    });
  }

  seleccionar(id: number): void { this.seleccionada.set(id); this.asistencia.set(null); this.error.set(''); }

  registrar(): void {
    const id = this.seleccionada();
    if (id === null || this.enviando()) return;
    this.enviando.set(true); this.error.set(''); this.asistencia.set(null);
    this.servicio.registrar(id).subscribe({
      next: (response) => {
        this.asistencia.set(response);
        this.inscripciones.update((items) => items.map((item) => item.idInscripcion === response.idInscripcion
          ? { ...item, asistenciaRegistrada: true, fechaAsistencia: response.fechaHora }
          : item));
        this.enviando.set(false);
      },
      error: (err) => { this.error.set(this.mensaje(err)); this.enviando.set(false); },
    });
  }

  private mensaje(error: unknown): string {
    const status = typeof error === 'object' && error !== null && 'status' in error ? Number(error['status']) : 0;
    switch (status) {
      case 400: return 'No se puede registrar la asistencia.';
      case 401: return 'Tu sesión expiró. Inicia sesión nuevamente.';
      case 403: return 'No tienes permiso para registrar asistencias o consultar inscripciones.';
      case 404: return 'No se puede registrar la asistencia.';
      case 409: return 'Esta asistencia ya fue registrada.';
      case 500: return 'No fue posible registrar la asistencia. Intenta nuevamente.';
      default: return obtenerMensajeError(error, 'No fue posible completar la operación.');
    }
  }
}
