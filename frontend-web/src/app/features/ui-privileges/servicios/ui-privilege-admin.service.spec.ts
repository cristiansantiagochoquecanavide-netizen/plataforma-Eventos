import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { UiPrivilegeAdminService } from './ui-privilege-admin.service';
import { UiPrivilegeGrantReplacement } from '../modelos/ui-privilege-admin.model';

const API = 'http://localhost:8080/api';

describe('UiPrivilegeAdminService', () => {
  let http: jasmine.SpyObj<HttpClient>;
  let service: UiPrivilegeAdminService;

  beforeEach(() => {
    http = jasmine.createSpyObj<HttpClient>('HttpClient', ['get', 'put']);

    TestBed.configureTestingModule({
      providers: [UiPrivilegeAdminService, { provide: HttpClient, useValue: http }],
    });

    service = TestBed.inject(UiPrivilegeAdminService);
  });

  it('uses the confirmed tenant UI privilege endpoints', () => {
    const payload: UiPrivilegeGrantReplacement = {
      privilegios: [
        { claveComponente: 'events.create.button', estados: ['VISIBLE', 'HABILITADO'] },
      ],
    };

    service.listarCatalogo();
    service.obtenerPrivilegiosGrupo(7);
    service.reemplazarPrivilegiosGrupo(7, payload);

    expect(http.get).toHaveBeenCalledWith(`${API}/componentes-ui/catalogo`);
    expect(http.get).toHaveBeenCalledWith(`${API}/componentes-ui/grupos/7/privilegios`);
    expect(http.put).toHaveBeenCalledWith(`${API}/componentes-ui/grupos/7/privilegios`, payload);
  });
});
