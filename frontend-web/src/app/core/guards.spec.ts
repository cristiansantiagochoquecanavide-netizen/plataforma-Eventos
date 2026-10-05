import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';
import { miembrosGuard, rolesGuard } from './guards';
import { SesionService } from './sesion.service';

describe('authorization guards', () => {
  let sesion: jasmine.SpyObj<SesionService>;

  beforeEach(() => {
    sesion = jasmine.createSpyObj<SesionService>('SesionService', [
      'restaurar',
      'puedeGestionarMiembros',
      'esAdministradorSaas',
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
});
