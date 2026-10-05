import { Routes } from '@angular/router';
import {
  eventosGuard,
  autenticacionGuard,
  convocatoriasGuard,
  recursosGuard,
  rolesGuard,
  espaciosGuard,
  usuariosGuard,
  postulacionesGuard,
  asignacionesGuard,
  bitacoraGuard,
  miembrosGuard,
  participantesGuard,
  administradorGuard,
} from './core/guards';
import { InicioSesionComponent } from './features/auth/inicio-sesion.component';
import { RecuperarClaveComponent } from './features/auth/recuperar-clave.component';
import { RestablecerClaveComponent } from './features/auth/restablecer-clave.component';
import { ConsultaEventosComponent } from './features/eventos/consulta-eventos.component';
import { PanelComponent } from './features/panel/panel.component';
import { PerfilComponent } from './features/perfil/perfil.component';
import { AyudaComponent } from './features/ayuda/ayuda.component';

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
      { path: 'ayuda', component: AyudaComponent },
      {
        path: 'saas/suscripciones',
        loadComponent: () => import('./features/saas/suscripciones.component').then((m) => m.SuscripcionesComponent),
        canActivate: [rolesGuard],
      },
      { path: 'consulta-eventos', component: ConsultaEventosComponent },
      { path: 'eventos', loadComponent: () => import('./features/eventos/eventos.component').then((m) => m.EventosComponent), canActivate: [eventosGuard] },
      { path: 'categorias', loadComponent: () => import('./features/categorias/categorias.component').then((m) => m.CategoriasComponent), canActivate: [eventosGuard] },
      { path: 'usuarios', loadComponent: () => import('./features/usuarios/usuarios.component').then((m) => m.UsuariosComponent), canActivate: [usuariosGuard] },
      { path: 'participantes', loadComponent: () => import('./features/participantes/participantes.component').then((m) => m.ParticipantesComponent), canActivate: [participantesGuard] },
      { path: 'grupos', loadComponent: () => import('./features/grupos/grupos.component').then((m) => m.GruposComponent), canActivate: [administradorGuard] },
      {
        path: 'privilegios-ui',
        loadComponent: () => import('./features/ui-privileges/privilegios-ui.component').then((m) => m.PrivilegiosUiComponent),
        canActivate: [administradorGuard],
      },
      { path: 'roles', loadComponent: () => import('./features/roles/roles.component').then((m) => m.RolesComponent), canActivate: [rolesGuard] },
      { path: 'recursos', loadComponent: () => import('./features/recursos/recursos.component').then((m) => m.RecursosComponent), canActivate: [recursosGuard] },
      { path: 'espacios', loadComponent: () => import('./features/espacios/espacios.component').then((m) => m.EspaciosComponent), canActivate: [espaciosGuard] },
      {
        path: 'convocatorias',
        loadComponent: () => import('./features/convocatorias/convocatorias.component').then((m) => m.ConvocatoriasComponent),
        canActivate: [convocatoriasGuard],
      },
      {
        path: 'postulaciones',
        loadComponent: () => import('./features/postulaciones/postulaciones.component').then((m) => m.PostulacionesComponent),
        canActivate: [postulacionesGuard],
      },
      {
        path: 'asignaciones',
        loadComponent: () => import('./features/asignaciones/asignaciones.component').then((m) => m.AsignacionesComponent),
        canActivate: [asignacionesGuard],
      },
      {
        path: 'bitacora',
        loadComponent: () => import('./features/bitacora/bitacora.component').then((m) => m.BitacoraComponent),
        canActivate: [bitacoraGuard],
      },
      {
        path: 'miembros-organizacion',
        loadComponent: () => import('./features/organizaciones/miembros-organizacion.component').then((m) => m.MiembrosOrganizacionComponent),
        canActivate: [miembrosGuard],
      },
      { path: '', pathMatch: 'full', redirectTo: 'perfil' },
    ],
  },
  { path: '**', redirectTo: '' },
];
