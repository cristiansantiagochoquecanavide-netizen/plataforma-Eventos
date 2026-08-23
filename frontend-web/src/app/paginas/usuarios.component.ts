import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Rol, Usuario } from '../core/modelos';

@Component({
  selector: 'app-usuarios',
  imports: [ReactiveFormsModule],
  templateUrl: './usuarios.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsuariosComponent implements OnInit {
  readonly usuarios = signal<Usuario[]>([]);
  readonly roles = signal<Rol[]>([]);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly editando = signal<Usuario | null>(null);
  readonly formulario = new FormBuilder().nonNullable.group({
    nombres: ['', Validators.required],
    apellidos: ['', Validators.required],
    correo: ['', [Validators.required, Validators.email]],
    clave: ['', [Validators.required, Validators.minLength(8)]],
    roles: ['PARTICIPANTE', Validators.required],
  });

  constructor(private readonly api: ApiService) {}

  ngOnInit() {
    this.cargar();
    this.api.listarRoles().subscribe({ next: (roles) => this.roles.set(roles) });
  }

  cargar() {
    this.api.listarUsuarios().subscribe({
      next: (pagina) => this.usuarios.set(pagina.elementos),
      error: (error) => this.error.set(error.error?.message ?? 'No fue posible cargar usuarios'),
    });
  }

  editar(usuario: Usuario) {
    this.editando.set(usuario);
    this.formulario.patchValue({ ...usuario, clave: '', roles: usuario.roles.join(', ') });
    this.formulario.controls.clave.clearValidators();
    this.formulario.controls.clave.updateValueAndValidity();
  }

  cancelar() {
    this.editando.set(null);
    this.formulario.reset({
      nombres: '',
      apellidos: '',
      correo: '',
      clave: '',
      roles: 'PARTICIPANTE',
    });
    this.formulario.controls.clave.setValidators([Validators.required, Validators.minLength(8)]);
    this.formulario.controls.clave.updateValueAndValidity();
  }

  guardar() {
    if (this.formulario.invalid) return;
    this.limpiarAvisos();
    const valor = this.formulario.getRawValue();
    const roles = valor.roles
      .split(',')
      .map((rol) => rol.trim().toUpperCase())
      .filter(Boolean);
    const editando = this.editando();
    const solicitud = editando
      ? this.api.actualizarUsuario(editando.id, {
          nombres: valor.nombres,
          apellidos: valor.apellidos,
          correo: valor.correo,
          roles,
        })
      : this.api.crearUsuario({ ...valor, roles });
    solicitud.subscribe({
      next: () => {
        this.mensaje.set(editando ? 'Usuario actualizado.' : 'Usuario creado.');
        this.cancelar();
        this.cargar();
      },
      error: (error) => this.error.set(error.error?.message ?? 'No fue posible guardar el usuario'),
    });
  }

  alternarEstado(usuario: Usuario) {
    const estado = usuario.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.api.cambiarEstadoUsuario(usuario.id, estado).subscribe({
      next: () => {
        this.mensaje.set(`Usuario ${estado.toLowerCase()}.`);
        this.cargar();
      },
      error: (error) => this.error.set(error.error?.message ?? 'No fue posible cambiar el estado'),
    });
  }

  private limpiarAvisos() {
    this.error.set('');
    this.mensaje.set('');
  }
}
