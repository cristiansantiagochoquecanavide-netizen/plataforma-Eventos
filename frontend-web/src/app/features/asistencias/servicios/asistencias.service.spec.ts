import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AsistenciasService } from './asistencias.service';
import { environment } from '../../../../environments/environment';

describe('AsistenciasService', () => {
  let servicio: AsistenciasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(AsistenciasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta el listado acotado a CU23 y no el listado general de participantes', () => {
    servicio.listarInscripciones().subscribe((inscripciones) => expect(inscripciones).toEqual([]));
    const request = http.expectOne(`${environment.apiUrl}/asistencias/inscripciones-elegibles`);
    expect(request.request.method).toBe('GET');
    expect(request.request.url).not.toContain('/inscripciones/participantes');
    request.flush([]);
  });
});
