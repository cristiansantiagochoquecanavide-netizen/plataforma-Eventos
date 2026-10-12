import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { UiPrivilegesService } from '../../core/ui-privileges.service';
import { SesionService } from '../../core/sesion.service';
import { OrganizacionesService } from '../organizaciones/servicios/organizaciones.service';
import { SaasShellComponent } from './saas-shell.component';

describe('SaasShellComponent', () => {
  let fixture: ComponentFixture<SaasShellComponent>;
  let organizaciones: jasmine.SpyObj<OrganizacionesService>;
  let privilegios: jasmine.SpyObj<UiPrivilegesService>;
  let sesion: jasmine.SpyObj<SesionService>;

  beforeEach(async () => {
    organizaciones = jasmine.createSpyObj<OrganizacionesService>('OrganizacionesService', ['listarMias']);
    organizaciones.listarMias.and.returnValue(of([
      { id: 1, nombre: 'Organizacion principal', slug: 'principal', estado: 'ACTIVA' },
    ]));
    privilegios = jasmine.createSpyObj<UiPrivilegesService>('UiPrivilegesService', ['loadForOrganization', 'clear']);
    privilegios.loadForOrganization.and.returnValue(of(undefined));
    sesion = jasmine.createSpyObj<SesionService>('SesionService', [
      'seleccionarOrganizacion', 'rolParaCabecera', 'cerrar', 'organizacionActivaId', 'usuario',
    ]);
    sesion.organizacionActivaId.and.returnValue(null);
    sesion.usuario.and.returnValue({
      id: 1,
      nombres: 'Prueba',
      apellidos: 'Inicial',
      correo: 'admin@eventos.local',
      roles: ['SAAS_ADMINISTRADOR'],
      permisos: ['ROL_VER'],
    } as ReturnType<SesionService['usuario']>);
    sesion.rolParaCabecera.and.returnValue('ADMINISTRADOR SAAS · ADMINISTRADOR');
    sesion.seleccionarOrganizacion.and.returnValue(of(null));

    await TestBed.configureTestingModule({
      imports: [SaasShellComponent],
      providers: [
        provideRouter([]),
        { provide: SesionService, useValue: sesion },
        { provide: OrganizacionesService, useValue: organizaciones },
        { provide: UiPrivilegesService, useValue: privilegios },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SaasShellComponent);
  });

  it('loads the selected tenant policy and only presents platform navigation', () => {
    fixture.detectChanges();

    const links = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('nav a'))
      .map((link) => link.textContent?.replace(/\s+/g, ' ').trim() ?? '');
    expect(organizaciones.listarMias).toHaveBeenCalled();
    expect(sesion.seleccionarOrganizacion).toHaveBeenCalledOnceWith(1);
    expect(privilegios.loadForOrganization).toHaveBeenCalledOnceWith(1);
    expect(links.some((label) => label.includes('Organizaciones'))).toBeTrue();
    expect(links.some((label) => label.includes('Roles globales'))).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Eventos');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ADMINISTRADOR SAAS');
  });
});
