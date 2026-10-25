import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import {
  UiPrivilegeCatalogEntry,
  UiPrivilegeGrantReplacement,
  UiPrivilegeGroupGrantsResponse,
} from '../modelos/ui-privilege-admin.model';

@Injectable({ providedIn: 'root' })
export class UiPrivilegeAdminService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/componentes-ui`;

  listarCatalogo() {
    return this.http.get<UiPrivilegeCatalogEntry[]>(`${this.url}/catalogo`);
  }

  obtenerPrivilegiosGrupo(idGrupo: number) {
    return this.http.get<UiPrivilegeGroupGrantsResponse>(`${this.url}/grupos/${idGrupo}/privilegios`);
  }

  reemplazarPrivilegiosGrupo(idGrupo: number, payload: UiPrivilegeGrantReplacement) {
    return this.http.put<UiPrivilegeGroupGrantsResponse>(`${this.url}/grupos/${idGrupo}/privilegios`, payload);
  }
}
