import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../features/auth/servicios/auth.service';
import { Usuario } from '../features/usuarios/modelos/usuario.modelo';
import { SesionService } from './sesion.service';

describe('SesionService', () => {
  let auth: jasmine.SpyObj<AuthService>;
  let service: SesionService;

  const sesion = { accessToken: 'access', refreshToken: 'refresh', tipoToken: 'Bearer' };

  beforeEach(() => {
    localStorage.clear();
    auth = jasmine.createSpyObj<AuthService>('AuthService', [
      'iniciarSesion',
      'usuarioActual',
      'refrescar',
      'cerrarSesion',
    ]);

    TestBed.configureTestingModule({
      providers: [
        SesionService,
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigateByUrl']) },
      ],
    });
    service = TestBed.inject(SesionService);
  });

  afterEach(() => localStorage.clear());

  it('keeps global SaaS roles separate from tenant member management', () => {
    service.usuario.set(usuario({
      roles: ['SAAS_ADMINISTRADOR'],
      perfilOrganizacionActiva: {
        idOrganizacion: 7,
        roles: ['ADMINISTRADOR'],
        permisos: [],
        administrador: true,
      },
    }));

    expect(service.tieneAccesoRoles()).toBeTrue();
    expect(service.puedeGestionarMiembros()).toBeTrue();
    expect(service.rolParaCabecera()).toBe('ADMINISTRADOR SAAS · ADMINISTRADOR');
  });

  it('allows tenant administrators to manage members without global roles access', () => {
    service.usuario.set(
      usuario({
        roles: ['PARTICIPANTE'],
        perfilOrganizacionActiva: {
          idOrganizacion: 7,
          roles: ['ADMINISTRADOR'],
          permisos: [],
          administrador: true,
        },
      }),
    );

    expect(service.puedeGestionarMiembros()).toBeTrue();
    expect(service.tieneAccesoRoles()).toBeFalse();
    expect(service.rolParaCabecera()).toBe('ADMINISTRADOR');
  });

  it('uses tenant permissions for feature visibility instead of global permissions', () => {
    service.usuario.set(usuario({ permisos: ['EVENTO_VER'], perfilOrganizacionActiva: null }));
    expect(service.tieneAccesoEventos()).toBeFalse();

    service.usuario.set(
      usuario({
        permisos: [],
        perfilOrganizacionActiva: {
          idOrganizacion: 7,
          roles: ['STAFF'],
          permisos: ['EVENTO_VER'],
          administrador: false,
        },
      }),
    );
    expect(service.tieneAccesoEventos()).toBeTrue();
  });

  it('shows staff assignments only to staff applicants or assignment managers', () => {
    service.usuario.set(usuario({
      perfilOrganizacionActiva: { idOrganizacion: 7, roles: ['PARTICIPANTE'], permisos: [], administrador: false },
    }));
    expect(service.puedeVerAsignacionesStaff()).toBeFalse();

    service.usuario.set(usuario({
      perfilOrganizacionActiva: { idOrganizacion: 7, roles: ['STAFF'], permisos: ['POSTULACION_STAFF_CREAR'], administrador: false },
    }));
    expect(service.puedeVerAsignacionesStaff()).toBeTrue();

    service.usuario.set(usuario({
      perfilOrganizacionActiva: { idOrganizacion: 7, roles: ['ORGANIZADOR'], permisos: ['ASIGNACION_STAFF_GESTIONAR'], administrador: false },
    }));
    expect(service.puedeVerAsignacionesStaff()).toBeTrue();
  });

  it('refreshes the current user profile when the active organization changes', () => {
    const perfilInicial = usuario({ perfilOrganizacionActiva: { idOrganizacion: 1, roles: [], permisos: [], administrador: false } });
    const perfilActualizado = usuario({ perfilOrganizacionActiva: { idOrganizacion: 2, roles: ['ADMINISTRADOR'], permisos: [], administrador: true } });
    auth.iniciarSesion.and.returnValue(of(sesion));
    auth.usuarioActual.and.returnValues(of(perfilInicial), of(perfilActualizado));

    service.iniciar('ana@test.local', 'Clave123!').subscribe();
    service.seleccionarOrganizacion(2).subscribe();

    expect(service.organizacionActivaId()).toBe(2);
    expect(service.usuario()?.perfilOrganizacionActiva?.idOrganizacion).toBe(2);
    expect(service.puedeGestionarMiembros()).toBeTrue();
    expect(auth.usuarioActual).toHaveBeenCalledTimes(2);
  });

  function usuario(parcial: Partial<Usuario>): Usuario {
    return {
      id: 1,
      nombres: 'Ana',
      apellidos: 'Admin',
      correo: 'ana@test.local',
      roles: [],
      permisos: [],
      ...parcial,
    };
  }
});
