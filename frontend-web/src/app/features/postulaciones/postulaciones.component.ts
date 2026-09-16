import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { PostulacionStaff } from './modelos/postulacion-staff.modelo';
import { PostulacionesStaffService } from './servicios/postulaciones-staff.service';

@Component({ selector: 'app-postulaciones', imports: [FormsModule, DatePipe], template: `
  <section class="page-heading"><p class="eyebrow">Staff y organización</p><h1>Postulaciones</h1><p>Consulta tus postulaciones o revisa las de una convocatoria.</p></section>
  @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
  <section class="admin-grid">
    <div class="card"><div class="card-header"><h2>Aplicar a convocatoria</h2><span>POSTULACION_STAFF_CREAR</span></div>
      @if (sesion.puedeCrearPostulacionesStaff()) { <label>Id de convocatoria <input type="number" min="1" [(ngModel)]="idConvocatoria" /></label><label>Motivación <textarea maxlength="1000" [(ngModel)]="motivacion" placeholder="Cuéntanos tu experiencia"></textarea></label><button class="primary" type="button" (click)="aplicar()" [disabled]="guardando()">{{ guardando() ? 'Enviando...' : 'Enviar postulación' }}</button> }
      @else { <p class="empty-inline">No tienes permiso para crear postulaciones.</p> }
    </div>
    <div class="card"><div class="card-header"><h2>Gestión por convocatoria</h2><span>GESTIONAR</span></div>
      @if (sesion.puedeGestionarPostulacionesStaff()) { <label>Id de convocatoria <input type="number" min="1" [(ngModel)]="idGestion" /></label><button class="secondary" type="button" (click)="cargarGestion()">Consultar</button> }
      @else { <p class="empty-inline">La gestión está disponible para usuarios autorizados.</p> }
    </div>
  </section>
  <section class="card table-card" style="margin-top:22px"><div class="card-title compact"><h2>{{ gestionActiva() ? 'Postulaciones de la convocatoria' : 'Mis postulaciones' }}</h2><span>{{ postulaciones().length }} registros</span></div><div class="table-wrap"><table><thead><tr><th>Convocatoria</th><th>Usuario</th><th>Motivación</th><th>Estado</th><th>Fecha</th><th>Acciones</th></tr></thead><tbody>@for (p of postulaciones(); track p.id) {<tr><td>#{{ p.idConvocatoria }}</td><td>#{{ p.idUsuario }}</td><td>{{ p.motivacion || 'Sin motivación' }}</td><td><span class="status" [class.off]="p.estado !== 'ACEPTADA'">{{ p.estado }}</span></td><td>{{ p.fechaPostulacion | date:'short' }}</td><td class="actions">@if (gestionActiva() && p.estado === 'PENDIENTE') {<button type="button" (click)="cambiar(p, true)">Aceptar</button><button type="button" (click)="cambiar(p, false)">Rechazar</button>}</td></tr>} @empty {<tr><td colspan="6" class="empty-state">No hay postulaciones para mostrar.</td></tr>}</tbody></table></div></section>
`, changeDetection: ChangeDetectionStrategy.OnPush })
export class PostulacionesComponent implements OnInit {
  readonly postulaciones = signal<PostulacionStaff[]>([]); readonly error = signal(''); readonly guardando = signal(false); readonly gestionActiva = signal(false);
  idConvocatoria = 0; idGestion = 0; motivacion = '';
  constructor(readonly sesion: SesionService, private readonly service: PostulacionesStaffService, private readonly toast: ToastService) {}
  ngOnInit() { this.cargarMias(); }
  cargarMias() { this.gestionActiva.set(false); this.service.listarMias().subscribe({ next: (data) => this.postulaciones.set(data), error: (e) => this.fallar(e) }); }
  cargarGestion() { if (!this.idGestion) return; this.service.listarPorConvocatoria(this.idGestion).subscribe({ next: (data) => { this.gestionActiva.set(true); this.postulaciones.set(data); }, error: (e) => this.fallar(e) }); }
  aplicar() { if (!this.idConvocatoria || this.guardando()) return; this.guardando.set(true); this.service.crear(this.idConvocatoria, { motivacion: this.motivacion.trim() }).subscribe({ next: () => { this.toast.exito('Postulación enviada.'); this.motivacion = ''; this.cargarMias(); }, error: (e) => this.fallar(e), complete: () => this.guardando.set(false) }); }
  cambiar(p: PostulacionStaff, aceptar: boolean) { const llamada = aceptar ? this.service.aceptar(p.id) : this.service.rechazar(p.id); llamada.subscribe({ next: () => { this.toast.exito(aceptar ? 'Postulación aceptada.' : 'Postulación rechazada.'); this.cargarGestion(); }, error: (e) => this.fallar(e) }); }
  private fallar(e: unknown) { const mensaje = obtenerMensajeError(e, 'No fue posible cargar las postulaciones.'); this.error.set(mensaje); this.toast.error(mensaje); }
}
