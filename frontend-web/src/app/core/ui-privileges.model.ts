export const UI_PRIVILEGE_STATES = ['VISIBLE', 'HABILITADO', 'EDITABLE'] as const;

export type UiPrivilegeState = (typeof UI_PRIVILEGE_STATES)[number];

export type UiComponentKey = string;

export interface UiComponentPrivilege {
  estadosPermitidos: UiPrivilegeState[];
}

export interface UiPrivilegesResponse {
  componentes: Record<UiComponentKey, UiComponentPrivilege>;
}
