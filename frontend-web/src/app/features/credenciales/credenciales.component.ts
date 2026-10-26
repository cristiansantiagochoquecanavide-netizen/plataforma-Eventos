import { CommonModule, registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

registerLocaleData(localeEs);

interface Inscripcion {
  idInscripcion: number;
  nombreEvento: string;
  categoriaEvento: string;
  fechaInicioEvento: string;
  lugarEvento: string;
  estado: string;
}

interface Credencial {
  idInscripcion: number;
  nombreParticipante: string;
  nombreEvento: string;
  categoria: string;
  fechaEvento: string;
  lugar: string;
  token: string;
  qrPngBase64: string;
  expiraEn: string;
}

@Component({
  selector: 'app-credenciales',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './credenciales.component.html',
  styleUrl: './credenciales.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CredencialesComponent implements OnInit {
  readonly inscripciones = signal<Inscripcion[]>([]);
  readonly credencial = signal<Credencial | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  private readonly api = `${environment.apiUrl}/inscripciones`;

  constructor(private readonly http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<Inscripcion[]>(`${this.api}/mis-inscripciones`).subscribe({
      next: (items) => this.inscripciones.set(items),
      error: () => this.error.set('No pudimos cargar tus inscripciones. Comprueba el tenant activo e inténtalo de nuevo.'),
    });
  }

  emitir(id: number): void {
    this.cargando.set(true);
    this.error.set('');
    this.credencial.set(null);
    this.http.get<Credencial>(`${this.api}/${id}/credencial`).subscribe({
      next: (credencial) => { this.credencial.set(credencial); this.cargando.set(false); },
      error: (error) => {
        this.error.set(error.status === 400
          ? 'La inscripción no está confirmada; no se puede emitir una credencial activa.'
          : 'No fue posible emitir la credencial. Verifica que la inscripción pertenezca a tu organización activa.');
        this.cargando.set(false);
      },
    });
  }

  descargar(): void {
    const card = this.credencial();
    if (!card) return;
    const link = document.createElement('a');
    link.href = `data:image/png;base64,${card.qrPngBase64}`;
    link.download = `credencial-${card.idInscripcion}.png`;
    link.click();
  }
}
