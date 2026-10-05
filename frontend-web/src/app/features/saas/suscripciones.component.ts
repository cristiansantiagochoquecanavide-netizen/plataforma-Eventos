import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../environments/environment';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { ToastService } from '../../core/toast.service';

type PlanSuscripcion = 'BASICO' | 'PROFESIONAL' | 'EMPRESARIAL';
type EstadoSuscripcion = 'ACTIVA' | 'SUSPENDIDA' | 'VENCIDA';

interface SuscripcionOrganizacion {
  idOrganizacion: number;
  nombreOrganizacion: string;
  slugOrganizacion: string;
  plan: PlanSuscripcion;
  estado: EstadoSuscripcion;
  fechaInicio: string;
  fechaFin: string | null;
  vigente: boolean;
}

interface BorradorSuscripcion {
  plan: PlanSuscripcion;
  estado: EstadoSuscripcion;
  fechaInicio: string;
  fechaFin: string;
}

@Component({
  selector: 'app-suscripciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page-heading">
      <p class="eyebrow">Administración SaaS</p>
      <h1>Suscripciones por organización</h1>
      <p>Administra manualmente el plan y vigencia. Esta vista no procesa cobros.</p>
    </section>
    @if (cargando()) { <p role="status">Cargando suscripciones…</p> }
    @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
    <section class="saas-list" aria-label="Suscripciones de organizaciones">
      @for (suscripcion of suscripciones(); track suscripcion.idOrganizacion) {
        <article class="card saas-card">
          <header>
            <div><h2>{{ suscripcion.nombreOrganizacion }}</h2><small>{{ suscripcion.slugOrganizacion }}</small></div>
            <span class="status" [class.off]="!suscripcion.vigente">{{ suscripcion.vigente ? 'Vigente' : 'Sin acceso' }}</span>
          </header>
          @if (borradores()[suscripcion.idOrganizacion]; as borrador) {
            <div class="saas-fields">
              <label>Plan
                <select [ngModel]="borrador.plan" (ngModelChange)="cambiar(suscripcion.idOrganizacion, 'plan', $event)">
                  @for (plan of planes; track plan) { <option [ngValue]="plan">{{ plan }}</option> }
                </select>
              </label>
              <label>Estado
                <select [ngModel]="borrador.estado" (ngModelChange)="cambiar(suscripcion.idOrganizacion, 'estado', $event)">
                  @for (estado of estados; track estado) { <option [ngValue]="estado">{{ estado }}</option> }
                </select>
              </label>
              <label>Inicio
                <input type="date" [ngModel]="borrador.fechaInicio" (ngModelChange)="cambiar(suscripcion.idOrganizacion, 'fechaInicio', $event)" />
              </label>
              <label>Fin (opcional)
                <input type="date" [ngModel]="borrador.fechaFin" (ngModelChange)="cambiar(suscripcion.idOrganizacion, 'fechaFin', $event)" />
              </label>
              <button type="button" class="primary" (click)="guardar(suscripcion.idOrganizacion)" [disabled]="guardandoId() === suscripcion.idOrganizacion || !borrador.fechaInicio">
                {{ guardandoId() === suscripcion.idOrganizacion ? 'Guardando…' : 'Guardar' }}
              </button>
            </div>
          }
        </article>
      }
    </section>
  `,
  styles: [`
    .saas-list{display:grid;gap:14px}
    .saas-card{padding:0;overflow:hidden}
    .saas-card header{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:18px 22px;border-bottom:1px solid var(--line)}
    .saas-card h2{margin:0;font-size:1rem}
    .saas-card small{color:var(--muted)}
    .saas-fields{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:14px;padding:18px 22px;align-items:end}
  `],
})
export class SuscripcionesComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly url = `${environment.apiUrl}/saas/suscripciones`;
  readonly planes: PlanSuscripcion[] = ['BASICO', 'PROFESIONAL', 'EMPRESARIAL'];
  readonly estados: EstadoSuscripcion[] = ['ACTIVA', 'SUSPENDIDA', 'VENCIDA'];
  readonly suscripciones = signal<SuscripcionOrganizacion[]>([]);
  readonly borradores = signal<Record<number, BorradorSuscripcion>>({});
  readonly cargando = signal(false);
  readonly guardandoId = signal<number | null>(null);
  readonly error = signal('');

  ngOnInit(): void { this.cargar(); }

  private cargar(): void {
    this.cargando.set(true);
    this.http.get<SuscripcionOrganizacion[]>(this.url).subscribe({
      next: (respuesta) => {
        this.suscripciones.set(respuesta);
        this.borradores.set(Object.fromEntries(respuesta.map((item) => [item.idOrganizacion, {
          plan: item.plan,
          estado: item.estado,
          fechaInicio: item.fechaInicio.slice(0, 10),
          fechaFin: item.fechaFin?.slice(0, 10) ?? '',
        }])));
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(obtenerMensajeError(err, 'No fue posible cargar las suscripciones.'));
        this.cargando.set(false);
      },
    });
  }

  cambiar(id: number, campo: keyof BorradorSuscripcion, valor: string): void {
    this.borradores.update((actuales) => ({
      ...actuales,
      [id]: { ...actuales[id], [campo]: valor },
    }));
  }

  guardar(id: number): void {
    const borrador = this.borradores()[id];
    if (!borrador?.fechaInicio || (borrador.fechaFin && borrador.fechaFin <= borrador.fechaInicio)) {
      this.toast.error('La fecha de fin debe ser posterior a la fecha de inicio.');
      return;
    }
    this.guardandoId.set(id);
    this.http.put<SuscripcionOrganizacion>(`${this.url}/${id}`, {
      plan: borrador.plan,
      estado: borrador.estado,
      fechaInicio: new Date(`${borrador.fechaInicio}T00:00:00.000Z`).toISOString(),
      fechaFin: borrador.fechaFin ? new Date(`${borrador.fechaFin}T23:59:59.999Z`).toISOString() : null,
    }).subscribe({
      next: (actualizada) => {
        this.suscripciones.update((actuales) => actuales.map((actual) =>
          actual.idOrganizacion === id ? actualizada : actual));
        this.toast.exito('Suscripción actualizada.');
        this.guardandoId.set(null);
      },
      error: (err) => {
        this.toast.error(obtenerMensajeError(err, 'No fue posible actualizar la suscripción.'));
        this.guardandoId.set(null);
      },
    });
  }
}
