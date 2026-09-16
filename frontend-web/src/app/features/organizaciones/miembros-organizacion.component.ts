import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { Usuario } from '../usuarios/modelos/usuario.modelo';
import { UsuariosService } from '../usuarios/servicios/usuarios.service';
import { Membresia } from './modelos/organizacion.modelo';
import { OrganizacionesService } from './servicios/organizaciones.service';

@Component({ selector: 'app-miembros-organizacion', imports: [FormsModule], template: `
  <section class="page-heading"><p class="eyebrow">Organización activa</p><h1>Miembros</h1><p>Administra el acceso de usuarios a esta organización.</p></section>
  @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
  <section class="card form-grid"><label>Usuario <select [(ngModel)]="idUsuario"><option [ngValue]="0">Selecciona un usuario</option>@for (u of usuarios(); track u.id) {<option [ngValue]="u.id">{{ u.nombres }} {{ u.apellidos }} · {{ u.correo }}</option>}</select></label><label>Roles <input [(ngModel)]="roles" placeholder="STAFF, PARTICIPANTE" /></label><div class="button-row full"><button class="primary" type="button" (click)="agregar()" [disabled]="!idUsuario">Agregar miembro</button><button type="button" (click)="cargar()">Actualizar</button></div></section>
  <section class="card table-card" style="margin-top:22px"><div class="card-title compact"><h2>Directorio de la organización</h2><span>{{ miembros().length }} miembros</span></div><div class="table-wrap"><table><thead><tr><th>Miembro</th><th>Correo</th><th>Roles</th><th>Estado</th><th>Acción</th></tr></thead><tbody>@for (m of miembros(); track m.idUsuario) {<tr><td><strong>{{ m.nombres }} {{ m.apellidos }}</strong></td><td>{{ m.correo }}</td><td>{{ m.roles.join(', ') }}</td><td><span class="status" [class.off]="!m.activo">{{ m.activo ? 'ACTIVO' : 'INACTIVO' }}</span></td><td><button type="button" (click)="alternar(m)">{{ m.activo ? 'Inactivar' : 'Activar' }}</button></td></tr>} @empty {<tr><td colspan="5" class="empty-state">No hay miembros.</td></tr>}</tbody></table></div></section>
`, changeDetection: ChangeDetectionStrategy.OnPush })
export class MiembrosOrganizacionComponent implements OnInit {
  readonly miembros = signal<Membresia[]>([]); readonly usuarios = signal<Usuario[]>([]); readonly error = signal(''); idUsuario = 0; roles = 'PARTICIPANTE';
  constructor(readonly sesion: SesionService, private readonly service: OrganizacionesService, private readonly usuariosService: UsuariosService, private readonly toast: ToastService) {}
  ngOnInit() { this.cargar(); this.usuariosService.listarUsuarios().subscribe({ next: (page) => this.usuarios.set(page.elementos), error: (e) => this.fallar(e) }); }
  cargar() { this.service.listarMiembros().subscribe({ next: (data) => this.miembros.set(data), error: (e) => this.fallar(e) }); }
  agregar() { const id = this.sesion.organizacionActivaId(); if (!id || !this.idUsuario) return; this.service.agregarMembresia(id, { idUsuario: this.idUsuario, roles: this.roles.split(',').map((r) => r.trim().toUpperCase()).filter(Boolean) }).subscribe({ next: () => { this.toast.exito('Miembro agregado.'); this.cargar(); }, error: (e) => this.fallar(e) }); }
  alternar(m: Membresia) { const id = this.sesion.organizacionActivaId(); if (!id) return; this.service.cambiarEstadoMembresia(id, m.idUsuario, !m.activo).subscribe({ next: () => { this.toast.exito('Estado actualizado.'); this.cargar(); }, error: (e) => this.fallar(e) }); }
  private fallar(e: unknown) { const mensaje = obtenerMensajeError(e, 'No fue posible cargar los miembros.'); this.error.set(mensaje); this.toast.error(mensaje); }
}
