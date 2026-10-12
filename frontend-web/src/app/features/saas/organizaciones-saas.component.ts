import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { EstadoOrganizacion, OrganizacionSaas, PaginaOrganizacionesSaas } from './modelos/organizacion-saas.modelo';
import { OrganizacionesSaasService } from './servicios/organizaciones-saas.service';

@Component({
  selector: 'app-organizaciones-saas',
  imports: [CommonModule, DatePipe, FormsModule],
  templateUrl: './organizaciones-saas.component.html',
  styleUrl: './organizaciones-saas.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganizacionesSaasComponent implements OnInit {
  readonly pagina = signal<PaginaOrganizacionesSaas | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly busqueda = signal('');
  readonly estado = signal<'TODAS' | EstadoOrganizacion>('TODAS');
  readonly paginaActual = signal(0);
  readonly organizacionPendiente = signal<OrganizacionSaas | null>(null);
  readonly actualizandoId = signal<number | null>(null);
  readonly principalSlug = 'principal';

  constructor(
    private readonly service: OrganizacionesSaasService,
    private readonly sesion: SesionService,
    private readonly router: Router,
    private readonly toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(pagina = this.paginaActual()): void {
    this.cargando.set(true);
    this.error.set('');
    const estado = this.estado();
    this.service.listar({
      pagina,
      tamano: 20,
      busqueda: this.busqueda(),
      estado: estado === 'TODAS' ? null : estado,
    }).subscribe({
      next: (respuesta) => {
        this.pagina.set(respuesta);
        this.paginaActual.set(respuesta.pagina);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(obtenerMensajeError(err, 'No fue posible cargar las organizaciones.'));
        this.cargando.set(false);
      },
    });
  }

  buscar(): void {
    this.cargar(0);
  }

  cambiarEstadoFiltro(estado: 'TODAS' | EstadoOrganizacion): void {
    this.estado.set(estado);
    this.cargar(0);
  }

  cambiarPagina(delta: number): void {
    const destino = this.paginaActual() + delta;
    if (destino < 0 || destino >= (this.pagina()?.totalPaginas ?? 0)) return;
    this.cargar(destino);
  }

  confirmarCambioEstado(organizacion: OrganizacionSaas): void {
    this.organizacionPendiente.set(organizacion);
  }

  cancelarCambioEstado(): void {
    this.organizacionPendiente.set(null);
  }

  guardarCambioEstado(): void {
    const organizacion = this.organizacionPendiente();
    if (!organizacion || this.actualizandoId() !== null) return;
    const estado: EstadoOrganizacion = organizacion.estado === 'ACTIVA' ? 'INACTIVA' : 'ACTIVA';
    this.actualizandoId.set(organizacion.id);
    this.service.actualizarEstado(organizacion.id, estado).subscribe({
      next: () => {
        this.organizacionPendiente.set(null);
        this.actualizandoId.set(null);
        this.toast.exito(estado === 'ACTIVA' ? 'La organización quedó activa.' : 'La organización quedó suspendida.');
        this.cargar();
      },
      error: (err) => {
        this.actualizandoId.set(null);
        this.error.set(obtenerMensajeError(err, 'No se pudo actualizar el estado de la organización.'));
      },
    });
  }

  abrirOrganizacion(organizacion: OrganizacionSaas): void {
    if (organizacion.estado !== 'ACTIVA') return;
    this.sesion.seleccionarOrganizacion(organizacion.id).subscribe({
      next: () => this.router.navigateByUrl('/perfil'),
      error: (err) => this.error.set(obtenerMensajeError(err, 'No se pudo abrir la organización.')),
    });
  }

  trackOrganizacion(_index: number, organizacion: OrganizacionSaas): number {
    return organizacion.id;
  }

  get totalPaginas(): number {
    return this.pagina()?.totalPaginas ?? 0;
  }

  get resumenActivasPagina(): number {
    return this.pagina()?.organizaciones.filter((organizacion) => organizacion.estado === 'ACTIVA').length ?? 0;
  }

  get resumenAtencionPagina(): number {
    return this.pagina()?.organizaciones.filter((organizacion) => !organizacion.suscripcionVigente).length ?? 0;
  }
}
