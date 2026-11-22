import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CertificadosService } from './servicios/certificados.service';
import { VerificacionCertificadoRespuesta } from './modelos/certificado.modelo';

@Component({
  selector: 'app-verificar-certificado',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, DatePipe],
  template: `
    <div class="verificacion-contenedor">
      <div class="tarjeta-verificacion">
        <div class="logo-area">
          <span class="icono-portal">🎓</span>
          <h1>Portal de Validación de Certificados</h1>
          <p class="descripcion-portal">Sistema Oficial de Verificación y Validación de Autenticidad Digital</p>
        </div>

        <div class="formulario-busqueda">
          <label for="codigoInput" class="etiqueta-input">Código Único de Verificación</label>
          <div class="grupo-input">
            <input 
              id="codigoInput" 
              type="text" 
              [(ngModel)]="codigoIngresado" 
              placeholder="Ej: CERT-ABCD-1234-EF56" 
              (keyup.enter)="consultar()" 
              [disabled]="cargando()"
            />
            <button class="btn-verificar" (click)="consultar()" [disabled]="cargando() || !codigoIngresado.trim()">
              <span *ngIf="!cargando()">🔍 Verificar</span>
              <span *ngIf="cargando()">Comprobando...</span>
            </button>
          </div>
        </div>

        <div *ngIf="cargando()" class="estado-cargando">
          <div class="spinner"></div>
          <p>Comprobando firmas digitales y registro en blockchain/base de datos...</p>
        </div>

        <div *ngIf="!cargando() && resultado()" class="resultado-seccion">
          <!-- Certificado VÁLIDO -->
          <div *ngIf="resultado()?.valido" class="panel-valido">
            <div class="cabecera-valido">
              <span class="icono-check">✓</span>
              <div>
                <h2>CERTIFICADO OFICIAL VÁLIDO</h2>
                <p>El documento digital ha sido verificado con éxito en los registros institucionales.</p>
              </div>
            </div>

            <div class="detalles-grid">
              <div class="campo-detalle">
                <span class="label">Participante Acreditado:</span>
                <span class="valor nombre-destacado">{{ resultado()?.nombreParticipante }}</span>
              </div>
              <div class="campo-detalle">
                <span class="label">Evento:</span>
                <span class="valor">{{ resultado()?.tituloEvento }}</span>
              </div>
              <div class="campo-detalle">
                <span class="label">Organización Emisora:</span>
                <span class="valor">{{ resultado()?.nombreOrganizacion }}</span>
              </div>
              <div class="campo-detalle">
                <span class="label">Fecha de Emisión:</span>
                <span class="valor">{{ resultado()?.fechaEmision | date:'dd/MM/yyyy HH:mm' }}</span>
              </div>
              <div class="campo-detalle full-width">
                <span class="label">Código de Verificación:</span>
                <span class="valor codigo-mono">{{ resultado()?.codigoVerificacion }}</span>
              </div>
              <div class="campo-detalle full-width">
                <span class="label">Hash Criptográfico SHA-256:</span>
                <span class="valor hash-mono">{{ resultado()?.hashSeguridad }}</span>
              </div>
            </div>
          </div>

          <!-- Certificado INVÁLIDO -->
          <div *ngIf="!resultado()?.valido" class="panel-invalido">
            <div class="cabecera-invalido">
              <span class="icono-cruz">✕</span>
              <div>
                <h2>CERTIFICADO NO ENCONTRADO O INVÁLIDO</h2>
                <p>{{ resultado()?.mensaje }}</p>
              </div>
            </div>
            <p class="advertencia-texto">
              Asegúrate de haber ingresado el código alfanumérico exactamente como figura en el documento o prueba escaneando nuevamente el código QR.
            </p>
          </div>
        </div>

        <div class="pie-tarjeta">
          <a routerLink="/" class="enlace-inicio">← Volver al portal principal</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .verificacion-contenedor {
      min-height: 100vh;
      background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 2rem 1rem;
      font-family: system-ui, -apple-system, sans-serif;
    }
    .tarjeta-verificacion {
      background: #ffffff;
      width: 100%;
      max-width: 720px;
      border-radius: 16px;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3);
      padding: 2.5rem;
    }
    .logo-area {
      text-align: center;
      margin-bottom: 2rem;
    }
    .icono-portal {
      font-size: 2.5rem;
    }
    .logo-area h1 {
      font-size: 1.5rem;
      color: #0f172a;
      margin: 0.5rem 0 0.25rem 0;
      font-weight: 700;
    }
    .descripcion-portal {
      color: #64748b;
      font-size: 0.9rem;
      margin: 0;
    }
    .formulario-busqueda {
      margin-bottom: 2rem;
    }
    .etiqueta-input {
      display: block;
      font-size: 0.85rem;
      font-weight: 600;
      color: #334155;
      margin-bottom: 0.5rem;
    }
    .grupo-input {
      display: flex;
      gap: 0.5rem;
    }
    .grupo-input input {
      flex: 1;
      padding: 0.75rem 1rem;
      border: 2px solid #cbd5e1;
      border-radius: 8px;
      font-size: 1rem;
      font-family: monospace;
      outline: none;
      transition: border-color 0.2s;
    }
    .grupo-input input:focus {
      border-color: #2563eb;
    }
    .btn-verificar {
      background: #2563eb;
      color: white;
      border: none;
      padding: 0.75rem 1.5rem;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-verificar:hover:not(:disabled) {
      background: #1d4ed8;
    }
    .btn-verificar:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .panel-valido {
      background: #f0fdf4;
      border: 2px solid #86efac;
      border-radius: 12px;
      padding: 1.5rem;
      margin-bottom: 1.5rem;
    }
    .cabecera-valido {
      display: flex;
      gap: 1rem;
      align-items: center;
      margin-bottom: 1.25rem;
    }
    .icono-check {
      background: #16a34a;
      color: white;
      width: 44px;
      height: 44px;
      border-radius: 50%;
      display: flex;
      justify-content: center;
      align-items: center;
      font-size: 1.5rem;
      font-weight: bold;
      flex-shrink: 0;
    }
    .cabecera-valido h2 {
      margin: 0;
      color: #166534;
      font-size: 1.25rem;
    }
    .cabecera-valido p {
      margin: 0.25rem 0 0 0;
      color: #15803d;
      font-size: 0.85rem;
    }
    .detalles-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      background: white;
      padding: 1.25rem;
      border-radius: 8px;
      border: 1px solid #dcfce7;
    }
    .campo-detalle {
      display: flex;
      flex-direction: column;
    }
    .campo-detalle.full-width {
      grid-column: 1 / -1;
    }
    .campo-detalle .label {
      font-size: 0.75rem;
      color: #64748b;
      font-weight: 600;
      text-transform: uppercase;
      margin-bottom: 0.25rem;
    }
    .campo-detalle .valor {
      color: #1e293b;
      font-size: 0.95rem;
    }
    .nombre-destacado {
      font-size: 1.15rem !important;
      font-weight: 700;
      color: #0f172a !important;
    }
    .codigo-mono, .hash-mono {
      font-family: monospace;
      font-size: 0.85rem !important;
      background: #f1f5f9;
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      word-break: break-all;
    }
    .panel-invalido {
      background: #fef2f2;
      border: 2px solid #fca5a5;
      border-radius: 12px;
      padding: 1.5rem;
      margin-bottom: 1.5rem;
    }
    .cabecera-invalido {
      display: flex;
      gap: 1rem;
      align-items: center;
      margin-bottom: 0.75rem;
    }
    .icono-cruz {
      background: #dc2626;
      color: white;
      width: 44px;
      height: 44px;
      border-radius: 50%;
      display: flex;
      justify-content: center;
      align-items: center;
      font-size: 1.5rem;
      font-weight: bold;
      flex-shrink: 0;
    }
    .cabecera-invalido h2 {
      margin: 0;
      color: #991b1b;
      font-size: 1.2rem;
    }
    .cabecera-invalido p {
      margin: 0.25rem 0 0 0;
      color: #b91c1c;
      font-size: 0.85rem;
    }
    .advertencia-texto {
      color: #7f1d1d;
      font-size: 0.85rem;
      margin: 0;
    }
    .pie-tarjeta {
      text-align: center;
      margin-top: 1.5rem;
      padding-top: 1rem;
      border-top: 1px solid #f1f5f9;
    }
    .enlace-inicio {
      color: #64748b;
      text-decoration: none;
      font-size: 0.9rem;
    }
    .enlace-inicio:hover {
      color: #2563eb;
    }
    .estado-cargando {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 2rem;
      color: #64748b;
    }
    .spinner {
      border: 3px solid #f3f3f3;
      border-top: 3px solid #2563eb;
      border-radius: 50%;
      width: 28px;
      height: 28px;
      animation: spin 1s linear infinite;
      margin-bottom: 0.75rem;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
  `]
})
export class VerificarCertificadoComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly certificadosService = inject(CertificadosService);

  codigoIngresado: string = '';
  readonly cargando = signal<boolean>(false);
  readonly resultado = signal<VerificacionCertificadoRespuesta | null>(null);

  ngOnInit(): void {
    const paramCodigo = this.route.snapshot.paramMap.get('codigo');
    if (paramCodigo) {
      this.codigoIngresado = paramCodigo.trim();
      this.consultar();
    }
  }

  consultar(): void {
    const cod = this.codigoIngresado.trim();
    if (!cod) return;

    this.cargando.set(true);
    this.resultado.set(null);

    this.certificadosService.verificarPublico(cod).subscribe({
      next: (resp) => {
        this.resultado.set(resp);
        this.cargando.set(false);
      },
      error: () => {
        this.resultado.set({
          valido: false,
          codigoVerificacion: cod,
          mensaje: 'Error de conexión con el servidor de validación.'
        });
        this.cargando.set(false);
      }
    });
  }
}
