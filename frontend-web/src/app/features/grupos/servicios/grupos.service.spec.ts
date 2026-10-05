import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { GruposService } from './grupos.service';

const API = 'http://localhost:8080/api';

describe('GruposService', () => {
  let http: jasmine.SpyObj<HttpClient>;
  let service: GruposService;

  beforeEach(() => {
    http = jasmine.createSpyObj<HttpClient>('HttpClient', ['get', 'post', 'put', 'patch']);

    TestBed.configureTestingModule({
      providers: [GruposService, { provide: HttpClient, useValue: http }],
    });

    service = TestBed.inject(GruposService);
  });

  it('uses tenant group endpoints and never the global role catalog', () => {
    service.listarGrupos();
    service.obtenerGrupo(7);
    service.crearGrupo({ nombre: 'Operaciones', descripcion: 'Equipo operativo' });
    service.actualizarGrupo(7, { nombre: 'Operaciones', descripcion: '' });
    service.desactivarGrupo(7);
    service.activarGrupo(7);
    service.asignarMiembros(7, [11, 12]);
    service.asignarRoles(7, [3, 4]);
    service.listarRolesDisponibles();

    expect(http.get).toHaveBeenCalledWith(`${API}/grupos`);
    expect(http.get).toHaveBeenCalledWith(`${API}/grupos/7`);
    expect(http.post).toHaveBeenCalledWith(`${API}/grupos`, {
      nombre: 'Operaciones',
      descripcion: 'Equipo operativo',
    });
    expect(http.put).toHaveBeenCalledWith(`${API}/grupos/7`, {
      nombre: 'Operaciones',
      descripcion: '',
    });
    expect(http.patch).toHaveBeenCalledWith(`${API}/grupos/7/desactivar`, {});
    expect(http.patch).toHaveBeenCalledWith(`${API}/grupos/7/activar`, {});
    expect(http.put).toHaveBeenCalledWith(`${API}/grupos/7/miembros`, { idsUsuarios: [11, 12] });
    expect(http.put).toHaveBeenCalledWith(`${API}/grupos/7/roles`, { idsRoles: [3, 4] });
    expect(http.get).toHaveBeenCalledWith(`${API}/grupos/roles-disponibles`);
    expect(http.get).not.toHaveBeenCalledWith(`${API}/roles`);
  });
});
