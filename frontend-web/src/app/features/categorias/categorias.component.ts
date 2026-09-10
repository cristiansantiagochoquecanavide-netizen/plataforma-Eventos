import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoriasService } from './servicios/categorias.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { Categoria, GuardarCategoriaSolicitud } from './modelos/categoria.modelo';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-categorias',
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './categorias.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoriasComponent implements OnInit {
  readonly categorias = signal<Categoria[]>([]);
  readonly categoriaSeleccionada = signal<Categoria | null>(null);
  readonly modalAbierto = signal(false);
  readonly guardando = signal(false);
  readonly eliminandoId = signal<number | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly intentoEnvio = signal(false);

  readonly formulario = new FormBuilder().nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    descripcion: ['', Validators.maxLength(255)],
  });

  constructor(
    private readonly categoriasService: CategoriasService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit() {
    this.cargarCategorias();
  }

  cargarCategorias() {
    this.cargando.set(true);
    this.categoriasService.listarCategorias().subscribe({
      next: (categorias) => {
        this.categorias.set(categorias);
        this.cargando.set(false);
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible cargar categorias');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.cargando.set(false);
      },
    });
  }

  abrirModalCrear() {
    this.categoriaSeleccionada.set(null);
    this.intentoEnvio.set(false);
    this.limpiarAvisos();
    this.formulario.reset({
      nombre: '',
      descripcion: '',
    });
    this.modalAbierto.set(true);
  }

  editar(categoria: Categoria) {
    this.categoriaSeleccionada.set(categoria);
    this.intentoEnvio.set(false);
    this.limpiarAvisos();
    this.formulario.reset({
      nombre: categoria.nombre,
      descripcion: categoria.descripcion ?? '',
    });
    this.modalAbierto.set(true);
  }

  cerrarModal() {
    this.modalAbierto.set(false);
    this.categoriaSeleccionada.set(null);
    this.intentoEnvio.set(false);
    this.limpiarAvisos();
  }

  guardar() {
    this.intentoEnvio.set(true);
    if (this.guardando()) return;
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      const mensaje = this.mensajeFormularioInvalido();
      this.error.set(mensaje);
      this.toast.error(mensaje);
      return;
    }

    const valor = this.formulario.getRawValue();
    const datos: GuardarCategoriaSolicitud = {
      nombre: valor.nombre.trim(),
      descripcion: valor.descripcion.trim() || null,
    };

    this.guardando.set(true);
    this.limpiarAvisos();
    const categoria = this.categoriaSeleccionada();
    const solicitud = categoria
      ? this.categoriasService.actualizarCategoria(categoria.id, datos)
      : this.categoriasService.crearCategoria(datos);

    solicitud.subscribe({
      next: () => {
        const mensaje = categoria
          ? 'Categoria actualizada correctamente.'
          : 'Categoria creada correctamente.';
        this.cerrarModal();
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.cargarCategorias();
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible guardar la categoria');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.guardando.set(false);
      },
      complete: () => this.guardando.set(false),
    });
  }

  eliminar(categoria: Categoria) {
    if (!confirm(`Eliminar la categoria "${categoria.nombre}"?`)) return;

    this.eliminandoId.set(categoria.id);
    this.limpiarAvisos();
    this.categoriasService.eliminarCategoria(categoria.id).subscribe({
      next: () => {
        const mensaje = 'Categoria eliminada correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
        this.cargarCategorias();
      },
      error: (error) => {
        const mensaje = obtenerMensajeError(error, 'No fue posible eliminar la categoria');
        this.error.set(mensaje);
        this.toast.error(mensaje);
        this.eliminandoId.set(null);
      },
      complete: () => this.eliminandoId.set(null),
    });
  }

  campoConError(nombre: keyof typeof this.formulario.controls) {
    const control = this.formulario.controls[nombre];
    return control.invalid && (control.touched || this.intentoEnvio());
  }

  mensajeCampo(nombre: keyof typeof this.formulario.controls) {
    const control = this.formulario.controls[nombre];
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('maxlength')) return 'Supera la longitud permitida.';
    return '';
  }

  private mensajeFormularioInvalido() {
    const orden: Array<keyof typeof this.formulario.controls> = ['nombre', 'descripcion'];
    const campo = orden.find((nombre) => this.formulario.controls[nombre].invalid);
    return campo ? this.mensajeCampo(campo) : 'Revisa los datos de la categoria.';
  }

  private limpiarAvisos() {
    this.error.set('');
    this.mensaje.set('');
  }
}
