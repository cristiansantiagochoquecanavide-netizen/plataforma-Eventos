import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Permiso, Rol } from '../core/modelos';

@Component({
  selector: 'app-roles',
  imports: [ReactiveFormsModule],
  templateUrl: './roles.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RolesComponent implements OnInit {
  readonly roles = signal<Rol[]>([]);
  readonly permisos = signal<Permiso[]>([]);
  readonly rolSeleccionado = signal<Rol | null>(null);
  readonly permisosSeleccionados = signal<Set<string>>(new Set());
  readonly editando = signal<Rol | null>(null);
  readonly mensaje = signal('');
  readonly error = signal('');
  readonly formulario = new FormBuilder().nonNullable.group({
    nombre: ['', [Validators.required, Validators.pattern(/^[A-Z][A-Z0-9_]{1,49}$/)]],
    descripcion: [''],
    estado: ['ACTIVO'],
  });

  constructor(private readonly api: ApiService) {}

  ngOnInit() {
    this.cargar();
    this.api.listarPermisos().subscribe({ next: (permisos) => this.permisos.set(permisos) });
  }

  cargar() {
    this.api
      .listarRoles()
      .subscribe({
        next: (roles) => this.roles.set(roles),
        error: (e) => this.error.set(e.error?.message ?? 'No fue posible cargar roles'),
      });
  }

  editar(rol: Rol) {
    this.editando.set(rol);
    this.formulario.patchValue({ ...rol, descripcion: rol.descripcion ?? '' });
  }

  cancelar() {
    this.editando.set(null);
    this.formulario.reset({ nombre: '', descripcion: '', estado: 'ACTIVO' });
  }

  guardarRol() {
    if (this.formulario.invalid) return;
    const datos = this.formulario.getRawValue();
    const editando = this.editando();
    const solicitud = editando
      ? this.api.actualizarRol(editando.id, datos)
      : this.api.crearRol(datos);
    solicitud.subscribe({
      next: () => {
        this.mensaje.set(editando ? 'Rol actualizado.' : 'Rol creado.');
        this.cancelar();
        this.cargar();
      },
      error: (e) => this.error.set(e.error?.message ?? 'No fue posible guardar el rol'),
    });
  }

  seleccionarRol(rol: Rol) {
    this.rolSeleccionado.set(rol);
    this.api.permisosRol(rol.id).subscribe({
      next: (respuesta) =>
        this.permisosSeleccionados.set(
          new Set(respuesta.permisos.map((permiso) => permiso.nombre)),
        ),
      error: (e) => this.error.set(e.error?.message ?? 'No fue posible consultar los permisos'),
    });
  }

  alternarPermiso(nombre: string, marcado: boolean) {
    const seleccion = new Set(this.permisosSeleccionados());
    marcado ? seleccion.add(nombre) : seleccion.delete(nombre);
    this.permisosSeleccionados.set(seleccion);
  }

  guardarPermisos() {
    const rol = this.rolSeleccionado();
    if (!rol) return;
    this.api.asignarPermisos(rol.id, [...this.permisosSeleccionados()]).subscribe({
      next: () => this.mensaje.set(`Permisos de ${rol.nombre} actualizados.`),
      error: (e) => this.error.set(e.error?.message ?? 'No fue posible asignar permisos'),
    });
  }
}
