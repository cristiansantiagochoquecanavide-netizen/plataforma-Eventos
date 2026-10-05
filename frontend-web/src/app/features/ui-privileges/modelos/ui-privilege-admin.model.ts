import { UiPrivilegeState } from '../../../core/ui-privileges.model';

export interface UiPrivilegeCatalogEntry {
  clave: string;
  tipo: string;
  descripcion: string;
  estadosSoportados: readonly string[];
  controlRecuperacion: boolean;
}

export interface UiPrivilegeGroupGrant {
  claveComponente: string;
  estados: UiPrivilegeState[];
}

export interface UiPrivilegeGroupGrantsResponse {
  idGrupo: number;
  privilegios: UiPrivilegeGroupGrant[];
}

export interface UiPrivilegeGrantReplacement {
  privilegios: UiPrivilegeGroupGrant[];
}
