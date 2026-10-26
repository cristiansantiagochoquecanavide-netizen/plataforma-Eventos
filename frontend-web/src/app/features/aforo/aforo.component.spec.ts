import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Subject, of } from 'rxjs';
import { AforoComponent } from './aforo.component';
import { AforoService, PanelAforo } from './aforo.service';
import { SesionService } from '../../core/sesion.service';

describe('CU24 monitor', () => {
  let fixture: ComponentFixture<AforoComponent>;
  let service: jasmine.SpyObj<AforoService>;
  let cambios: Subject<void>; let org: ReturnType<typeof signal<number | null>>;
  const panel: PanelAforo={resumen:[{eventoId:1,evento:'Congreso',salaId:2,sala:'Auditorio',capacidad:10,inscritos:20,presentes:12,ausentes:8,ingresosFranja:12,ingresosSala:12,porcentajeAsistencia:60,porcentajeAforo:120,alerta:'COMPLETO'}],nomina:{filas:[],total:60,pagina:0,tamano:25},actualizado:'2026-10-26T12:00:00Z'};
  beforeEach(async()=>{
    cambios=new Subject<void>();org=signal<number|null>(1);
    service=jasmine.createSpyObj<AforoService>('AforoService',['opciones','consultar','conectar','desconectar','exportar','clasificar'],{estado:signal('En vivo'),cambios});
    service.opciones.and.returnValue(of({eventos:[],categorias:[]}));service.consultar.and.returnValue(of(panel));
    await TestBed.configureTestingModule({imports:[AforoComponent],providers:[{provide:SesionService,useValue:{organizacionActivaId:org,usuario:signal({id:1}),puedeConsultarAsistencia:()=>true}}]})
      .overrideComponent(AforoComponent,{set:{providers:[{provide:AforoService,useValue:service}]}}).compileComponents();
    fixture=TestBed.createComponent(AforoComponent);fixture.detectChanges();
  });
  it('muestra aforo sobre 100 y explica que es acumulado',()=>{
    expect(fixture.nativeElement.textContent).toContain('120%');expect(fixture.nativeElement.textContent).toContain('no descuenta salidas');
    expect(fixture.nativeElement.querySelector('[data-alerta="COMPLETO"]')).toBeTruthy();
  });
  it('actualiza tras aviso sin duplicar contadores',fakeAsync(()=>{
    const count=service.consultar.calls.count();cambios.next();cambios.next();tick(201);fixture.detectChanges();
    expect(service.consultar.calls.count()).toBe(count+1);expect(fixture.componentInstance.totales().presentes).toBe(12);
  }));
  it('pagina sin que el efecto reinicie filtros',()=>{
    fixture.componentInstance.eventoId='1';fixture.componentInstance.categoria='VIP';fixture.componentInstance.aplicar();fixture.detectChanges();
    fixture.componentInstance.mover(1);fixture.detectChanges();
    expect(fixture.componentInstance.pagina()).toBe(1);expect(service.consultar.calls.mostRecent().args[0].categoria).toBe('VIP');
  });
  it('descarta respuestas antiguas al cambiar filtros y organización',()=>{
    const old=new Subject<PanelAforo>();service.consultar.and.returnValue(old);fixture.componentInstance.actualizar();
    service.consultar.and.returnValue(of({...panel,resumen:[]}));org.set(2);fixture.detectChanges();old.next(panel);
    expect(fixture.componentInstance.datos()?.resumen.length).toBe(0);expect(service.conectar).toHaveBeenCalledWith(2);
  });
  it('rechaza rango horario invertido',()=>{
    fixture.componentInstance.desde='2026-10-26T11:00';fixture.componentInstance.hasta='2026-10-26T10:00';
    const count=service.consultar.calls.count();fixture.componentInstance.aplicar();expect(service.consultar.calls.count()).toBe(count);expect(fixture.componentInstance.error()).toContain('anterior');
  });
});
