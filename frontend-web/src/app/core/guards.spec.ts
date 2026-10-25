import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';
import { asignacionesGuard, consultaEventosGuard, miembrosGuard, rolesGuard } from './guards';
import { SesionService } from './sesion.service';

describe('authorization guards', () => {
  let sesion: jasmine.SpyObj<SesionService>;

  beforeEach(() => {
    sesion = jasmine.createSpyObj<SesionService>('SesionService', [
      'restaurar',
      'puedeGestionarMiembros',
      'esAdministradorSaas',
      'puedeVerAsignacionesStaff',
      'puedeVerEventos',
    ]);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SesionService, useValue: sesion }],
    });
  });

  it('allows organization member management for tenant administrators', async () => {
    sesion.restaurar.and.returnValue(of(true));
    sesion.puedeGestionarMiembros.and.returnValue(true);

    const resultado = await TestBed.runInInjectionContext(() =>
      firstValueFrom(miembrosGuard({} as never, {} as never) as Observable<boolean | UrlTree>),
    );

    expect(resultado).toBeTrue();
  });

  it('keeps global roles guarded by the SaaS administrator role only', async () => {
    sesion.restaurar.and.returnValue(of(true));
    sesion.esAdministradorSaas.and.returnValue(false);

    const resultado = await TestBed.runInInjectionContext(() =>
      firstValueFrom(rolesGuard({} as never, {} as never) as Observable<boolean | UrlTree>),
    );

    expect(resultado instanceof UrlTree).toBeTrue();
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/perfil');
  });

  it('denies the staff assignments route to users without staff or manager access', async () => {
    sesion.restaurar.and.returnValue(of(true));
    sesion.puedeVerAsignacionesStaff.and.returnValue(false);

    const resultado = await TestBed.runInInjectionContext(() =>
      firstValueFrom(asignacionesGuard({} as never, {} as never) as Observable<boolean | UrlTree>),
    );

    expect(resultado instanceof UrlTree).toBeTrue();
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/perfil');
  });

  it('denies event search to users without event view permission', async () => {
    sesion.restaurar.and.returnValue(of(true));
    sesion.puedeVerEventos.and.returnValue(false);

    const resultado = await TestBed.runInInjectionContext(() =>
      firstValueFrom(consultaEventosGuard({} as never, {} as never) as Observable<boolean | UrlTree>),
    );

    expect(resultado instanceof UrlTree).toBeTrue();
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/perfil');
  });
});
