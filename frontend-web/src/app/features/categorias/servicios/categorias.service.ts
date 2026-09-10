import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Categoria, GuardarCategoriaSolicitud } from '../modelos/categoria.modelo';

@Injectable({ providedIn: 'root' })
export class CategoriasService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  listarCategorias() {
    return this.http.get<Categoria[]>(`${this.url}/categorias`);
  }

  obtenerCategoria(id: number) {
    return this.http.get<Categoria>(`${this.url}/categorias/${id}`);
  }

  crearCategoria(datos: GuardarCategoriaSolicitud) {
    return this.http.post<Categoria>(`${this.url}/categorias`, datos);
  }

  actualizarCategoria(id: number, datos: GuardarCategoriaSolicitud) {
    return this.http.put<Categoria>(`${this.url}/categorias/${id}`, datos);
  }

  eliminarCategoria(id: number) {
    return this.http.delete<void>(`${this.url}/categorias/${id}`);
  }
}
