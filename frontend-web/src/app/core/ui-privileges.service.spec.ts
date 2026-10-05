import { effect } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Subject, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { UiPrivilegeState } from './ui-privileges.model';
import { UiPrivilegesService } from './ui-privileges.service';

type UiPrivilegesTestResponse = {
  componentes: Record<string, { estadosPermitidos: UiPrivilegeState[] }>;
};

describe('UiPrivilegesService', () => {
  let http: jasmine.SpyObj<HttpClient>;
  let service: UiPrivilegesService;

  beforeEach(() => {
    http = jasmine.createSpyObj<HttpClient>('HttpClient', ['get']);

    TestBed.configureTestingModule({
      providers: [UiPrivilegesService, { provide: HttpClient, useValue: http }],
    });

    service = TestBed.inject(UiPrivilegesService);
  });

  it('denies every component state before the policy is loaded', () => {
    expect(service.allows('eventos.menu', 'VISIBLE')).toBeFalse();
    expect(service.allows('eventos.menu', 'HABILITADO')).toBeFalse();
    expect(service.allows('eventos.menu', 'EDITABLE')).toBeFalse();
  });

  it('loads the effective server policy for the selected organization', () => {
    const request$ = new Subject<UiPrivilegesTestResponse>();
    http.get.and.returnValue(request$.asObservable());

    service.loadForOrganization(17).subscribe();

    expect(http.get).toHaveBeenCalledWith('/api/componentes-ui/actual/privilegios', {
      params: { organizacionId: 17 },
    });

    request$.next({ componentes: { 'eventos.menu': { estadosPermitidos: ['VISIBLE'] } } });
    request$.complete();

    expect(service.allows('eventos.menu', 'VISIBLE')).toBeTrue();
    expect(service.allows('eventos.menu', 'HABILITADO')).toBeFalse();
    expect(service.allows('eventos.detalle', 'VISIBLE')).toBeFalse();
  });

  it('notifies Angular effects when an async policy load grants a privilege', () => {
    const request$ = new Subject<UiPrivilegesTestResponse>();
    http.get.and.returnValue(request$.asObservable());
    const observed: boolean[] = [];
    const effectRef = TestBed.runInInjectionContext(() =>
      effect(() => observed.push(service.allows('eventos.menu', 'VISIBLE'))),
    );

    TestBed.tick();
    service.loadForOrganization(17).subscribe();
    request$.next({ componentes: { 'eventos.menu': { estadosPermitidos: ['VISIBLE'] } } });
    request$.complete();
    TestBed.tick();

    expect(observed).toEqual([false, true]);
    effectRef.destroy();
  });

  it('clears the current policy and denies after load', () => {
    const request$ = new Subject<UiPrivilegesTestResponse>();
    http.get.and.returnValue(request$.asObservable());

    service.loadForOrganization(17).subscribe();
    request$.next({ componentes: { 'eventos.menu': { estadosPermitidos: ['VISIBLE', 'HABILITADO'] } } });
    request$.complete();

    expect(service.allows('eventos.menu', 'HABILITADO')).toBeTrue();

    service.clear();

    expect(service.allows('eventos.menu', 'VISIBLE')).toBeFalse();
    expect(service.allows('eventos.menu', 'HABILITADO')).toBeFalse();
  });

  it('denies on load errors instead of keeping the previous tenant policy', () => {
    const firstRequest$ = new Subject<UiPrivilegesTestResponse>();
    http.get.and.returnValue(firstRequest$.asObservable());

    service.loadForOrganization(17).subscribe();
    firstRequest$.next({ componentes: { 'eventos.menu': { estadosPermitidos: ['VISIBLE'] } } });
    firstRequest$.complete();
    expect(service.allows('eventos.menu', 'VISIBLE')).toBeTrue();

    http.get.and.returnValue(throwError(() => new Error('network')));
    service.loadForOrganization(18).subscribe({ error: () => undefined });

    expect(service.allows('eventos.menu', 'VISIBLE')).toBeFalse();
  });

  it('ignores a late response from a previous organization', () => {
    const oldTenantRequest$ = new Subject<UiPrivilegesTestResponse>();
    const currentTenantRequest$ = new Subject<UiPrivilegesTestResponse>();
    http.get.and.returnValues(oldTenantRequest$.asObservable(), currentTenantRequest$.asObservable());

    service.loadForOrganization(17).subscribe();
    service.loadForOrganization(18).subscribe();

    currentTenantRequest$.next({
      componentes: { 'eventos.menu': { estadosPermitidos: ['VISIBLE', 'HABILITADO'] } },
    });
    currentTenantRequest$.complete();
    oldTenantRequest$.next({ componentes: { 'eventos.menu': { estadosPermitidos: ['EDITABLE'] } } });
    oldTenantRequest$.complete();

    expect(service.allows('eventos.menu', 'VISIBLE')).toBeTrue();
    expect(service.allows('eventos.menu', 'HABILITADO')).toBeTrue();
    expect(service.allows('eventos.menu', 'EDITABLE')).toBeFalse();
  });
});
