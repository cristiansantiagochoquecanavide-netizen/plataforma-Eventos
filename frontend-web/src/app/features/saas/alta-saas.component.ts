import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { obtenerMensajeError } from '../../core/mensajes-error';
import { validadorClaveSegura } from '../../core/validacion-clave';
import { AltaSaasService, PlanSaas } from './alta-saas.service';

type VistaAlta = 'planes' | 'verificar' | 'activar' | 'pago';

@Component({
  selector: 'app-alta-saas',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe],
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
  readonly token = signal('');
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly aviso = signal('');
  readonly activacionEnviada = signal(false);

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

  ngOnInit() {
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
        this.planSeleccionado.set(null);
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
}
