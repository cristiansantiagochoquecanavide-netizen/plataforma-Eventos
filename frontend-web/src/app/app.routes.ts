import { Routes } from '@angular/router';
import { administradorGuard, autenticacionGuard } from './core/guards';
import { InicioSesionComponent } from './features/auth/inicio-sesion.component';
import { RecuperarClaveComponent } from './features/auth/recuperar-clave.component';
import { RestablecerClaveComponent } from './features/auth/restablecer-clave.component';
import { PanelComponent } from './features/panel/panel.component';
import { PerfilComponent } from './features/perfil/perfil.component';
import { RolesComponent } from './features/roles/roles.component';
import { UsuariosComponent } from './features/usuarios/usuarios.component';

export const routes: Routes = [
  { path: 'iniciar-sesion', component: InicioSesionComponent },
  { path: 'recuperar-clave', component: RecuperarClaveComponent },
  { path: 'restablecer-clave', component: RestablecerClaveComponent },
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
