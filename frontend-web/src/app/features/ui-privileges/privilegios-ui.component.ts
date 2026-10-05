import { ChangeDetectionStrategy, Component, OnInit, computed, signal } from '@angular/core';
import { UiPrivilegeEditableDirective, UiPrivilegeEnabledDirective, UiPrivilegeVisibleDirective } from '../../core/ui-privilege.directive';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { ToastService } from '../../core/toast.service';
import { UI_PRIVILEGE_STATES, UiPrivilegeState } from '../../core/ui-privileges.model';
import { GrupoUsuario } from '../grupos/modelos/grupo-usuario.modelo';
import { GruposService } from '../grupos/servicios/grupos.service';
import { UiPrivilegeCatalogEntry, UiPrivilegeGroupGrant } from './modelos/ui-privilege-admin.model';
import { UiPrivilegeAdminService } from './servicios/ui-privilege-admin.service';

const ESTADOS_ADMINISTRABLES = new Set<string>(UI_PRIVILEGE_STATES);
const ORDEN_ESTADOS: UiPrivilegeState[] = ['VISIBLE', 'HABILITADO', 'EDITABLE'];

@Component({
  selector: 'app-privilegios-ui',
  imports: [UiPrivilegeVisibleDirective, UiPrivilegeEnabledDirective, UiPrivilegeEditableDirective],
  templateUrl: './privilegios-ui.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivilegiosUiComponent implements OnInit {
  readonly grupos = signal<GrupoUsuario[]>([]);
  readonly catalogo = signal<UiPrivilegeCatalogEntry[]>([]);
  readonly idGrupoSeleccionado = signal<number | null>(null);
  readonly concesiones = signal<ReadonlyMap<string, ReadonlySet<UiPrivilegeState>>>(new Map());
  readonly cargando = signal(false);
  readonly cargandoPrivilegios = signal(false);
  readonly guardando = signal(false);
  readonly mensaje = signal('');
  readonly error = signal('');
  readonly grupoSeleccionado = computed(() => {
    const id = this.idGrupoSeleccionado();
    return this.grupos().find((grupo) => grupo.id === id) ?? null;
  });

  readonly estados = ORDEN_ESTADOS;

  constructor(
    private readonly gruposService: GruposService,
    private readonly adminService: UiPrivilegeAdminService,
    private readonly toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.cargarInicial();
  }

  cargarInicial(): void {
    this.limpiarAvisos();
    this.cargando.set(true);

    this.gruposService.listarGrupos().subscribe({
      next: (grupos) => this.grupos.set(grupos),
      error: (error) => this.fallar(error, 'No fue posible cargar grupos'),
      complete: () => this.cargando.set(false),
    });

    this.adminService.listarCatalogo().subscribe({
      next: (catalogo) => this.catalogo.set(catalogo.map((entrada) => this.normalizarEntradaCatalogo(entrada))),
      error: (error) => this.fallar(error, 'No fue posible cargar el catálogo de componentes UI'),
    });
  }

  seleccionarGrupo(idGrupo: number | string): void {
    const id = Number(idGrupo);

    this.limpiarAvisos();
    this.concesiones.set(new Map());

    if (!Number.isFinite(id) || id <= 0) {
      this.idGrupoSeleccionado.set(null);
      this.cargandoPrivilegios.set(false);
      return;
    }

    this.idGrupoSeleccionado.set(id);
    this.cargandoPrivilegios.set(true);

    this.adminService.obtenerPrivilegiosGrupo(id).subscribe({
      next: (respuesta) => this.concesiones.set(this.concesionesDesdeRespuesta(respuesta.privilegios)),
      error: (error) => {
        this.fallar(error, 'No fue posible cargar los privilegios del grupo');
        this.cargandoPrivilegios.set(false);
      },
      complete: () => this.cargandoPrivilegios.set(false),
    });
  }

  estaConcedido(clave: string, estado: UiPrivilegeState): boolean {
    return this.concesiones().get(clave)?.has(estado) ?? false;
  }

  soportaEstado(entrada: UiPrivilegeCatalogEntry, estado: UiPrivilegeState): boolean {
    return entrada.estadosSoportados.includes(estado);
  }

  alternarEstado(clave: string, estado: UiPrivilegeState, concedido: boolean): void {
    const entrada = this.catalogo().find((item) => item.clave === clave);
    if (!entrada || !this.soportaEstado(entrada, estado)) return;

    const concesiones = new Map(this.concesiones());
    const estados = new Set(concesiones.get(clave) ?? []);

    if (concedido) {
      estados.add(estado);
      if ((estado === 'HABILITADO' || estado === 'EDITABLE') && this.soportaEstado(entrada, 'VISIBLE')) {
        estados.add('VISIBLE');
      }
    } else {
      estados.delete(estado);
      if (estado === 'VISIBLE') {
        estados.delete('HABILITADO');
        estados.delete('EDITABLE');
      }
    }

    if (estados.size > 0) {
      concesiones.set(clave, estados);
    } else {
      concesiones.delete(clave);
    }

    this.concesiones.set(concesiones);
  }

  guardar(): void {
    const idGrupo = this.idGrupoSeleccionado();
    if (!idGrupo || this.guardando()) return;

    this.limpiarAvisos();
    this.guardando.set(true);

    this.adminService.reemplazarPrivilegiosGrupo(idGrupo, { privilegios: this.armarPayload() }).subscribe({
      next: (respuesta) => {
        this.concesiones.set(this.concesionesDesdeRespuesta(respuesta.privilegios));
        const mensaje = 'Privilegios de interfaz actualizados correctamente.';
        this.mensaje.set(mensaje);
        this.toast.exito(mensaje);
      },
      error: (error) => {
        this.fallar(error, 'No fue posible guardar los privilegios de interfaz');
        this.guardando.set(false);
      },
      complete: () => this.guardando.set(false),
    });
  }

  private normalizarEntradaCatalogo(entrada: UiPrivilegeCatalogEntry): UiPrivilegeCatalogEntry {
    const soportados = new Set((entrada.estadosSoportados ?? []).filter((estado) => ESTADOS_ADMINISTRABLES.has(estado)));

    return {
      ...entrada,
      estadosSoportados: ORDEN_ESTADOS.filter((estado) => soportados.has(estado)),
    };
  }

  private concesionesDesdeRespuesta(privilegios: UiPrivilegeGroupGrant[]): ReadonlyMap<string, ReadonlySet<UiPrivilegeState>> {
    return new Map(
      (privilegios ?? []).map((privilegio) => [
        privilegio.claveComponente,
        new Set(ORDEN_ESTADOS.filter((estado) => privilegio.estados.includes(estado))),
      ]),
    );
  }

  private armarPayload(): UiPrivilegeGroupGrant[] {
    return this.catalogo()
      .map((entrada) => ({
        claveComponente: entrada.clave,
        estados: ORDEN_ESTADOS.filter(
          (estado) => entrada.estadosSoportados.includes(estado) && this.estaConcedido(entrada.clave, estado),
        ),
      }))
      .filter((privilegio) => privilegio.estados.length > 0);
  }

  private fallar(error: unknown, respaldo: string): void {
    const mensaje = obtenerMensajeError(error, respaldo);
    this.error.set(mensaje);
    this.toast.error(mensaje);
  }

  private limpiarAvisos(): void {
    this.mensaje.set('');
    this.error.set('');
  }
}
