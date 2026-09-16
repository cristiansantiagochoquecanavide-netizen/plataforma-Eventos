import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { AsignacionStaff } from './modelos/asignacion-staff.modelo';
import { AsignacionesStaffService } from './servicios/asignaciones-staff.service';

@Component({ selector: 'app-asignaciones', imports: [FormsModule, DatePipe], template: `
  <section class="page-heading"><p class="eyebrow">Staff y organización</p><h1>Asignaciones</h1><p>Organiza turnos de staff y consulta tus tareas.</p></section>
  @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
  @if (sesion.puedeGestionarAsignacionesStaff()) { <section class="card form-grid"><label>Id de postulación *<input type="number" min="1" [(ngModel)]="form.idPostulacion" /></label><label>Tarea *<input [(ngModel)]="form.tarea" maxlength="255" /></label><label>Inicio<input type="datetime-local" [(ngModel)]="form.turnoInicio" /></label><label>Fin<input type="datetime-local" [(ngModel)]="form.turnoFin" /></label><label class="full">Observaciones<textarea [(ngModel)]="form.observaciones" maxlength="1000"></textarea></label><div class="button-row full"><button class="primary" type="button" (click)="crear()" [disabled]="guardando()">{{ guardando() ? 'Guardando...' : 'Crear asignación' }}</button><button type="button" (click)="cargarGestion()">Actualizar gestión</button></div></section> }
  <section class="card table-card" style="margin-top:22px"><div class="card-title compact"><h2>{{ sesion.puedeGestionarAsignacionesStaff() ? 'Asignaciones de la organización' : 'Mis asignaciones' }}</h2><span>{{ asignaciones().length }} registros</span></div><div class="table-wrap"><table><thead><tr><th>Postulación</th><th>Usuario</th><th>Tarea</th><th>Turno</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>@for (a of asignaciones(); track a.id) {<tr><td>#{{ a.idPostulacion }}</td><td>#{{ a.idUsuario }}</td><td>{{ a.tarea }}</td><td>{{ a.turnoInicio | date:'short' }}<br>{{ a.turnoFin | date:'short' }}</td><td><span class="status" [class.off]="a.estado === 'CANCELADA'">{{ a.estado }}</span></td><td>@if (sesion.puedeGestionarAsignacionesStaff() && a.estado === 'ASIGNADA') {<button type="button" (click)="cancelar(a)">Cancelar</button>}</td></tr>} @empty {<tr><td colspan="6" class="empty-state">No hay asignaciones.</td></tr>}</tbody></table></div></section>
`, changeDetection: ChangeDetectionStrategy.OnPush })
export class AsignacionesComponent implements OnInit {
  readonly asignaciones = signal<AsignacionStaff[]>([]); readonly error = signal(''); readonly guardando = signal(false);
  form = { idPostulacion: 0, tarea: '', turnoInicio: '', turnoFin: '', observaciones: '' };
  constructor(readonly sesion: SesionService, private readonly service: AsignacionesStaffService, private readonly toast: ToastService) {}
  ngOnInit() { this.cargarGestion(); }
  cargarGestion() { const llamada = this.sesion.puedeGestionarAsignacionesStaff() ? this.service.listar() : this.service.listarMias(); llamada.subscribe({ next: (data) => this.asignaciones.set(data), error: (e) => this.fallar(e) }); }
  crear() { if (!this.form.idPostulacion || !this.form.tarea.trim() || this.guardando()) return; this.guardando.set(true); this.service.crear({ ...this.form, tarea: this.form.tarea.trim(), turnoInicio: this.form.turnoInicio ? `${this.form.turnoInicio}:00Z` : null, turnoFin: this.form.turnoFin ? `${this.form.turnoFin}:00Z` : null }).subscribe({ next: () => { this.toast.exito('Asignación creada.'); this.cargarGestion(); }, error: (e) => this.fallar(e), complete: () => this.guardando.set(false) }); }
  cancelar(a: AsignacionStaff) { if (!confirm(`¿Cancelar la asignación de ${a.tarea}?`)) return; this.service.cancelar(a.id).subscribe({ next: () => { this.toast.exito('Asignación cancelada.'); this.cargarGestion(); }, error: (e) => this.fallar(e) }); }
  private fallar(e: unknown) { const mensaje = obtenerMensajeError(e, 'No fue posible cargar las asignaciones.'); this.error.set(mensaje); this.toast.error(mensaje); }
}
