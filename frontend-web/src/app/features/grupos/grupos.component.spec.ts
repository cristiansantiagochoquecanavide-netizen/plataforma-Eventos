import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ToastService } from '../../core/toast.service';
import { Membresia } from '../organizaciones/modelos/organizacion.modelo';
import { OrganizacionesService } from '../organizaciones/servicios/organizaciones.service';
import { GrupoUsuario, RolGrupo } from './modelos/grupo-usuario.modelo';
import { GruposComponent } from './grupos.component';
import { GruposService } from './servicios/grupos.service';

describe('GruposComponent', () => {
  let fixture: ComponentFixture<GruposComponent>;
  let component: GruposComponent;
  let gruposService: jasmine.SpyObj<GruposService>;
  let organizacionesService: jasmine.SpyObj<OrganizacionesService>;
  let toast: jasmine.SpyObj<ToastService>;

  const grupo: GrupoUsuario = {
    id: 1,
    idOrganizacion: 10,
    nombre: 'Logistica',
    descripcion: 'Equipo logistico',
    activo: true,
    miembros: [{ idUsuario: 101, nombres: 'Ana', apellidos: 'Diaz', correo: 'ana@example.com' }],
    roles: [{ idRol: 5, nombre: 'STAFF' }],
  };
  const miembros: Membresia[] = [
    { idUsuario: 101, nombres: 'Ana', apellidos: 'Diaz', correo: 'ana@example.com', activo: true, cuentaActiva: true, roles: ['STAFF'] },
    { idUsuario: 102, nombres: 'Luis', apellidos: 'Rojas', correo: 'luis@example.com', activo: true, cuentaActiva: false, roles: ['PARTICIPANTE'] },
    { idUsuario: 103, nombres: 'Ivo', apellidos: 'Paz', correo: 'ivo@example.com', activo: false, cuentaActiva: true, roles: ['STAFF'] },
  ];
  const roles: RolGrupo[] = [
    { idRol: 5, nombre: 'STAFF' },
    { idRol: 6, nombre: 'ORGANIZADOR' },
  ];

  beforeEach(async () => {
    gruposService = jasmine.createSpyObj<GruposService>('GruposService', [
      'listarGrupos',
      'obtenerGrupo',
      'crearGrupo',
      'actualizarGrupo',
      'activarGrupo',
      'desactivarGrupo',
      'asignarMiembros',
      'asignarRoles',
      'listarRolesDisponibles',
    ]);
    organizacionesService = jasmine.createSpyObj<OrganizacionesService>('OrganizacionesService', ['listarMiembros']);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['exito', 'error']);

    gruposService.listarGrupos.and.returnValue(of([grupo]));
    gruposService.listarRolesDisponibles.and.returnValue(of(roles));
    gruposService.obtenerGrupo.and.returnValue(of(grupo));
    gruposService.crearGrupo.and.returnValue(of(grupo));
    gruposService.actualizarGrupo.and.returnValue(of(grupo));
    gruposService.activarGrupo.and.returnValue(of({ ...grupo, activo: true }));
    gruposService.desactivarGrupo.and.returnValue(of({ ...grupo, activo: false }));
    gruposService.asignarMiembros.and.returnValue(of(grupo));
    gruposService.asignarRoles.and.returnValue(of(grupo));
    organizacionesService.listarMiembros.and.returnValue(of(miembros));

    await TestBed.configureTestingModule({
      imports: [GruposComponent],
      providers: [
        { provide: GruposService, useValue: gruposService },
        { provide: OrganizacionesService, useValue: organizacionesService },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GruposComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads groups, members with active tenant membership and active global account, and tenant-assignable roles on init', () => {
    expect(gruposService.listarGrupos).toHaveBeenCalled();
    expect(organizacionesService.listarMiembros).toHaveBeenCalled();
    expect(gruposService.listarRolesDisponibles).toHaveBeenCalled();
    expect(component.grupos()).toEqual([grupo]);
    expect(component.miembrosDisponibles().map((m) => m.idUsuario)).toEqual([101]);
    expect(component.rolesDisponibles()).toEqual(roles);
  });

  it('preserves current member and role assignments when selecting a group', () => {
    component.seleccionarGrupo(grupo);

    expect(gruposService.obtenerGrupo).toHaveBeenCalledWith(1);
    expect(component.grupoSeleccionado()).toEqual(grupo);
    expect(component.miembrosSeleccionados().has(101)).toBeTrue();
    expect(component.rolesSeleccionados().has(5)).toBeTrue();
  });

  it('creates and edits groups with Spanish success feedback', () => {
    component.abrirCrear();
    component.formulario.setValue({ nombre: 'Produccion', descripcion: 'Equipo de produccion' });
    component.guardarGrupo();

    expect(gruposService.crearGrupo).toHaveBeenCalledWith({
      nombre: 'Produccion',
      descripcion: 'Equipo de produccion',
    });
    expect(toast.exito).toHaveBeenCalledWith('Grupo creado correctamente.');

    component.editar(grupo);
    component.formulario.setValue({ nombre: 'Logistica', descripcion: 'Equipo actualizado' });
    component.guardarGrupo();

    expect(gruposService.actualizarGrupo).toHaveBeenCalledWith(1, {
      nombre: 'Logistica',
      descripcion: 'Equipo actualizado',
    });
    expect(toast.exito).toHaveBeenCalledWith('Grupo actualizado correctamente.');
  });

  it('activates, deactivates and persists selected members and roles without losing unchecked assignments', () => {
    component.seleccionarGrupo(grupo);
    component.alternarMiembro(102, true);
    component.alternarRol(6, true);
    component.guardarAsignaciones();
    component.desactivar(grupo);
    component.activar({ ...grupo, activo: false });

    expect(gruposService.asignarMiembros).toHaveBeenCalledWith(1, [101, 102]);
    expect(gruposService.asignarRoles).toHaveBeenCalledWith(1, [5, 6]);
    expect(gruposService.desactivarGrupo).toHaveBeenCalledWith(1);
    expect(gruposService.activarGrupo).toHaveBeenCalledWith(1);
  });

  it('reports API errors from the backend response message', () => {
    gruposService.listarGrupos.and.returnValue(throwError(() => ({ error: { message: 'No autorizado' } })));
    component.cargarGrupos();

    expect(component.error()).toBe('No autorizado');
    expect(toast.error).toHaveBeenCalledWith('No autorizado');
  });
});
