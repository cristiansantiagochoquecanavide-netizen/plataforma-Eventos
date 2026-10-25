import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SesionService } from './sesion.service';
import { autenticacionInterceptor } from './autenticacion.interceptor';

describe('autenticacionInterceptor SaaS routes', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([autenticacionInterceptor])),
        provideHttpClientTesting(),
        {
          provide: SesionService,
          useValue: {
            tokenAcceso: () => 'saas-access-token',
            organizacionActivaId: () => 1,
          },
        },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('requires the logged-in SaaS token for the global organization console API', () => {
    http.get('/api/saas/organizaciones').subscribe();

    const request = backend.expectOne('/api/saas/organizaciones');
    expect(request.request.headers.get('Authorization')).toBe('Bearer saas-access-token');
    expect(request.request.headers.get('X-Organization-Id')).toBe('1');
    request.flush({ organizaciones: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 });
  });

  it('leaves the public plans catalog available before login', () => {
    http.get('/api/saas/planes').subscribe();

    const request = backend.expectOne('/api/saas/planes');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    expect(request.request.headers.has('X-Organization-Id')).toBeFalse();
    request.flush([]);
  });
});
