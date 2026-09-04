import { Routes } from '@angular/router';
import {
  administradorGuard,
  eventosGuard,
  autenticacionGuard,
  convocatoriasGuard,
  recursosGuard,
  espaciosGuard,
} from './core/guards';
import { InicioSesionComponent } from './features/auth/inicio-sesion.component';
import { RecuperarClaveComponent } from './features/auth/recuperar-clave.component';
import { RestablecerClaveComponent } from './features/auth/restablecer-clave.component';
import { CategoriasComponent } from './features/categorias/categorias.component';
import { ConvocatoriasComponent } from './features/convocatorias/convocatorias.component';
import { EventosComponent } from './features/eventos/eventos.component';
import { ConsultaEventosComponent } from './features/eventos/consulta-eventos.component';
import { PanelComponent } from './features/panel/panel.component';
import { PerfilComponent } from './features/perfil/perfil.component';
import { RecursosComponent } from './features/recursos/recursos.component';
import { EspaciosComponent } from './features/espacios/espacios.component';
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
      { path: 'consulta-eventos', component: ConsultaEventosComponent },
      { path: 'eventos', component: EventosComponent, canActivate: [eventosGuard] },
      { path: 'categorias', component: CategoriasComponent, canActivate: [eventosGuard] },
      { path: 'usuarios', component: UsuariosComponent, canActivate: [administradorGuard] },
      { path: 'roles', component: RolesComponent, canActivate: [administradorGuard] },
      { path: 'recursos', component: RecursosComponent, canActivate: [recursosGuard] },
      { path: 'espacios', component: EspaciosComponent, canActivate: [espaciosGuard] },
      {
        path: 'convocatorias',
        component: ConvocatoriasComponent,
        canActivate: [convocatoriasGuard],
      },
      { path: '', pathMatch: 'full', redirectTo: 'perfil' },
    ],
  },
  { path: '**', redirectTo: '' },
];
