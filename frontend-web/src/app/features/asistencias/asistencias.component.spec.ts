import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AsistenciasComponent } from './asistencias.component';
import { AsistenciasService } from './servicios/asistencias.service';
import { Asistencia } from './modelos/asistencia.modelo';

const respuesta: Asistencia = {
  idAsistencia: 5, idInscripcion: 12, participante: 'Ana Pérez', correoParticipante: 'ana@example.com',
  idEvento: 3, nombreEvento: 'Encuentro', fechaHora: '2026-10-25T15:30:00Z', idRegistradoPor: 8,
  nombreRegistradoPor: 'Staff Demo',
};

describe('AsistenciasComponent', () => {
  let fixture: ComponentFixture<AsistenciasComponent>;
  let servicio: jasmine.SpyObj<AsistenciasService>;

  beforeEach(async () => {
    servicio = jasmine.createSpyObj<AsistenciasService>('AsistenciasService', ['listarInscripciones', 'registrar']);
    servicio.listarInscripciones.and.returnValue(of([{ idInscripcion: 12, nombres: 'Ana', apellidos: 'Pérez', correo: 'ana@example.com', idEvento: 3, tituloEvento: 'Encuentro', estado: 'CONFIRMADA', asistenciaRegistrada: false, fechaAsistencia: null }]));
    servicio.registrar.and.returnValue(of(respuesta));
    await TestBed.configureTestingModule({ imports: [AsistenciasComponent], providers: [{ provide: AsistenciasService, useValue: servicio }] }).compileComponents();
    fixture = TestBed.createComponent(AsistenciasComponent);
    fixture.detectChanges();
  });

  it('carga las inscripciones y permite la selección', () => {
    expect(fixture.nativeElement.textContent).toContain('Registrar asistencia');
    expect(fixture.nativeElement.querySelector('option[value="12"]')).toBeTruthy();
  });

  it('muestra las asistencias ya registradas y bloquea su seleccion', () => {
    fixture.componentInstance.inscripciones.set([{ idInscripcion: 12, nombres: 'Ana', apellidos: 'Perez',
      correo: 'ana@example.com', idEvento: 3, tituloEvento: 'Encuentro', estado: 'CONFIRMADA',
      asistenciaRegistrada: true, fechaAsistencia: '2026-10-25T15:30:00Z' }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('option[value="12"]').disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Asistencia registrada');
  });
  it('muestra respuesta y fecha del servidor tras un registro exitoso', () => {
    fixture.componentInstance.seleccionar(12);
    fixture.componentInstance.registrar(); fixture.detectChanges();
    expect(servicio.registrar).toHaveBeenCalledWith(12);
    expect(fixture.nativeElement.textContent).toContain('Ana Pérez');
    expect(fixture.nativeElement.textContent).toContain('Oct 25, 2026');
  });

  for (const [status, message] of [[401, 'Tu sesión expiró'], [409, 'Esta asistencia ya fue registrada.'], [403, 'No tienes permiso'], [404, 'No se puede registrar'], [400, 'No se puede registrar'], [500, 'No fue posible registrar']] as const) {
    it(`presenta mensaje seguro para HTTP ${status}`, () => {
      servicio.registrar.and.returnValue(throwError(() => new HttpErrorResponse({ status })));
      fixture.componentInstance.seleccionar(12); fixture.componentInstance.registrar(); fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain(message);
    });
  }
});
