import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { SesionRespuesta } from '../modelos/auth.modelo';
import { Usuario } from '../../usuarios/modelos/usuario.modelo';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  iniciarSesion(correo: string, clave: string) {
    return this.http.post<SesionRespuesta>(`${this.url}/autenticacion/iniciar-sesion`, {
      correo,
      clave,
    });
  }

  recuperarClave(correo: string) {
    return this.http.post<void>(`${this.url}/autenticacion/solicitar-recuperacion-clave`, { correo });
  }

  restablecerClave(token: string, nuevaClave: string) {
    return this.http.post<void>(`${this.url}/autenticacion/restablecer-clave`, {
      token,
      nuevaClave,
    });
  }

  refrescar(tokenRefresco: string) {
    return this.http.post<SesionRespuesta>(`${this.url}/autenticacion/refrescar`, {
      tokenRefresco,
    });
  }

  cerrarSesion(tokenRefresco: string) {
    return this.http.post<void>(`${this.url}/autenticacion/cerrar-sesion`, { tokenRefresco });
  }

  usuarioActual() {
    return this.http.get<Usuario>(`${this.url}/autenticacion/usuario-actual`);
  }
}
