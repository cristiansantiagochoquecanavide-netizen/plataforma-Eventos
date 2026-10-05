import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ToastService } from '../../core/toast.service';
import { UiPrivilegesService } from '../../core/ui-privileges.service';
import { GrupoUsuario } from '../grupos/modelos/grupo-usuario.modelo';
import { GruposService } from '../grupos/servicios/grupos.service';
import { PrivilegiosUiComponent } from './privilegios-ui.component';
import { UiPrivilegeAdminService } from './servicios/ui-privilege-admin.service';

const grupo: GrupoUsuario = {
  id: 7,
  idOrganizacion: 3,
  nombre: 'Operaciones',
  descripcion: 'Equipo operativo',
  activo: true,
  miembros: [],
  roles: [],
};

const catalogo = [
  {
    clave: 'events.create.button',
    tipo: 'BOTON',
    descripcion: 'Crear eventos',
    estadosSoportados: ['VISIBLE', 'HABILITADO', 'ELIMINADO'],
    controlRecuperacion: false,
  },
  {
    clave: 'ui-privileges.group-grants',
    tipo: 'FORMULARIO',
    descripcion: 'Administrar permisos UI',
    estadosSoportados: ['VISIBLE', 'HABILITADO', 'EDITABLE'],
    controlRecuperacion: true,
  },
] as const;

describe('PrivilegiosUiComponent', () => {
  let fixture: ComponentFixture<PrivilegiosUiComponent>;
  let component: PrivilegiosUiComponent;
  let gruposService: jasmine.SpyObj<GruposService>;
  let adminService: jasmine.SpyObj<UiPrivilegeAdminService>;
  let toast: jasmine.SpyObj<ToastService>;
  let runtimePrivileges: jasmine.SpyObj<UiPrivilegesService>;

  beforeEach(async () => {
    gruposService = jasmine.createSpyObj<GruposService>('GruposService', ['listarGrupos']);
    adminService = jasmine.createSpyObj<UiPrivilegeAdminService>('UiPrivilegeAdminService', [
      'listarCatalogo',
      'obtenerPrivilegiosGrupo',
      'reemplazarPrivilegiosGrupo',
    ]);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['exito', 'error']);
    runtimePrivileges = jasmine.createSpyObj<UiPrivilegesService>('UiPrivilegesService', ['allows']);

    gruposService.listarGrupos.and.returnValue(of([grupo]));
    adminService.listarCatalogo.and.returnValue(of([...catalogo]));
    adminService.obtenerPrivilegiosGrupo.and.returnValue(
      of({
        idGrupo: 7,
        privilegios: [{ claveComponente: 'events.create.button', estados: ['VISIBLE'] }],
      }),
    );
    adminService.reemplazarPrivilegiosGrupo.and.returnValue(of({ idGrupo: 7, privilegios: [] }));
    runtimePrivileges.allows.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [PrivilegiosUiComponent],
      providers: [
        { provide: GruposService, useValue: gruposService },
        { provide: UiPrivilegeAdminService, useValue: adminService },
        { provide: ToastService, useValue: toast },
        { provide: UiPrivilegesService, useValue: runtimePrivileges },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PrivilegiosUiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads groups and catalog on init and renders only supported administrable states', () => {
    expect(gruposService.listarGrupos).toHaveBeenCalled();
    expect(adminService.listarCatalogo).toHaveBeenCalled();
    expect(component.grupos()).toEqual([grupo]);
    expect(component.catalogo()[0].estadosSoportados).toEqual(['VISIBLE', 'HABILITADO']);
    expect(component.catalogo()[1].estadosSoportados).toEqual(['VISIBLE', 'HABILITADO', 'EDITABLE']);
  });

  it('treats blank, nonpositive, and invalid group ids as no selection without loading grants', () => {
    component.concesiones.set(new Map([['events.create.button', new Set(['VISIBLE'])]]));

    component.seleccionarGrupo('');
    component.seleccionarGrupo(0);
    component.seleccionarGrupo(-1);
    component.seleccionarGrupo('not-a-number');

    expect(component.idGrupoSeleccionado()).toBeNull();
    expect(component.concesiones().size).toBe(0);
    expect(component.cargandoPrivilegios()).toBeFalse();
    expect(adminService.obtenerPrivilegiosGrupo).not.toHaveBeenCalled();
  });

  it('loads selected group grants and replaces them with the confirmed payload shape', () => {
    component.seleccionarGrupo(7);
    component.alternarEstado('events.create.button', 'HABILITADO', true);
    component.alternarEstado('events.create.button', 'EDITABLE', true);
    component.guardar();

    expect(adminService.obtenerPrivilegiosGrupo).toHaveBeenCalledWith(7);
    expect(adminService.reemplazarPrivilegiosGrupo).toHaveBeenCalledWith(7, {
      privilegios: [
        { claveComponente: 'events.create.button', estados: ['VISIBLE', 'HABILITADO'] },
      ],
    });
    expect(toast.exito).toHaveBeenCalledWith('Privilegios de interfaz actualizados correctamente.');
  });

  it('auto-adds VISIBLE when granting HABILITADO or EDITABLE states', () => {
    component.seleccionarGrupo(7);
    component.alternarEstado('events.create.button', 'VISIBLE', false);
    component.alternarEstado('events.create.button', 'HABILITADO', true);
    component.alternarEstado('ui-privileges.group-grants', 'EDITABLE', true);

    expect(component.estaConcedido('events.create.button', 'VISIBLE')).toBeTrue();
    expect(component.estaConcedido('events.create.button', 'HABILITADO')).toBeTrue();
    expect(component.estaConcedido('ui-privileges.group-grants', 'VISIBLE')).toBeTrue();
    expect(component.estaConcedido('ui-privileges.group-grants', 'EDITABLE')).toBeTrue();
  });

  it('removes dependent HABILITADO and EDITABLE states when VISIBLE is unchecked', () => {
    component.seleccionarGrupo(7);
    component.alternarEstado('ui-privileges.group-grants', 'HABILITADO', true);
    component.alternarEstado('ui-privileges.group-grants', 'EDITABLE', true);

    component.alternarEstado('ui-privileges.group-grants', 'VISIBLE', false);

    expect(component.estaConcedido('ui-privileges.group-grants', 'VISIBLE')).toBeFalse();
    expect(component.estaConcedido('ui-privileges.group-grants', 'HABILITADO')).toBeFalse();
    expect(component.estaConcedido('ui-privileges.group-grants', 'EDITABLE')).toBeFalse();
  });

  it('surfaces backend errors in Spanish and keeps the recovery-access note visible', () => {
    adminService.obtenerPrivilegiosGrupo.and.returnValue(throwError(() => ({ error: { message: 'Sin permisos' } })));

    component.seleccionarGrupo(7);
    fixture.detectChanges();

    expect(component.error()).toBe('Sin permisos');
    expect(toast.error).toHaveBeenCalledWith('Sin permisos');
    expect(fixture.nativeElement.textContent).toContain('acceso de recuperación');
    expect(fixture.nativeElement.textContent).toContain('no implica acceso a controles de negocio');
  });
});
