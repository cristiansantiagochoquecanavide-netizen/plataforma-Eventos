import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import {
  UiPrivilegeEnabledDirective,
  UiPrivilegeEditableDirective,
  UiPrivilegeVisibleDirective,
} from '../../core/ui-privilege.directive';
import { UiPrivilegesService } from '../../core/ui-privileges.service';
import { SesionService } from '../../core/sesion.service';
import { ToastService } from '../../core/toast.service';
import { CategoriasService } from '../categorias/servicios/categorias.service';
import { EspaciosService } from '../espacios/servicios/espacios.service';
import { Evento } from './modelos/evento.modelo';
import { EventosComponent } from './eventos.component';
import { EventosService } from './servicios/eventos.service';

const evento: Evento = {
  id: 1,
  nombre: 'Congreso Regional',
  descripcion: 'Evento principal',
  fechaInicio: '2026-01-10T09:00:00.000Z',
  fechaFin: null,
  lugarPrincipal: 'Auditorio',
  categoria: { id: 1, nombre: 'Tecnologia', descripcion: null },
  categoriaNombre: 'Tecnologia',
  espacio: { id: 2, nombre: 'Sala A', ubicacion: 'Piso 1', capacidad: 100 },
  aforoMaximo: 80,
  estado: 'PLANIFICADO',
  responsables: [{ id: 5, nombres: 'Ana', apellidos: 'Diaz', correo: 'ana@example.com', roles: ['ORGANIZADOR'] }],
};

function enabledPrivilegeKey(host: DebugElement): string | undefined {
  const privilege = host.injector.get(UiPrivilegeEnabledDirective).privilege;
  return typeof privilege === 'string' ? privilege : privilege?.key;
}

describe('EventosComponent UI privileges', () => {
  let fixture: ComponentFixture<EventosComponent>;
  let component: EventosComponent;
  let eventosService: jasmine.SpyObj<EventosService>;
  let privileges: jasmine.SpyObj<UiPrivilegesService>;
  let sesion: jasmine.SpyObj<SesionService>;

  beforeEach(async () => {
    eventosService = jasmine.createSpyObj<EventosService>('EventosService', [
      'listarEventos',
      'crearEvento',
      'actualizarEvento',
      'asignarResponsablesEvento',
      'listarResponsablesDisponiblesEvento',
    ]);
    const categoriasService = jasmine.createSpyObj<CategoriasService>('CategoriasService', ['listarCategorias']);
    const espaciosService = jasmine.createSpyObj<EspaciosService>('EspaciosService', ['listarEspacios']);
    const toast = jasmine.createSpyObj<ToastService>('ToastService', ['exito', 'error']);
    sesion = jasmine.createSpyObj<SesionService>('SesionService', [
      'puedeCrearEventos',
      'puedeEditarEventos',
      'puedeAsignarResponsablesEvento',
    ]);
    privileges = jasmine.createSpyObj<UiPrivilegesService>('UiPrivilegesService', ['allows']);

    eventosService.listarEventos.and.returnValue(of([evento]));
    eventosService.listarResponsablesDisponiblesEvento.and.returnValue(of(evento.responsables));
    categoriasService.listarCategorias.and.returnValue(of([evento.categoria!]));
    espaciosService.listarEspacios.and.returnValue(of([evento.espacio!]));
    sesion.puedeCrearEventos.and.returnValue(true);
    sesion.puedeEditarEventos.and.returnValue(true);
    sesion.puedeAsignarResponsablesEvento.and.returnValue(true);
    privileges.allows.and.returnValue(true);

    await TestBed.configureTestingModule({
      imports: [EventosComponent],
      providers: [
        { provide: EventosService, useValue: eventosService },
        { provide: CategoriasService, useValue: categoriasService },
        { provide: EspaciosService, useValue: espaciosService },
        { provide: SesionService, useValue: sesion },
        { provide: ToastService, useValue: toast },
        { provide: UiPrivilegesService, useValue: privileges },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EventosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('gates the event list, create action, edit action, summary and form fields with stable policy keys', () => {
    component.abrirModalCrear();
    fixture.detectChanges();

    const visibleHosts = fixture.debugElement.queryAll(By.directive(UiPrivilegeVisibleDirective));
    const enabledHosts = fixture.debugElement.queryAll(By.directive(UiPrivilegeEnabledDirective));
    const editableHosts = fixture.debugElement.queryAll(By.directive(UiPrivilegeEditableDirective));

    expect(visibleHosts.some((host) => host.injector.get(UiPrivilegeVisibleDirective).key === 'events.list')).toBeTrue();
    expect(fixture.debugElement.query(By.css('.page-heading button.primary'))).not.toBeNull();
    expect(fixture.debugElement.query(By.css('.actions button'))).not.toBeNull();
    expect(fixture.debugElement.query(By.css('form.form-grid'))).not.toBeNull();
    expect(fixture.debugElement.query(By.css('input[formControlName="aforoMaximo"]'))).not.toBeNull();
    expect(enabledHosts.some((host) => enabledPrivilegeKey(host) === 'events.create')).toBeTrue();
    expect(enabledHosts.some((host) => enabledPrivilegeKey(host) === 'events.edit')).toBeTrue();
    expect(editableHosts.some((host) => host.injector.get(UiPrivilegeEditableDirective).privilege === 'event.capacity')).toBeTrue();

    expect(privileges.allows).toHaveBeenCalledWith('events.list', 'VISIBLE');
    expect(privileges.allows).toHaveBeenCalledWith('events.create', 'HABILITADO');
    expect(privileges.allows).toHaveBeenCalledWith('events.edit', 'HABILITADO');
    expect(privileges.allows).toHaveBeenCalledWith('event.summary', 'VISIBLE');
    expect(privileges.allows).toHaveBeenCalledWith('event.form', 'EDITABLE');
    expect(privileges.allows).toHaveBeenCalledWith('event.capacity', 'EDITABLE');
    expect(privileges.allows).toHaveBeenCalledWith('event.submit', 'HABILITADO');
  });

  it('preserves business disabled state while UI policy enables event submit', () => {
    component.abrirModalCrear();
    component.guardando.set(true);
    fixture.detectChanges();

    const submit = fixture.debugElement.query(By.css('button[type="submit"]')).nativeElement as HTMLButtonElement;
    expect(submit.disabled).toBeTrue();
    expect(privileges.allows).toHaveBeenCalledWith('event.submit', 'HABILITADO');
  });

  it('gates responsible assignment without adding per-row static identifiers', () => {
    component.seleccionar(evento);
    component.modalAbierto.set(true);
    fixture.detectChanges();

    const visibleHosts = fixture.debugElement.queryAll(By.directive(UiPrivilegeVisibleDirective));
    expect(visibleHosts.some((host) => host.injector.get(UiPrivilegeVisibleDirective).key === 'event.responsibles.assign')).toBeTrue();
    expect(privileges.allows).toHaveBeenCalledWith('event.responsibles.assign', 'VISIBLE');
    expect(privileges.allows).toHaveBeenCalledWith('event.responsibles.assign', 'EDITABLE');
    expect(fixture.nativeElement.querySelector('[id^="event-responsible-row-"]')).toBeNull();
  });
});
