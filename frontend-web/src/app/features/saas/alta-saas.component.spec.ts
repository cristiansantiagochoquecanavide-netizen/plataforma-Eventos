import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AltaSaasService, PlanSaas } from './alta-saas.service';
import { AltaSaasComponent } from './alta-saas.component';

const planServidor: PlanSaas = {
  codigo: 'EQUIPO_PLUS',
  nombre: 'Equipo Plus',
  precioMensualBob: 777,
  gratuito: false,
  usuariosInternos: 42,
  eventosActivos: 17,
  inscripcionesMes: 4200,
  reportes: ['CSV'],
};

describe('AltaSaasComponent', () => {
  let fixture: ComponentFixture<AltaSaasComponent>;
  let servicio: jasmine.SpyObj<AltaSaasService>;

  beforeEach(async () => {
    servicio = jasmine.createSpyObj<AltaSaasService>('AltaSaasService', ['planes', 'iniciar', 'verificar', 'activar']);
    servicio.planes.and.returnValue(of([planServidor]));
    servicio.iniciar.and.returnValue(of({ folio: 'folio-demo', mensaje: 'Revisa tu correo para continuar.' }));

    await TestBed.configureTestingModule({
      imports: [AltaSaasComponent],
      providers: [
        provideRouter([]),
        { provide: AltaSaasService, useValue: servicio },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AltaSaasComponent);
  });

  it('usa exclusivamente los planes devueltos por el backend', () => {
    fixture.detectChanges();

    expect(servicio.planes).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelectorAll('.plan-card').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Equipo Plus');
    expect(fixture.nativeElement.textContent).toContain('Bs 777');
    expect(fixture.nativeElement.textContent).not.toContain('Profesional');
  });

  it('no ofrece planes locales alternativos si falla el catálogo del backend', () => {
    servicio.planes.and.returnValue(throwError(() => new Error('backend no disponible')));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.plan-card').length).toBe(0);
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
  });

  it('explica que la verificación del correo redirige al checkout de Stripe', () => {
    fixture.detectChanges();
    fixture.componentInstance.seleccionarPlan(planServidor);
    fixture.componentInstance.formularioAlta.patchValue({
      nombreEmpresa: 'Equipo de prueba',
      slugSugerido: 'equipo-prueba',
      nombresContacto: 'Ana',
      apellidosContacto: 'Prueba',
      correoContacto: 'ana@example.test',
    });

    fixture.componentInstance.enviarAlta();
    fixture.detectChanges();

    expect(servicio.iniciar).toHaveBeenCalledWith({
      nombreEmpresa: 'Equipo de prueba',
      slugSugerido: 'equipo-prueba',
      nombresContacto: 'Ana',
      apellidosContacto: 'Prueba',
      correoContacto: 'ana@example.test',
      plan: 'EQUIPO_PLUS',
    });
    expect(servicio.verificar).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('ana@example.test');
    expect(fixture.nativeElement.textContent).toContain('te redirigiremos a Stripe Checkout');
  });

  it('explica que el plan gratuito continúa con la activación y no con un cobro', () => {
    const planGratis = { ...planServidor, codigo: 'BASICO', nombre: 'Básico', precioMensualBob: 0, gratuito: true };
    servicio.planes.and.returnValue(of([planGratis]));
    fixture.detectChanges();
    fixture.componentInstance.seleccionarPlan(planGratis);
    fixture.componentInstance.formularioAlta.patchValue({
      nombreEmpresa: 'Equipo gratuito',
      slugSugerido: 'equipo-gratuito',
      nombresContacto: 'Ana',
      apellidosContacto: 'Prueba',
      correoContacto: 'ana@example.test',
    });

    fixture.componentInstance.enviarAlta();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('te enviaremos otro enlace para activar tu cuenta');
    expect(fixture.nativeElement.textContent).not.toContain('Stripe Checkout');
  });

  it('filtra los eventos destacados por categoria y por texto de busqueda', () => {
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    expect(comp.eventos().length).toBeGreaterThan(0);
    expect(comp.eventosFiltrados().length).toBe(comp.eventos().length);

    comp.seleccionarCategoria('Tecnología');
    expect(comp.eventosFiltrados().every((e) => e.categoria === 'Tecnología')).toBeTrue();

    comp.filtroTexto.set('Inteligencia');
    expect(comp.eventosFiltrados().length).toBeGreaterThan(0);
    expect(comp.eventosFiltrados()[0].nombre).toContain('Inteligencia Artificial');

    comp.filtroTexto.set('texto-imposible-que-no-existe');
    expect(comp.eventosFiltrados().length).toBe(0);
  });

  it('permite abrir y cerrar el modal de detalle del evento', () => {
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    const evento = comp.eventos()[0];

    comp.abrirModalEvento(evento);
    fixture.detectChanges();

    expect(comp.eventoModal()).toEqual(evento);
    expect(fixture.nativeElement.querySelector('.modal-backdrop')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.modal-body h2').textContent).toContain(evento.nombre);

    comp.cerrarModalEvento();
    fixture.detectChanges();

    expect(comp.eventoModal()).toBeNull();
    expect(fixture.nativeElement.querySelector('.modal-backdrop')).toBeNull();
  });

  it('calcula la capacidad requerida y sugiere el plan correspondiente en el simulador', () => {
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.calcEventos.set(1);
    comp.calcAsistentes.set(100);
    expect(comp.calcInscripcionesTotales()).toBe(100);
    expect(comp.planRecomendadoCodigo()).toBe('BASICO');

    comp.calcEventos.set(4);
    comp.calcAsistentes.set(250);
    expect(comp.calcInscripcionesTotales()).toBe(1000);
    expect(comp.planRecomendadoCodigo()).toBe('PROFESIONAL');

    comp.calcEventos.set(12);
    comp.calcAsistentes.set(500);
    expect(comp.calcInscripcionesTotales()).toBe(6000);
    expect(comp.planRecomendadoCodigo()).toBe('EMPRESARIAL');
  });

  it('alterna el acordeon de preguntas frecuentes', () => {
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    expect(comp.faqAbierto()).toBe(0);
    comp.toggleFaq(1);
    expect(comp.faqAbierto()).toBe(1);

    comp.toggleFaq(1);
    expect(comp.faqAbierto()).toBeNull();
  });
});
