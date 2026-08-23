import { Routes } from '@angular/router';
import { administradorGuard, autenticacionGuard } from './core/guards';
import { InicioSesionComponent } from './paginas/inicio-sesion.component';
import { PanelComponent } from './paginas/panel.component';
import { PerfilComponent } from './paginas/perfil.component';
import { RolesComponent } from './paginas/roles.component';
import { UsuariosComponent } from './paginas/usuarios.component';

export const routes: Routes = [
  { path: 'iniciar-sesion', component: InicioSesionComponent },
  {
    path: '',
    component: PanelComponent,
    canActivate: [autenticacionGuard],
    children: [
      { path: 'perfil', component: PerfilComponent },
      { path: 'usuarios', component: UsuariosComponent, canActivate: [administradorGuard] },
      { path: 'roles', component: RolesComponent, canActivate: [administradorGuard] },
      { path: '', pathMatch: 'full', redirectTo: 'perfil' },
    ],
  },
  { path: '**', redirectTo: '' },
];
