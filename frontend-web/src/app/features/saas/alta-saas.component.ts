import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { validadorClaveSegura } from '../../core/validacion-clave';
import { RequisitosClaveComponent } from '../../shared/requisitos-clave.component';
import { AltaSaasService, PlanSaas } from './alta-saas.service';

export interface EventoVitrina {
  id: number;
  nombre: string;
  descripcion: string;
  categoria: string;
  fechaInicio: string;
  fechaFin?: string;
  lugar: string;
  aforoMaximo: number;
  inscritos: number;
  imagen: string;
  destacadoBadge?: string;
  estado: string;
}

export interface ModuloPlataforma {
  numero: string;
  titulo: string;
  descripcion: string;
  icono: string;
  destacados: string[];
}

type VistaAlta = 'planes' | 'correo' | 'verificar' | 'activar' | 'pago';

@Component({
  selector: 'app-alta-saas',
  imports: [ReactiveFormsModule, FormsModule, RouterLink, DecimalPipe, DatePipe, RequisitosClaveComponent],
  templateUrl: './alta-saas.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AltaSaasComponent implements OnInit {
  private readonly api = inject(AltaSaasService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder).nonNullable;

  readonly planes = signal<PlanSaas[]>([]);
  readonly vista = signal<VistaAlta>('planes');
  readonly planSeleccionado = signal<PlanSaas | null>(null);
  readonly correoVerificacion = signal('');
  readonly token = signal('');
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly activacionEnviada = signal(false);

  // ==================== VITRINA DE EVENTOS DESTACADOS ====================
  readonly eventosShowcase: EventoVitrina[] = [
    {
      id: 101,
      nombre: 'Cumbre Internacional de Inteligencia Artificial y Datos 2026',
      descripcion:
        'Conferencias magistrales con líderes de tecnología, paneles sobre modelos generativos, visión por computador y networking de alta dirección.',
      categoria: 'Tecnología',
      fechaInicio: '2026-11-14T09:00:00',
      fechaFin: '2026-11-16T18:00:00',
      lugar: 'Auditorio Central y Salas de Innovación',
      aforoMaximo: 650,
      inscritos: 540,
      imagen: 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1000&q=80',
      destacadoBadge: 'Más Esperado',
      estado: 'PUBLICADO',
    },
    {
      id: 102,
      nombre: 'Festival de Arte, Experiencias Digitales y Cultura Urbana',
      descripcion:
        'Exposiciones inmersivas, música en vivo, talleres de diseño y muestras interactivas de creadores contemporáneos en un espacio multidisciplinario.',
      categoria: 'Cultura',
      fechaInicio: '2026-10-28T16:00:00',
      fechaFin: '2026-10-30T23:00:00',
      lugar: 'Pabellón Creativo y Ágora Abierta',
      aforoMaximo: 1200,
      inscritos: 1020,
      imagen: 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1000&q=80',
      destacadoBadge: 'Próximamente',
      estado: 'PUBLICADO',
    },
    {
      id: 103,
      nombre: 'Simposio Global de Sostenibilidad y Transición Energética',
      descripcion:
        'Debates científicos y políticas públicas con expertos internacionales sobre mitigación climática, energías limpias y economía circular.',
      categoria: 'Académico',
      fechaInicio: '2026-11-05T08:30:00',
      fechaFin: '2026-11-06T17:30:00',
      lugar: 'Sala Magistral B - Campus Central',
      aforoMaximo: 350,
      inscritos: 310,
      imagen: 'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1000&q=80',
      destacadoBadge: 'Inscripción Abierta',
      estado: 'PUBLICADO',
    },
    {
      id: 104,
      nombre: 'Hackathon Nacional de Desarrollo Ágil y Ciberseguridad',
      descripcion:
        '48 horas continuas de creación de prototipos tecnológicos, mentorías con especialistas y premios para los mejores equipos multidisciplinarios.',
      categoria: 'Tecnología',
      fechaInicio: '2026-12-02T10:00:00',
      fechaFin: '2026-12-04T14:00:00',
      lugar: 'Centro de Co-Working y Laboratorios',
      aforoMaximo: 200,
      inscritos: 185,
      imagen: 'https://images.unsplash.com/photo-1515187029135-18ee286d815b?auto=format&fit=crop&w=1000&q=80',
      destacadoBadge: 'Cupos Limitados',
      estado: 'PUBLICADO',
    },
    {
      id: 105,
      nombre: 'Encuentro de Liderazgo Corporativo & Capital de Riesgo',
      descripcion:
        'Conexión estratégica entre fundadores, inversionistas ángeles y directivos de empresas para explorar rondas de inversión y alianzas de negocio.',
      categoria: 'Corporativo',
      fechaInicio: '2026-11-20T14:00:00',
      fechaFin: '2026-11-21T20:00:00',
      lugar: 'Gran Salón de Convenciones',
      aforoMaximo: 400,
      inscritos: 330,
      imagen: 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=1000&q=80',
      destacadoBadge: 'Exclusivo',
      estado: 'PUBLICADO',
    },
    {
      id: 106,
      nombre: 'Congreso Internacional de Ciencias de la Salud y Bioética',
      descripcion:
        'Ponencias sobre avances médicos, telemedicina, investigación clínica y bioética aplicada con especialistas de Latinoamérica y Europa.',
      categoria: 'Académico',
      fechaInicio: '2026-11-25T08:00:00',
      fechaFin: '2026-11-27T18:00:00',
      lugar: 'Auditorio de Ciencias Médicas',
      aforoMaximo: 500,
      inscritos: 410,
      imagen: 'https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=1000&q=80',
      destacadoBadge: 'Certificado Oficial',
      estado: 'PUBLICADO',
    },
  ];

  readonly eventos = signal<EventoVitrina[]>(this.eventosShowcase);
  readonly filtroTexto = signal('');
  readonly categoriaActiva = signal('TODAS');
  readonly eventoModal = signal<EventoVitrina | null>(null);

  readonly categoriasDisponibles = computed(() => {
    const conjunto = new Set<string>();
    for (const ev of this.eventos()) {
      if (ev.categoria) conjunto.add(ev.categoria);
    }
    return ['TODAS', ...Array.from(conjunto).sort()];
  });

  readonly eventosFiltrados = computed(() => {
    const texto = this.filtroTexto().trim().toLowerCase();
    const cat = this.categoriaActiva();

    return this.eventos().filter((ev) => {
      const coincideCat = cat === 'TODAS' || ev.categoria.toLowerCase() === cat.toLowerCase();
      const coincideTexto =
        !texto ||
        ev.nombre.toLowerCase().includes(texto) ||
        ev.descripcion.toLowerCase().includes(texto) ||
        ev.lugar.toLowerCase().includes(texto);
      return coincideCat && coincideTexto;
    });
  });

  // ==================== MÓDULOS DEL SISTEMA ====================
  readonly modulos: ModuloPlataforma[] = [
    {
      numero: '01',
      titulo: 'Gestión Multisede y Espacios',
      descripcion: 'Administración de recintos físicos, auditorios y salas con control automático de solapamiento.',
      icono: 'M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
      destacados: ['Control de aforos', 'Disponibilidad en calendario', 'Asignación de recursos técnicos'],
    },
    {
      numero: '02',
      titulo: 'Convocatorias y Ponencias',
      descripcion: 'Recepción de postulaciones públicas, evaluación ciega por comités y publicación de agenda.',
      icono: 'M9 12h6m-6 4h6m2 5H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5.5L19 7.5V19a2 2 0 0 1-2 2z',
      destacados: ['Llamados a ponentes', 'Criterios de evaluación', 'Notificaciones de aceptación'],
    },
    {
      numero: '03',
      titulo: 'Acreditación y Aforo en Vivo',
      descripcion: 'Credenciales digitales con código QR para escaneo rápido en puerta y control de concurrencia.',
      icono: 'M12 4v1m6 11h2m-6 0a2 2 0 1 0 0-4 2 2 0 0 0 0 4zm-8-7a2 2 0 1 1 4 0 2 2 0 0 1-4 0z',
      destacados: ['Pases QR seguros', 'Validación sin demoras', 'Reporte de asistencia real'],
    },
    {
      numero: '04',
      titulo: 'Roles y Privilegios Granulares',
      descripcion: 'Matriz RBAC por organización: organizadores, staff de apoyo, evaluadores y participantes.',
      icono: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z',
      destacados: ['Aislamiento multi-tenant', 'Auditoría de acciones', 'Permisos por pantalla'],
    },
    {
      numero: '05',
      titulo: 'Analítica y Reportes con IA',
      descripcion: 'Resúmenes automáticos de desempeño, estimaciones de interés y reportes ejecutivos listos.',
      icono: 'M3 3v18h18 M9 17V9m4 8V5m4 12v-6',
      destacados: ['Métricas consolidadas', 'Exportación en CSV y PDF', 'Generación asistida por IA'],
    },
    {
      numero: '06',
      titulo: 'SaaS Seguro y Facturación',
      descripcion: 'Separación estricta de inquilinos, pagos en bolivianos o Stripe y autoservicio transparente.',
      icono: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z',
      destacados: ['Pagos seguros en línea', 'Activación por correo', 'Trazabilidad en bitácora'],
    },
  ];

  // ==================== SIMULADOR / CALCULADORA INTERACTIVA ====================
  readonly calcEventos = signal<number>(4);
  readonly calcAsistentes = signal<number>(300);
  readonly calcCiclo = signal<'MENSUAL' | 'ANUAL'>('MENSUAL');

  readonly calcInscripcionesTotales = computed(() => this.calcEventos() * this.calcAsistentes());

  readonly planRecomendadoCodigo = computed(() => {
    const total = this.calcInscripcionesTotales();
    const eventos = this.calcEventos();
    if (total <= 250 && eventos <= 2) return 'BASICO';
    if (total <= 1500 && eventos <= 8) return 'PROFESIONAL';
    return 'EMPRESARIAL';
  });

  readonly planRecomendado = computed(() => {
    const codigo = this.planRecomendadoCodigo();
    return this.planes().find((p) => p.codigo === codigo) ?? null;
  });

  // ==================== PREGUNTAS FRECUENTES (FAQ) ====================
  readonly faqAbierto = signal<number | null>(0);

  readonly faqs = [
    {
      pregunta: '¿Cómo garantiza la plataforma el aislamiento de los datos de mi organización?',
      respuesta:
        'Cada organización funciona bajo una arquitectura multi-tenant estricta. Tus usuarios, eventos, postulaciones y registros están completamente aislados y protegidos a nivel de base de datos y autorización.',
    },
    {
      pregunta: '¿Puedo empezar con un plan sin costo y actualizar después?',
      respuesta:
        'Sí. El plan Básico te permite explorar la plataforma y organizar eventos iniciales sin costo alguno. Cuando tu volumen crezca, puedes cambiarte a un plan superior en cualquier momento conservando toda tu información.',
    },
    {
      pregunta: '¿Cómo funciona el pago mediante Stripe y la facturación?',
      respuesta:
        'Para los planes con costo, tras verificar tu correo electrónico serás redirigido a Stripe Checkout, donde podrás pagar con tarjeta de débito o crédito de manera 100% segura. Una vez confirmado, tu cuenta se activa inmediatamente.',
    },
    {
      pregunta: '¿La acreditación de asistentes requiere equipamiento especial?',
      respuesta:
        'No. La verificación de credenciales con código QR funciona directamente desde cualquier teléfono móvil o tableta con cámara a través del navegador web o nuestra aplicación móvil, sin necesidad de tótems costosos.',
    },
    {
      pregunta: '¿Qué tipo de reportes puedo exportar para patrocinadores o autoridades?',
      respuesta:
        'Puedes generar reportes de asistencia en tiempo real, desglose demográfico, estados de postulaciones y resúmenes ejecutivos enriquecidos con análisis de inteligencia artificial listos para descargar o enviar por correo.',
    },
  ];

  // ==================== FORMULARIOS DE ALTA Y ACTIVACIÓN ====================
  readonly formularioAlta = this.fb.group({
    nombreEmpresa: ['', [Validators.required, Validators.maxLength(150)]],
    slugSugerido: ['', [Validators.maxLength(100), Validators.pattern(/^[a-z0-9]+(?:-[a-z0-9]+)*$/)]],
    nombresContacto: ['', [Validators.required, Validators.maxLength(100)]],
    apellidosContacto: ['', [Validators.required, Validators.maxLength(100)]],
    correoContacto: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
  });

  readonly formularioActivacion = this.fb.group({
    nuevaClave: ['', [Validators.required, Validators.maxLength(72), validadorClaveSegura()]],
  });
  readonly claveActivacion = signal('');
  readonly mostrarClave = signal(false);

  ngOnInit() {
    this.formularioActivacion.controls.nuevaClave.valueChanges.subscribe((v) => {
      this.claveActivacion.set(v);
    });

    const path = this.route.snapshot.routeConfig?.path;
    this.token.set(this.route.snapshot.queryParamMap.get('token') ?? '');

    if (path === 'verificar-alta-saas') {
      this.vista.set('verificar');
      this.verificarCorreo();
    } else if (path === 'activar-cuenta-saas') {
      this.vista.set('activar');
      if (!this.token()) this.error.set('El enlace de activación está incompleto.');
    } else if (path === 'pago/completado') {
      this.vista.set('pago');
    } else {
      this.api.planes().subscribe({
        next: (planes) => this.planes.set(planes),
        error: (error) => this.error.set(obtenerMensajeError(error, 'No se pudo cargar el catálogo de planes.')),
      });
      if (this.route.snapshot.queryParamMap.get('checkout') === 'cancelado') {
        this.aviso.set('El checkout se canceló. Abre de nuevo el enlace de verificación que enviamos a tu correo para intentarlo otra vez.');
      }
    }
  }

  seleccionarPlan(plan: PlanSaas) {
    this.planSeleccionado.set(plan);
    this.formularioAlta.reset({ nombreEmpresa: '', slugSugerido: '', nombresContacto: '', apellidosContacto: '', correoContacto: '' });
    this.error.set('');
    this.aviso.set('');
    window.requestAnimationFrame(() =>
      document.getElementById('alta-formulario')?.scrollIntoView({ behavior: 'smooth', block: 'start' }),
    );
  }

  elegirPlanPorCodigo(codigo: string) {
    const plan = this.planes().find((p) => p.codigo === codigo);
    if (plan) {
      this.seleccionarPlan(plan);
    } else {
      this.hacerScrollA('planes');
    }
  }

  enviarAlta() {
    const plan = this.planSeleccionado();
    if (!plan || this.cargando()) return;
    if (this.formularioAlta.invalid) {
      this.formularioAlta.markAllAsTouched();
      this.error.set('Revisa los campos obligatorios para continuar.');
      return;
    }
    this.cargando.set(true);
    this.error.set('');
    const datos = this.formularioAlta.getRawValue();
    this.api.iniciar({ ...datos, plan: plan.codigo }).subscribe({
      next: (respuesta) => {
        this.aviso.set(respuesta.mensaje);
        this.correoVerificacion.set(datos.correoContacto);
        this.vista.set('correo');
      },
      error: (error) => {
        this.error.set(obtenerMensajeError(error, 'No se pudo iniciar el alta.'));
        this.cargando.set(false);
      },
      complete: () => this.cargando.set(false),
    });
  }

  private verificarCorreo() {
    if (!this.token()) {
      this.error.set('El enlace de verificación está incompleto.');
      return;
    }
    this.cargando.set(true);
    this.api.verificar(this.token()).subscribe({
      next: (respuesta) => {
        this.aviso.set(respuesta.mensaje);
        if (respuesta.checkoutRequerido && respuesta.checkoutUrl) {
          window.location.assign(respuesta.checkoutUrl);
        } else {
          this.activacionEnviada.set(true);
        }
      },
      error: (error) => {
        this.error.set(obtenerMensajeError(error, 'No se pudo verificar el correo.'));
        this.cargando.set(false);
      },
      complete: () => this.cargando.set(false),
    });
  }

  activarPropietario() {
    if (this.formularioActivacion.invalid || this.cargando()) {
      this.formularioActivacion.markAllAsTouched();
      return;
    }
    this.cargando.set(true);
    this.error.set('');
    this.api.activar(this.token(), this.formularioActivacion.controls.nuevaClave.value).subscribe({
      next: () => this.activacionEnviada.set(true),
      error: (error) => {
        this.error.set(obtenerMensajeError(error, 'No se pudo activar la cuenta.'));
        this.cargando.set(false);
      },
      complete: () => this.cargando.set(false),
    });
  }

  // ==================== MÉTODOS DE INTERACCIÓN ====================
  seleccionarCategoria(cat: string): void {
    this.categoriaActiva.set(cat);
  }

  abrirModalEvento(evento: EventoVitrina): void {
    this.eventoModal.set(evento);
  }

  cerrarModalEvento(): void {
    this.eventoModal.set(null);
  }

  porcentajeOcupacion(evento: EventoVitrina): number {
    if (!evento.aforoMaximo || evento.aforoMaximo <= 0) return 0;
    return Math.min(100, Math.round((evento.inscritos / evento.aforoMaximo) * 100));
  }

  toggleFaq(index: number): void {
    this.faqAbierto.update((actual) => (actual === index ? null : index));
  }

  actualizarCalcEventos(event: Event): void {
    const val = Number((event.target as HTMLInputElement).value);
    this.calcEventos.set(val);
  }

  actualizarCalcAsistentes(event: Event): void {
    const val = Number((event.target as HTMLInputElement).value);
    this.calcAsistentes.set(val);
  }

  setCiclo(ciclo: 'MENSUAL' | 'ANUAL'): void {
    this.calcCiclo.set(ciclo);
  }

  hacerScrollA(id: string): void {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
}
