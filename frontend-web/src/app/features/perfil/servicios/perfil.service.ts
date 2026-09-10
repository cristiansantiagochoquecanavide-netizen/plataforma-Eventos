import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Usuario } from '../../usuarios/modelos/usuario.modelo';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  obtenerPerfil() {
    return this.http.get<Usuario>(`${this.url}/perfil`);
  }

  actualizarPerfil(datos: Pick<Usuario, 'nombres' | 'apellidos' | 'correo'>) {
    return this.http.put<Usuario>(`${this.url}/perfil`, datos);
  }

  cambiarClave(claveActual: string, nuevaClave: string) {
    return this.http.patch<void>(`${this.url}/perfil/clave`, { claveActual, nuevaClave });
  }
}
