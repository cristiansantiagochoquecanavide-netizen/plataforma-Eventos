import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CertificadoRespuesta, VerificacionCertificadoRespuesta } from '../modelos/certificado.modelo';

@Injectable({ providedIn: 'root' })
export class CertificadosService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/certificados`;
  private readonly publicUrl = `${environment.apiUrl}/public/certificados`;

  obtenerMisCertificados(): Observable<CertificadoRespuesta[]> {
    return this.http.get<CertificadoRespuesta[]>(`${this.baseUrl}/mis-certificados`);
  }

  obtenerPorEvento(idEvento: number): Observable<CertificadoRespuesta[]> {
    return this.http.get<CertificadoRespuesta[]>(`${this.baseUrl}/evento/${idEvento}`);
  }

  emitirPorInscripcion(idInscripcion: number): Observable<CertificadoRespuesta> {
    return this.http.post<CertificadoRespuesta>(`${this.baseUrl}/inscripcion/${idInscripcion}`, {});
  }

  emitirMasivo(idEvento: number): Observable<CertificadoRespuesta[]> {
    return this.http.post<CertificadoRespuesta[]>(`${this.baseUrl}/evento/${idEvento}/emitir-masivo`, {});
  }

  descargarPdf(idCertificado: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${idCertificado}/descargar`, {
      responseType: 'blob',
    });
  }

  verificarPublico(codigo: string): Observable<VerificacionCertificadoRespuesta> {
    return this.http.get<VerificacionCertificadoRespuesta>(`${this.publicUrl}/verificar/${encodeURIComponent(codigo)}`);
  }
}
