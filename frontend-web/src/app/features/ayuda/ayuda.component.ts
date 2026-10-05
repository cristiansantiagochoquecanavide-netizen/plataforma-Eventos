import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-ayuda',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page-heading">
      <p class="eyebrow">Centro de ayuda</p>
      <h1>¿Cómo podemos ayudarte?</h1>
      <p>Guías breves para resolver las tareas frecuentes de la plataforma.</p>
    </section>
    <section class="help-grid" aria-label="Guías de uso">
      @for (guia of guias; track guia.titulo) {
        <details class="card help-card">
          <summary><span>{{ guia.numero }}</span><strong>{{ guia.titulo }}</strong></summary>
          <p>{{ guia.descripcion }}</p>
          <ol>
            @for (paso of guia.pasos; track paso) { <li>{{ paso }}</li> }
          </ol>
        </details>
      }
    </section>
    <p class="help-note" role="note">La información y los permisos disponibles dependen de la organización seleccionada.</p>
  `,
  styles: [`
    .help-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,340px),1fr));gap:16px}
    .help-card{padding:0;overflow:hidden}
    .help-card summary{display:flex;align-items:center;gap:12px;padding:20px;cursor:pointer;list-style:none}
    .help-card summary::-webkit-details-marker{display:none}
    .help-card summary span{display:grid;place-items:center;width:32px;height:32px;border-radius:50%;background:var(--brand-soft);color:var(--brand);font-weight:800}
    .help-card summary strong{color:var(--ink)}
    .help-card[open] summary{border-bottom:1px solid var(--line)}
    .help-card p,.help-card ol{margin:16px 20px;color:var(--muted);line-height:1.6}
    .help-card li{padding:3px 0}
    .help-note{margin-top:20px;color:var(--muted);font-size:.86rem}
  `],
})
export class AyudaComponent {
  readonly guias = [
    { numero: '01', titulo: 'Organización activa', descripcion: 'La organización elegida en el selector superior determina los datos que consultas y administras.', pasos: ['Selecciona una organización antes de abrir un módulo.', 'Si no ves registros, comprueba que el tenant y sus filtros sean correctos.', 'Solicita una membresía activa al administrador para acceder a otra organización.'] },
    { numero: '02', titulo: 'Eventos y programación', descripcion: 'Los eventos reúnen categoría, espacio, fechas, aforo y responsables.', pasos: ['Completa los datos obligatorios y elige un espacio.', 'Verifica que las fechas no se crucen con otra actividad.', 'Guarda y confirma que el evento aparece bajo la organización activa.'] },
    { numero: '03', titulo: 'Inscripciones y participantes', descripcion: 'El padrón muestra asistentes, estados e historial de inscripciones del tenant.', pasos: ['Filtra por evento, estado o nombre/correo.', 'Abre Historial para revisar inscripciones asociadas.', 'Usa Reporte configurable para vista previa, descarga o envío autorizado.'] },
    { numero: '04', titulo: 'Grupos y privilegios', descripcion: 'Los roles asignados a grupos aportan permisos de acceso y de interfaz.', pasos: ['Agrega miembros y roles al grupo correcto.', 'Configura componentes visibles, habilitados o editables.', 'La política de interfaz no reemplaza la autorización del servidor.'] },
    { numero: '05', titulo: 'Staff', descripcion: 'Las convocatorias permiten recibir postulaciones y gestionar asignaciones.', pasos: ['Selecciona una convocatoria vigente.', 'Completa y envía tu postulación.', 'Consulta el estado y tus asignaciones desde el módulo Staff.'] },
    { numero: '06', titulo: 'Bitácora', descripcion: 'La bitácora lista las acciones autorizadas de la organización seleccionada.', pasos: ['Usa filtros de módulo, entidad y acción.', 'El contenido se descifra solo en la aplicación y según permisos.', 'Contacta al administrador si el acceso es rechazado.'] },
  ];
}
