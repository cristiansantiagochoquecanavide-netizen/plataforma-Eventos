import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { UiPrivilegeEditableDirective, UiPrivilegeEnabledDirective, UiPrivilegeVisibleDirective } from '../../core/ui-privilege.directive';
import { UiPrivilegesService } from '../../core/ui-privileges.service';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { EventosService } from '../eventos/servicios/eventos.service';
import { ParticipanteDetalle, ParticipanteResumen } from './modelos/participante.modelo';
import { ParticipantesComponent } from './participantes.component';
import { ParticipantesService } from './servicios/participantes.service';

const participante: ParticipanteResumen = {
  idUsuario: 7,
  nombres: 'Lina',
  apellidos: 'Rojas',
  correo: 'lina@example.com',
  estado: 'ACTIVO',
  fechaRegistro: '2025-01-01T00:00:00.000Z',
  totalInscripciones: 2,
  inscripcionesConfirmadas: 1,
  inscripcionesCanceladas: 1,
  ultimaInscripcion: '2025-02-01T00:00:00.000Z',
};

const detalle: ParticipanteDetalle = {
  ...participante,
  inscripciones: [
    {
      idInscripcion: 1,
      idEvento: 10,
      tituloEvento: 'Congreso Regional',
      fechaInicioEvento: '2026-01-10T09:00:00.000Z',
      fechaInscripcion: '2025-02-01T00:00:00.000Z',
      codigoQr: 'QR-1',
      estado: 'CONFIRMADA',
    },
  ],
};

describe('ParticipantesComponent UI privileges', () => {
  let fixture: ComponentFixture<ParticipantesComponent>;
  let component: ParticipantesComponent;
  let participantesService: jasmine.SpyObj<ParticipantesService>;
  let privileges: jasmine.SpyObj<UiPrivilegesService>;

  beforeEach(async () => {
    participantesService = jasmine.createSpyObj<ParticipantesService>('ParticipantesService', [
      'listar',
      'obtenerDetalle',
      'cambiarEstado',
      'exportarCsv',
    ]);
    const eventosService = jasmine.createSpyObj<EventosService>('EventosService', ['listarEventos']);
    const toast = jasmine.createSpyObj<ToastService>('ToastService', ['exito', 'error']);
    const sesion = jasmine.createSpyObj<SesionService>('SesionService', ['puedeGestionarParticipantes']);
    privileges = jasmine.createSpyObj<UiPrivilegesService>('UiPrivilegesService', ['allows']);

    participantesService.listar.and.returnValue(of({
      elementos: [participante],
      pagina: 0,
      tamano: 10,
      totalElementos: 1,
      totalPaginas: 1,
    }));
    participantesService.obtenerDetalle.and.returnValue(of(detalle));
    participantesService.cambiarEstado.and.returnValue(of({ ...participante, estado: 'INACTIVO' }));
    participantesService.exportarCsv.and.returnValue(of(new Blob(['id'], { type: 'text/csv' })));
    eventosService.listarEventos.and.returnValue(of([]));
    sesion.puedeGestionarParticipantes.and.returnValue(true);
    privileges.allows.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [ParticipantesComponent],
      providers: [
        { provide: ParticipantesService, useValue: participantesService },
        { provide: EventosService, useValue: eventosService },
        { provide: ToastService, useValue: toast },
        { provide: SesionService, useValue: sesion },
        { provide: UiPrivilegesService, useValue: privileges },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ParticipantesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('gates participant list, filters, export, history and status controls with stable policy keys', () => {
    const visibleTargets = fixture.debugElement.queryAll(By.directive(UiPrivilegeVisibleDirective));
    const editableTargets = fixture.debugElement.queryAll(By.directive(UiPrivilegeEditableDirective));
    const enabledTargets = fixture.debugElement.queryAll(By.directive(UiPrivilegeEnabledDirective));

    expect(visibleTargets.some((target) => target.nativeElement.matches('section.table-card'))).toBeTrue();
    expect(visibleTargets.some((target) => target.nativeElement.matches('section.filters-card'))).toBeTrue();
    expect(editableTargets.some((target) => target.nativeElement.matches('input#busqueda'))).toBeTrue();
    expect(enabledTargets.some((target) => target.nativeElement.matches('.heading-actions button.secondary-btn'))).toBeTrue();
    expect(enabledTargets.some((target) => target.nativeElement.matches('button.btn-detail'))).toBeTrue();
    expect(enabledTargets.some((target) => target.nativeElement.matches('button.btn-toggle'))).toBeTrue();

    expect(privileges.allows).toHaveBeenCalledWith('participants.list', 'VISIBLE');
    expect(privileges.allows).toHaveBeenCalledWith('participants.filters', 'EDITABLE');
    expect(privileges.allows).toHaveBeenCalledWith('participants.export', 'HABILITADO');
    expect(privileges.allows).toHaveBeenCalledWith('participants.history', 'HABILITADO');
    expect(privileges.allows).toHaveBeenCalledWith('participants.status.toggle', 'HABILITADO');
  });

  it('preserves business disabled state while UI policy enables participant export', () => {
    component.exportando.set(true);
    fixture.detectChanges();

    const exportButton = fixture.debugElement.query(By.css('.heading-actions button.secondary-btn')).nativeElement as HTMLButtonElement;
    expect(exportButton.textContent).toContain('Generando...');
    expect(exportButton.disabled).toBeTrue();
    expect(privileges.allows).toHaveBeenCalledWith('participants.export', 'HABILITADO');
  });

  it('gates participant detail and keeps close controls ungated', () => {
    component.verDetalle(participante);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('.participant-summary-card'))).not.toBeNull();
    expect(privileges.allows).toHaveBeenCalledWith('participants.detail', 'VISIBLE');
    expect(fixture.debugElement.query(By.css('.modal-header .btn-close'))).not.toBeNull();
    expect(fixture.debugElement.query(By.css('.modal-footer button.secondary-btn'))).not.toBeNull();
  });
});
