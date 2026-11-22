import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CertificadosService } from './servicios/certificados.service';
import { CertificadoRespuesta } from './modelos/certificado.modelo';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-mis-certificados',
  standalone: true,
  imports: [CommonModule, RouterModule, DatePipe],
  template: `
    <div class="certificados-contenedor">
      <div class="encabezado-seccion">
        <div>
          <h2>Mis Certificados de Asistencia</h2>
          <p class="subtitulo">Descarga y valida tus certificaciones oficiales emitidas por tu participación en eventos.</p>
        </div>
        <button class="btn-secundario" (click)="cargarCertificados()">
          <span class="icono">🔄</span> Actualizar
        </button>
      </div>

      <div *ngIf="cargando()" class="estado-mensaje">
        <div class="spinner"></div>
        <p>Cargando tus certificados...</p>
      </div>

      <div *ngIf="!cargando() && error()" class="alerta alerta-error">
        {{ error() }}
      </div>

      <div *ngIf="!cargando() && certificados().length === 0 && !error()" class="vacio-contenedor">
        <div class="icono-vacio">🎓</div>
        <h3>No tienes certificados emitidos aún</h3>
        <p>Los certificados se generan automáticamente una vez que el staff registra tu asistencia efectiva al evento.</p>
      </div>

      <div *ngIf="!cargando() && certificados().length > 0" class="cuadricula-certificados">
        <div *ngFor="let cert of certificados()" class="tarjeta-certificado">
          <div class="tarjeta-cabecera">
            <span class="insignia-oficial">✓ Certificado Oficial</span>
            <span class="fecha-emision">{{ cert.fechaEmision | date:'dd/MM/yyyy' }}</span>
          </div>
          <div class="tarjeta-cuerpo">
            <h3 class="titulo-evento">{{ cert.tituloEvento }}</h3>
            <p class="nombre-org">🏢 {{ cert.nombreOrganizacion }}</p>
            <div class="datos-meta">
              <span class="etiqueta-codigo">Código: <strong>{{ cert.codigoVerificacion }}</strong></span>
            </div>
          </div>
          <div class="tarjeta-acciones">
            <button 
              class="btn-descargar" 
              [disabled]="descargandoId() === cert.idCertificado" 
              (click)="descargar(cert)">
              <span *ngIf="descargandoId() !== cert.idCertificado">📥 Descargar PDF</span>
              <span *ngIf="descargandoId() === cert.idCertificado">Generando PDF...</span>
            </button>
            <a [routerLink]="['/verificar-certificado', cert.codigoVerificacion]" target="_blank" class="enlace-verificar">
              🔍 Validar Autenticidad
            </a>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .certificados-contenedor {
      max-width: 1100px;
      margin: 0 auto;
      padding: 1.5rem;
    }
    .encabezado-seccion {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 2rem;
      border-bottom: 1px solid #e2e8f0;
      padding-bottom: 1rem;
    }
    .encabezado-seccion h2 {
      font-size: 1.6rem;
      font-weight: 700;
      color: #1e293b;
      margin: 0 0 0.25rem 0;
    }
    .subtitulo {
      color: #64748b;
      font-size: 0.95rem;
      margin: 0;
    }
    .btn-secundario {
      background: #f1f5f9;
      border: 1px solid #cbd5e1;
      color: #334155;
      padding: 0.5rem 1rem;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 500;
      transition: background 0.2s;
    }
    .btn-secundario:hover {
      background: #e2e8f0;
    }
    .cuadricula-certificados {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 1.5rem;
    }
    .tarjeta-certificado {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 10px;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      border-top: 4px solid #2563eb;
    }
    .tarjeta-cabecera {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.75rem;
    }
    .insignia-oficial {
      background: #ecfdf5;
      color: #059669;
      font-size: 0.75rem;
      font-weight: 600;
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      border: 1px solid #a7f3d0;
    }
    .fecha-emision {
      color: #94a3b8;
      font-size: 0.8rem;
    }
    .titulo-evento {
      font-size: 1.15rem;
      color: #0f172a;
      margin: 0 0 0.5rem 0;
      line-height: 1.3;
    }
    .nombre-org {
      color: #475569;
      font-size: 0.9rem;
      margin: 0 0 0.75rem 0;
    }
    .datos-meta {
      background: #f8fafc;
      padding: 0.5rem;
      border-radius: 6px;
      font-size: 0.8rem;
      color: #64748b;
      margin-bottom: 1rem;
    }
    .tarjeta-acciones {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }
    .btn-descargar {
      background: #2563eb;
      color: white;
      border: none;
      padding: 0.65rem 1rem;
      border-radius: 6px;
      font-weight: 600;
      font-size: 0.9rem;
      cursor: pointer;
      display: flex;
      justify-content: center;
      align-items: center;
      transition: background 0.2s;
    }
    .btn-descargar:hover:not(:disabled) {
      background: #1d4ed8;
    }
    .btn-descargar:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .enlace-verificar {
      text-align: center;
      color: #64748b;
      font-size: 0.8rem;
      text-decoration: none;
      padding: 0.25rem;
    }
    .enlace-verificar:hover {
      color: #2563eb;
      text-decoration: underline;
    }
    .vacio-contenedor {
      text-align: center;
      padding: 4rem 1rem;
      background: #f8fafc;
      border-radius: 12px;
      border: 2px dashed #cbd5e1;
    }
    .icono-vacio {
      font-size: 3rem;
      margin-bottom: 1rem;
    }
    .estado-mensaje {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 3rem;
      color: #64748b;
    }
    .spinner {
      border: 3px solid #f3f3f3;
      border-top: 3px solid #2563eb;
      border-radius: 50%;
      width: 28px;
      height: 28px;
      animation: spin 1s linear infinite;
      margin-bottom: 1rem;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    .alerta-error {
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #b91c1c;
      padding: 1rem;
      border-radius: 6px;
      margin-bottom: 1.5rem;
    }
  `]
})
export class MisCertificadosComponent implements OnInit {
  private readonly certificadosService = inject(CertificadosService);
  private readonly toast = inject(ToastService);

  readonly certificados = signal<CertificadoRespuesta[]>([]);
  readonly cargando = signal<boolean>(true);
  readonly descargandoId = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargarCertificados();
  }

  cargarCertificados(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.certificadosService.obtenerMisCertificados().subscribe({
      next: (datos) => {
        this.certificados.set(datos);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set('No fue posible cargar tus certificados. Verifica tu conexión.');
        this.cargando.set(false);
      }
    });
  }

  descargar(cert: CertificadoRespuesta): void {
    this.descargandoId.set(cert.idCertificado);
    this.certificadosService.descargarPdf(cert.idCertificado).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Certificado_${cert.codigoVerificacion}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.descargandoId.set(null);
        this.toast.exito('Certificado descargado con éxito');
      },
      error: () => {
        this.descargandoId.set(null);
        this.toast.error('Ocurrió un error al generar el PDF del certificado');
      }
    });
  }
}
