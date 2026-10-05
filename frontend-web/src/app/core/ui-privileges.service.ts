import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, catchError, map, tap, throwError } from 'rxjs';
import { UiComponentKey, UiPrivilegesResponse, UiPrivilegeState } from './ui-privileges.model';

const API_PRIVILEGIOS_UI = '/api/componentes-ui/actual/privilegios';

@Injectable({ providedIn: 'root' })
export class UiPrivilegesService {
  private readonly policy = signal<ReadonlyMap<UiComponentKey, ReadonlySet<UiPrivilegeState>>>(new Map());
  private requestVersion = 0;
  private currentOrganizationId: number | null = null;

  constructor(private readonly http: HttpClient) {}

  loadForOrganization(orgId: number): Observable<void> {
    const version = ++this.requestVersion;
    this.currentOrganizationId = orgId;
    this.policy.set(new Map());

    return this.http
      .get<UiPrivilegesResponse>(API_PRIVILEGIOS_UI, { params: { organizacionId: orgId } })
      .pipe(
        tap((response) => {
          if (!this.isCurrentRequest(version, orgId)) return;
          this.policy.set(this.toPolicy(response));
        }),
        map(() => undefined),
        catchError((error) => {
          if (this.isCurrentRequest(version, orgId)) this.policy.set(new Map());
          return throwError(() => error);
        }),
      );
  }

  clear(): void {
    this.requestVersion++;
    this.currentOrganizationId = null;
    this.policy.set(new Map());
  }

  allows(key: UiComponentKey, state: UiPrivilegeState): boolean {
    return this.policy().get(key)?.has(state) ?? false;
  }

  private isCurrentRequest(version: number, orgId: number): boolean {
    return this.requestVersion === version && this.currentOrganizationId === orgId;
  }

  private toPolicy(response: UiPrivilegesResponse): Map<UiComponentKey, ReadonlySet<UiPrivilegeState>> {
    return new Map(
      Object.entries(response.componentes ?? {}).map(([key, privilege]) => [
        key,
        new Set(privilege.estadosPermitidos ?? []),
      ]),
    );
  }
}
