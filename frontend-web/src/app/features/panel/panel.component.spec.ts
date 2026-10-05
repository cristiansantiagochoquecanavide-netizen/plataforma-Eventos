import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { UiPrivilegesService } from '../../core/ui-privileges.service';
import { SesionService } from '../../core/sesion.service';
import { OrganizacionesService } from '../organizaciones/servicios/organizaciones.service';
import { PanelComponent } from './panel.component';

class SesionServiceStub {
  private activeOrganizationId: number | null = 2;
  readonly usuario = () => ({ nombres: 'Ada', apellidos: 'Lovelace', roles: ['ADMIN'] });

  organizacionActivaId = Object.assign(() => this.activeOrganizationId, {
    set: (id: number | null) => {
      this.activeOrganizationId = id;
    },
  });

  seleccionarOrganizacion = jasmine
    .createSpy('seleccionarOrganizacion')
    .and.callFake((id: number) => {
      this.activeOrganizationId = id;
      return of(this.usuario());
    });

  esAdministrador = jasmine.createSpy('esAdministrador').and.returnValue(true);
  esAdministradorSaas = jasmine.createSpy('esAdministradorSaas').and.returnValue(false);
  tieneAccesoUsuarios = jasmine.createSpy('tieneAccesoUsuarios').and.returnValue(false);
  puedeVerParticipantes = jasmine.createSpy('puedeVerParticipantes').and.returnValue(false);
  tieneAccesoRoles = jasmine.createSpy('tieneAccesoRoles').and.returnValue(false);
  puedeGestionarMiembros = jasmine.createSpy('puedeGestionarMiembros').and.returnValue(false);
  puedeVerBitacora = jasmine.createSpy('puedeVerBitacora').and.returnValue(false);
  tieneAccesoEventos = jasmine.createSpy('tieneAccesoEventos').and.returnValue(false);
  tieneAccesoRecursos = jasmine.createSpy('tieneAccesoRecursos').and.returnValue(false);
  tieneAccesoEspacios = jasmine.createSpy('tieneAccesoEspacios').and.returnValue(false);
  tieneAccesoConvocatorias = jasmine.createSpy('tieneAccesoConvocatorias').and.returnValue(false);
  puedeCrearPostulacionesStaff = jasmine.createSpy('puedeCrearPostulacionesStaff').and.returnValue(false);
  puedeGestionarPostulacionesStaff = jasmine.createSpy('puedeGestionarPostulacionesStaff').and.returnValue(false);
  puedeGestionarAsignacionesStaff = jasmine.createSpy('puedeGestionarAsignacionesStaff').and.returnValue(false);
  cerrar = jasmine.createSpy('cerrar');
}

describe('PanelComponent tenant policy integration', () => {
  let fixture: ComponentFixture<PanelComponent>;
  let component: PanelComponent;
  let sesion: SesionServiceStub;
  let organizacionesService: jasmine.SpyObj<OrganizacionesService>;
  let uiPrivileges: jasmine.SpyObj<UiPrivilegesService>;
  let events: string[];

  beforeEach(async () => {
    events = [];
    sesion = new SesionServiceStub();
    organizacionesService = jasmine.createSpyObj<OrganizacionesService>('OrganizacionesService', ['listarMias']);
    uiPrivileges = jasmine.createSpyObj<UiPrivilegesService>('UiPrivilegesService', ['clear', 'loadForOrganization', 'allows']);

    organizacionesService.listarMias.and.returnValue(
      of([
        { id: 1, nombre: 'Org A', slug: 'org-a', estado: 'ACTIVA' },
        { id: 2, nombre: 'Org B', slug: 'org-b', estado: 'ACTIVA' },
      ]),
    );
    uiPrivileges.clear.and.callFake(() => events.push('clear'));
    uiPrivileges.loadForOrganization.and.callFake((id: number) => {
      events.push(`load:${id}`);
      return of(undefined);
    });
    uiPrivileges.allows.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [PanelComponent],
      providers: [
        provideRouter([]),
        { provide: SesionService, useValue: sesion },
        { provide: OrganizacionesService, useValue: organizacionesService },
        { provide: UiPrivilegesService, useValue: uiPrivileges },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PanelComponent);
    component = fixture.componentInstance;
  });

  it('reloads a persisted valid tenant before loading its effective UI policy on first panel visit', () => {
    sesion.seleccionarOrganizacion.and.callFake((id: number) => {
      events.push(`select:${id}`);
      sesion.organizacionActivaId.set(id);
      return of(sesion.usuario());
    });

    fixture.detectChanges();

    expect(sesion.seleccionarOrganizacion).toHaveBeenCalledOnceWith(2);
    expect(uiPrivileges.loadForOrganization).toHaveBeenCalledOnceWith(2);
    expect(events).toEqual(['clear', 'select:2', 'load:2']);
  });

  it('clears old UI policy before changing tenant and loads the new tenant policy after selection completes', () => {
    organizacionesService.listarMias.calls.reset();
    sesion.seleccionarOrganizacion.calls.reset();
    uiPrivileges.clear.calls.reset();
    uiPrivileges.loadForOrganization.calls.reset();
    component.organizaciones = [
      { id: 1, nombre: 'Org A', slug: 'org-a', estado: 'ACTIVA' },
      { id: 2, nombre: 'Org B', slug: 'org-b', estado: 'ACTIVA' },
    ];
    events = [];
    sesion.seleccionarOrganizacion.and.callFake((id: number) => {
      events.push(`select:${id}`);
      sesion.organizacionActivaId.set(id);
      return of(sesion.usuario());
    });

    component.seleccionarOrganizacion(1);

    expect(sesion.seleccionarOrganizacion).toHaveBeenCalledOnceWith(1);
    expect(uiPrivileges.loadForOrganization).toHaveBeenCalledOnceWith(1);
    expect(events).toEqual(['clear', 'select:1', 'load:1']);
  });

  it('renders the groups navigation link from server UI policy even when the local user is not administrator', () => {
    sesion.esAdministrador.and.returnValue(false);
    uiPrivileges.allows.and.callFake((key, state) => key === 'groups.menu' && state === 'VISIBLE');

    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;
    const groupsLink = Array.from(nativeElement.querySelectorAll('a')).find(
      (link): link is HTMLAnchorElement => link.textContent?.trim() === 'Grupos',
    );
    expect(groupsLink).toBeDefined();
    expect(groupsLink?.hidden).toBeFalse();
    expect(groupsLink?.getAttribute('routerLink')).toBe('/grupos');
    expect(sesion.esAdministrador).not.toHaveBeenCalled();
  });

  it('keeps deny-by-default policy when tenant selection fails', () => {
    component.organizaciones = [{ id: 1, nombre: 'Org A', slug: 'org-a', estado: 'ACTIVA' }];
    sesion.seleccionarOrganizacion.and.returnValue(throwError(() => new Error('selection failed')));
    const router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);

    component.seleccionarOrganizacion(1);

    expect(uiPrivileges.clear).toHaveBeenCalled();
    expect(uiPrivileges.loadForOrganization).not.toHaveBeenCalled();
  });
});
