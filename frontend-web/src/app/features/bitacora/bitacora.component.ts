import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { Bitacora } from './modelos/bitacora.modelo';
import { BitacoraService } from './servicios/bitacora.service';

@Component({ selector: 'app-bitacora', imports: [FormsModule, DatePipe], template: `
  <section class="page-heading"><p class="eyebrow">Seguridad y auditoría</p><h1>Bitácora</h1><p>Registro inmutable de actividad de la organización activa.</p></section>
  @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
  <form class="card form-grid" (ngSubmit)="buscar()"><label>Módulo<input name="modulo" [(ngModel)]="filtros.modulo" placeholder="EVENTOS" /></label><label>Entidad<input name="entidad" [(ngModel)]="filtros.entidad" placeholder="EVENTO" /></label><label>Acción<input name="accion" [(ngModel)]="filtros.accion" placeholder="CREAR" /></label><label>Resultado<select name="exito" [(ngModel)]="filtros.exito"><option [ngValue]="undefined">Todos</option><option [ngValue]="true">Exitosas</option><option [ngValue]="false">Fallidas</option></select></label><div class="button-row full"><button class="primary" type="submit">Aplicar filtros</button><button type="button" (click)="limpiar()">Limpiar</button></div></form>
  <section class="card table-card" style="margin-top:22px"><div class="card-title compact"><h2>Actividad reciente</h2><span>{{ total() }} eventos</span></div><div class="table-wrap"><table><thead><tr><th>Fecha</th><th>Actor</th><th>Acción</th><th>Módulo / entidad</th><th>Resultado</th><th>IP</th></tr></thead><tbody>@for (item of registros(); track item.id) {<tr><td>{{ item.fecha | date:'medium' }}</td><td>#{{ item.actor ?? 'sistema' }}</td><td>{{ item.accion }}</td><td><strong>{{ item.modulo }}</strong>{{ item.entidad }} #{{ item.idEntidad ?? '-' }}</td><td><span class="status" [class.off]="!item.exito">{{ item.exito ? 'Éxito' : 'Falló' }}</span><br><small>{{ item.resultado }}</small></td><td>{{ item.ip || '-' }}</td></tr>} @empty {<tr><td colspan="6" class="empty-state">No hay eventos.</td></tr>}</tbody></table></div><div class="button-row" style="padding:16px 24px;justify-content:flex-end"><button type="button" (click)="pagina(-1)" [disabled]="paginaActual() === 0">Anterior</button><small>Página {{ paginaActual() + 1 }} de {{ totalPaginas() || 1 }}</small><button type="button" (click)="pagina(1)" [disabled]="paginaActual() + 1 >= totalPaginas()">Siguiente</button></div></section>
`, changeDetection: ChangeDetectionStrategy.OnPush })
export class BitacoraComponent implements OnInit {
  readonly registros = signal<Bitacora[]>([]); readonly total = signal(0); readonly totalPaginas = signal(0); readonly paginaActual = signal(0); readonly error = signal('');
  filtros: { modulo: string; entidad: string; accion: string; exito: boolean | undefined } = { modulo: '', entidad: '', accion: '', exito: undefined };
  constructor(readonly sesion: SesionService, private readonly service: BitacoraService, private readonly toast: ToastService) {}
  ngOnInit() { this.buscar(); }
  buscar() { this.paginaActual.set(0); this.cargar(); }
  limpiar() { this.filtros = { modulo: '', entidad: '', accion: '', exito: undefined }; this.buscar(); }
  pagina(delta: number) { this.paginaActual.update((page) => page + delta); this.cargar(); }
  private cargar() { this.service.listar({ ...this.filtros, page: this.paginaActual(), size: 25 }).subscribe({ next: (page) => { this.registros.set(page.content); this.total.set(page.totalElements); this.totalPaginas.set(page.totalPages); }, error: (e) => { const mensaje = obtenerMensajeError(e, 'No fue posible cargar la bitácora.'); this.error.set(mensaje); this.toast.error(mensaje); } }); }
}
