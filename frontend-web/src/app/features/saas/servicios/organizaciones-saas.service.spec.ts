import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OrganizacionesSaasService } from './organizaciones-saas.service';

describe('OrganizacionesSaasService', () => {
  let service: OrganizacionesSaasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), OrganizacionesSaasService],
    });
    service = TestBed.inject(OrganizacionesSaasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests a paged, filtered global organization directory', () => {
    service.listar({ pagina: 2, tamano: 20, busqueda: 'Casa Norte', estado: 'INACTIVA' }).subscribe();

    const request = http.expectOne((candidate) => candidate.url.endsWith('/api/saas/organizaciones'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('pagina')).toBe('2');
    expect(request.request.params.get('tamano')).toBe('20');
    expect(request.request.params.get('busqueda')).toBe('Casa Norte');
    expect(request.request.params.get('estado')).toBe('INACTIVA');
    request.flush({ organizaciones: [], pagina: 2, tamano: 20, totalElementos: 0, totalPaginas: 0 });
  });

  it('patches only the organization lifecycle state', () => {
    service.actualizarEstado(8, 'INACTIVA').subscribe();

    const request = http.expectOne((candidate) => candidate.url.endsWith('/api/saas/organizaciones/8/estado'));
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ estado: 'INACTIVA' });
    request.flush(null);
  });
});
