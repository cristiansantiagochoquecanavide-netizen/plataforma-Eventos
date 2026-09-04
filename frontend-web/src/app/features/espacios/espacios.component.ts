import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { Espacio, GuardarEspacioSolicitud } from '../../core/modelos';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-espacios',
  imports: [ReactiveFormsModule],
  templateUrl: './espacios.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EspaciosComponent implements OnInit {
  readonly espacios = signal<Espacio[]>([]);
  readonly seleccionado = signal<Espacio | null>(null);
  readonly modalAbierto = signal(false);
  readonly error = signal('');
  readonly guardando = signal(false);
  readonly formulario = new FormBuilder().nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    ubicacion: ['', [Validators.required, Validators.maxLength(255)]],
    capacidad: [1, [Validators.required, Validators.min(1)]],
  });

  constructor(private readonly api: ApiService, private readonly toast: ToastService) {}

  ngOnInit() { this.cargar(); }

  cargar() {
    this.api.listarEspacios().subscribe({
      next: (espacios) => this.espacios.set(espacios),
      error: (error) => this.mostrarError(error, 'No fue posible cargar los espacios'),
    });
  }

  nuevo() {
    this.seleccionado.set(null);
    this.formulario.reset({ nombre: '', ubicacion: '', capacidad: 1 });
    this.error.set('');
    this.modalAbierto.set(true);
  }

  editar(espacio: Espacio) {
    this.seleccionado.set(espacio);
    this.formulario.reset({ nombre: espacio.nombre, ubicacion: espacio.ubicacion, capacidad: espacio.capacidad });
    this.error.set('');
    this.modalAbierto.set(true);
  }

  cerrar() { this.modalAbierto.set(false); }

  guardar() {
    if (this.formulario.invalid || this.guardando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    const datos: GuardarEspacioSolicitud = this.formulario.getRawValue();
    const espacio = this.seleccionado();
    const solicitud = espacio ? this.api.actualizarEspacio(espacio.id, datos) : this.api.crearEspacio(datos);
    solicitud.subscribe({
      next: () => {
        this.toast.exito(espacio ? 'Espacio actualizado correctamente.' : 'Espacio registrado correctamente.');
        this.cerrar();
        this.cargar();
      },
      error: (error) => this.mostrarError(error, 'No fue posible guardar el espacio'),
      complete: () => this.guardando.set(false),
    });
  }

  private mostrarError(error: unknown, alternativo: string) {
    const mensaje = obtenerMensajeError(error, alternativo);
    this.error.set(mensaje);
    this.toast.error(mensaje);
    this.guardando.set(false);
  }
}
