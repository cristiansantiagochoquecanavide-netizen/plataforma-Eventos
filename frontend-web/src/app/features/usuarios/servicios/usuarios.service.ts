import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { PaginaUsuarios, Usuario } from '../modelos/usuario.modelo';

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  listarUsuarios(pagina = 0, tamano = 10, termino = '') {
    const params = {
      pagina: pagina.toString(),
      tamano: tamano.toString(),
      ...(termino.trim() ? { termino: termino.trim() } : {}),
    };
    return this.http.get<PaginaUsuarios>(`${this.url}/usuarios`, { params });
  }

  crearUsuario(datos: object) {
    return this.http.post<Usuario>(`${this.url}/usuarios`, datos);
  }

  actualizarUsuario(id: number, datos: object) {
    return this.http.put<Usuario>(`${this.url}/usuarios/${id}`, datos);
  }

  cambiarEstadoUsuario(id: number, estado: string) {
    return this.http.patch<Usuario>(`${this.url}/usuarios/${id}/estado`, { estado });
  }
}
