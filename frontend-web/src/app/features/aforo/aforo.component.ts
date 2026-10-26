import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, OnDestroy, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, catchError, EMPTY, switchMap, auditTime } from 'rxjs';
import { SesionService } from '../../core/sesion.service';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { AforoService, EventoAforo, FiltroAforo, PanelAforo, FilaAforo } from './aforo.service';

@Component({ selector: 'app-aforo', standalone: true, imports: [CommonModule, FormsModule], providers: [AforoService],
  templateUrl: './aforo.component.html', styleUrl: './aforo.component.css', changeDetection: ChangeDetectionStrategy.OnPush })
export class AforoComponent implements OnDestroy {
  readonly servicio = inject(AforoService); private readonly sesion = inject(SesionService);
  readonly datos = signal<PanelAforo | null>(null); readonly eventos = signal<EventoAforo[]>([]); readonly categorias = signal<string[]>([]);
  readonly cargando = signal(false); readonly exportando = signal(false); readonly error = signal(''); readonly pagina = signal(0);
  readonly guardando = signal<number | null>(null);
  eventoId = ''; salaId = ''; categoria: string | null = null; desde = ''; hasta = '';
  readonly salas = computed(() => [...new Map(this.eventos().filter(e => e.salaId !== null).map(e => [e.salaId, { id: e.salaId!, nombre: e.sala! }])).values()]);
  readonly totales = computed(() => (this.datos()?.resumen ?? []).reduce((a,r) => ({ inscritos:a.inscritos+r.inscritos, presentes:a.presentes+r.presentes, ausentes:a.ausentes+r.ausentes, franja:a.franja+r.ingresosFranja }), { inscritos:0,presentes:0,ausentes:0,franja:0 }));
  private readonly consultas = new Subject<void>(); private readonly subs = new Subscription(); private peticion?: Subscription;
  private opcionesSub?: Subscription; private versionOrg=0;
  private aplicado: FiltroAforo = {};
  constructor() {
    this.subs.add(this.consultas.pipe(switchMap(() => {
      if (!this.sesion.organizacionActivaId() || !this.sesion.usuario() || !this.sesion.puedeConsultarAsistencia()) return EMPTY;
      this.cargando.set(true); this.error.set('');
      return this.servicio.consultar(this.aplicado, this.pagina()).pipe(catchError(e => { this.error.set(obtenerMensajeError(e, 'No se pudo completar la operación')); this.cargando.set(false); return EMPTY; }));
    })).subscribe(d => { this.datos.set(d); this.cargando.set(false); }));
    this.subs.add(this.servicio.cambios.pipe(auditTime(200)).subscribe(() => { this.recargarOpciones(); this.consultas.next(); }));
    effect(() => {
      const org = this.sesion.organizacionActivaId(); const usuario = this.sesion.usuario();
      const autorizado = this.sesion.puedeConsultarAsistencia();
      untracked(() => {
      this.versionOrg++; this.peticion?.unsubscribe(); this.opcionesSub?.unsubscribe();
      this.datos.set(null); this.eventos.set([]); this.categorias.set([]); this.servicio.desconectar();
      this.guardando.set(null); this.exportando.set(false); this.cargando.set(false); this.error.set('');
      if (org && usuario && autorizado) {
        this.eventoId=''; this.salaId=''; this.categoria=null; this.desde=''; this.hasta='';
        this.aplicado={}; this.pagina.set(0); this.recargarOpciones(); this.consultas.next(); this.servicio.conectar(org);
      } else { this.consultas.next(); }
      });
    });
  }
  private recargarOpciones() {
    this.opcionesSub?.unsubscribe();
    this.opcionesSub=this.servicio.opciones().subscribe({ next:o=>{this.eventos.set(o.eventos);this.categorias.set(o.categorias);}, error:e=>this.error.set(obtenerMensajeError(e, 'No se pudo completar la operación')) });
  }
  aplicar() {
    if(this.desde && this.hasta && new Date(this.desde)>=new Date(this.hasta)) { this.error.set('El inicio debe ser anterior al fin de la franja.'); return; }
    this.aplicado={ eventoId:this.eventoId?Number(this.eventoId):undefined, salaId:this.salaId?Number(this.salaId):undefined,
      categoria:this.categoria??undefined, desde:this.desde?new Date(this.desde).toISOString():undefined,
      hasta:this.hasta?new Date(this.hasta).toISOString():undefined };
    this.pagina.set(0); this.consultas.next();
  }
  actualizar() { this.consultas.next(); }
  mover(delta:number) { this.pagina.update(n=>Math.max(0,n+delta)); this.consultas.next(); }
  ancho(p:number|null) { return Math.min(100,Math.max(0,p??0)); }
  etiqueta(alerta:string) { return ({ NORMAL:'Normal', PRECAUCION:'Precaución · 80%', CRITICO:'Crítico · 90%', COMPLETO:'Capacidad alcanzada · 100%', SIN_CAPACIDAD:'Sin capacidad definida' } as Record<string,string>)[alerta]??alerta; }
  clasificar(f:FilaAforo, valor:string) {
    this.guardando.set(f.inscripcionId); this.error.set(''); const version=this.versionOrg;
    this.peticion=this.servicio.clasificar(f.inscripcionId,valor).subscribe({ next:()=>{if(version!==this.versionOrg)return;this.guardando.set(null);this.recargarOpciones();this.consultas.next();}, error:e=>{if(version!==this.versionOrg)return;this.guardando.set(null);this.error.set(obtenerMensajeError(e, 'No se pudo completar la operación'));} });
  }
  exportar(formato:string) {
    this.exportando.set(true); this.error.set(''); const version=this.versionOrg;
    this.peticion=this.servicio.exportar(this.aplicado,formato).subscribe({ next:blob=>{
      if(version!==this.versionOrg)return;
      const url=URL.createObjectURL(blob); const link=document.createElement('a');link.href=url;link.download=`asistencia.${formato}`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);this.exportando.set(false);
    },error:async e=>{ if(version!==this.versionOrg)return;this.exportando.set(false);
      if(e.error instanceof Blob) { try { const body=JSON.parse(await e.error.text()); if(version===this.versionOrg)this.error.set(body.message??'No se pudo exportar'); } catch { this.error.set('No se pudo exportar'); } }
      else this.error.set(obtenerMensajeError(e, 'No se pudo completar la operación'));
    }});
  }
  ngOnDestroy() { this.versionOrg++;this.subs.unsubscribe();this.peticion?.unsubscribe();this.opcionesSub?.unsubscribe();this.servicio.desconectar(); }
}
