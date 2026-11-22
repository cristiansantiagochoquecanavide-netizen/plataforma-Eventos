import { Routes } from '@angular/router';
import {
  eventosGuard,
  consultaEventosGuard,
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
  reportesGuard,
  asistenciasGuard,
  notificacionesGuard,
  permisoGuard,
} from './core/guards';
import { InicioSesionComponent } from './features/auth/inicio-sesion.component';
import { RecuperarClaveComponent } from './features/auth/recuperar-clave.component';
import { RestablecerClaveComponent } from './features/auth/restablecer-clave.component';
import { ConsultaEventosComponent } from './features/eventos/consulta-eventos.component';
import { PanelComponent } from './features/panel/panel.component';
import { PerfilComponent } from './features/perfil/perfil.component';
import { AyudaComponent } from './features/ayuda/ayuda.component';
import { SaasShellComponent } from './features/saas/saas-shell.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./features/saas/alta-saas.component').then((m) => m.AltaSaasComponent) },
  { path: 'planes', loadComponent: () => import('./features/saas/alta-saas.component').then((m) => m.AltaSaasComponent) },
  { path: 'verificar-alta-saas', loadComponent: () => import('./features/saas/alta-saas.component').then((m) => m.AltaSaasComponent) },
  { path: 'activar-cuenta-saas', loadComponent: () => import('./features/saas/alta-saas.component').then((m) => m.AltaSaasComponent) },
  { path: 'pago/completado', loadComponent: () => import('./features/saas/alta-saas.component').then((m) => m.AltaSaasComponent) },
  { path: 'verificar-certificado/:codigo', loadComponent: () => import('./features/certificados/verificar-certificado.component').then((m) => m.VerificarCertificadoComponent) },
  { path: 'verificar-certificado', loadComponent: () => import('./features/certificados/verificar-certificado.component').then((m) => m.VerificarCertificadoComponent) },
  { path: 'iniciar-sesion', component: InicioSesionComponent },
  { path: 'recuperar-clave', component: RecuperarClaveComponent },
  { path: 'restablecer-clave', component: RestablecerClaveComponent },
  {
    path: 'saas',
    component: SaasShellComponent,
    canActivate: [rolesGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'organizaciones' },
      { path: 'organizaciones', loadComponent: () => import('./features/saas/organizaciones-saas.component').then((m) => m.OrganizacionesSaasComponent) },
      { path: 'roles', loadComponent: () => import('./features/roles/roles.component').then((m) => m.RolesComponent) },
    ],
  },
  { path: 'roles', pathMatch: 'full', redirectTo: 'saas/roles' },
  {
    path: '',
    component: PanelComponent,
    canActivate: [autenticacionGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent) },
      { path: 'perfil', component: PerfilComponent },
      { path: 'mis-credenciales', loadComponent: () => import('./features/credenciales/credenciales.component').then((m) => m.CredencialesComponent) },
      { path: 'mis-certificados', loadComponent: () => import('./features/certificados/mis-certificados.component').then((m) => m.MisCertificadosComponent) },
      { path: 'mi-historial', loadComponent: () => import('./features/historial/historial-participacion.component').then((m) => m.HistorialParticipacionComponent), canActivate: [permisoGuard((s) => s.puedeVerHistorialParticipacion())] },
      { path: 'ayuda', component: AyudaComponent },
      { path: 'consulta-eventos', component: ConsultaEventosComponent, canActivate: [consultaEventosGuard] },
      { path: 'eventos', loadComponent: () => import('./features/eventos/eventos.component').then((m) => m.EventosComponent), canActivate: [eventosGuard] },
      { path: 'categorias', loadComponent: () => import('./features/categorias/categorias.component').then((m) => m.CategoriasComponent), canActivate: [eventosGuard] },
      { path: 'reportes', loadComponent: () => import('./features/reportes/reportes.component').then((m) => m.ReportesComponent), canActivate: [reportesGuard] },
      { path: 'usuarios', loadComponent: () => import('./features/usuarios/usuarios.component').then((m) => m.UsuariosComponent), canActivate: [usuariosGuard] },
      { path: 'participantes', loadComponent: () => import('./features/participantes/participantes.component').then((m) => m.ParticipantesComponent), canActivate: [participantesGuard] },
      { path: 'asistencias', loadComponent: () => import('./features/asistencias/asistencias.component').then((m) => m.AsistenciasComponent), canActivate: [asistenciasGuard] },
      { path: 'notificaciones', loadComponent: () => import('./features/notificaciones/notificaciones.component').then((m) => m.NotificacionesComponent), canActivate: [notificacionesGuard] },
      { path: 'aforo', loadComponent: () => import('./features/aforo/aforo.component').then((m) => m.AforoComponent), canActivate: [permisoGuard((s) => s.puedeConsultarAsistencia())] },
      { path: 'grupos', loadComponent: () => import('./features/grupos/grupos.component').then((m) => m.GruposComponent), canActivate: [administradorGuard] },
      {
        path: 'privilegios-ui',
        loadComponent: () => import('./features/ui-privileges/privilegios-ui.component').then((m) => m.PrivilegiosUiComponent),
        canActivate: [administradorGuard],
      },
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
