import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { OrganizacionesSaasService } from './servicios/organizaciones-saas.service';
import { OrganizacionesSaasComponent } from './organizaciones-saas.component';

describe('OrganizacionesSaasComponent', () => {
  let fixture: ComponentFixture<OrganizacionesSaasComponent>;
  let service: jasmine.SpyObj<OrganizacionesSaasService>;
  let sesion: jasmine.SpyObj<SesionService>;
  let toast: jasmine.SpyObj<ToastService>;
  let router: Router;

  const page = {
    organizaciones: [{
      id: 8,
      nombre: 'Club Illimani',
      slug: 'club-illimani',
      estado: 'ACTIVA' as const,
      fechaCreacion: '2026-10-01T00:00:00Z',
      miembrosActivos: 29,
      plan: 'PROFESIONAL' as const,
      estadoSuscripcion: 'ACTIVA' as const,
      fechaInicio: '2026-10-01T00:00:00Z',
      fechaFin: null,
      suscripcionVigente: true,
    }],
    pagina: 0,
    tamano: 20,
    totalElementos: 1,
    totalPaginas: 1,
  };

  beforeEach(async () => {
    service = jasmine.createSpyObj<OrganizacionesSaasService>('OrganizacionesSaasService', ['listar', 'actualizarEstado']);
    service.listar.and.returnValue(of(page));
    service.actualizarEstado.and.returnValue(of(undefined));
    sesion = jasmine.createSpyObj<SesionService>('SesionService', ['seleccionarOrganizacion']);
    sesion.seleccionarOrganizacion.and.returnValue(of(null));
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['exito']);

    await TestBed.configureTestingModule({
      imports: [OrganizacionesSaasComponent],
      providers: [
        provideRouter([]),
        { provide: OrganizacionesSaasService, useValue: service },
        { provide: SesionService, useValue: sesion },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrganizacionesSaasComponent);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
  });

  it('renders tenant health, subscription and member summary from the directory response', () => {
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(service.listar).toHaveBeenCalledOnceWith({ pagina: 0, tamano: 20, busqueda: '', estado: null });
    expect(text).toContain('Club Illimani');
    expect(text).toContain('29');
    expect(text).toContain('PROFESIONAL');
    expect(text).toContain('Vigente');
  });

  it('switches into a selected active organization workspace', () => {
    fixture.detectChanges();

    fixture.componentInstance.abrirOrganizacion(page.organizaciones[0]);

    expect(sesion.seleccionarOrganizacion).toHaveBeenCalledOnceWith(8);
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/perfil');
  });

  it('suspends organization access without changing Stripe subscription fields', () => {
    fixture.detectChanges();

    fixture.componentInstance.confirmarCambioEstado(page.organizaciones[0]);
    fixture.componentInstance.guardarCambioEstado();

    expect(service.actualizarEstado).toHaveBeenCalledOnceWith(8, 'INACTIVA');
    expect(toast.exito).toHaveBeenCalled();
    expect(service.listar).toHaveBeenCalledTimes(2);
  });
});
