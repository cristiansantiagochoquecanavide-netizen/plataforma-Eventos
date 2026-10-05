package com.plataformaeventos.configuracion;

import com.plataformaeventos.asignacionesstaff.modelo.AsignacionStaff;
import com.plataformaeventos.asignacionesstaff.modelo.EstadoAsignacionStaff;
import com.plataformaeventos.asignacionesstaff.repositorio.AsignacionStaffRepository;
import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.espacios.modelo.Espacio;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.postulacionesstaff.modelo.PostulacionStaff;
import com.plataformaeventos.postulacionesstaff.repositorio.PostulacionStaffRepository;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoDatosInicializadorTest {

    @Test
    void demoSeederIsDisabledByDefault() {
        new ApplicationContextRunner()
                .withUserConfiguration(DemoDatosInicializador.class)
                .withBean(OrganizacionRepository.class, () -> mock(OrganizacionRepository.class))
                .withBean(UsuarioRepository.class, () -> mock(UsuarioRepository.class))
                .withBean(RolRepository.class, () -> mock(RolRepository.class))
                .withBean(MembresiaOrganizacionRepository.class, () -> mock(MembresiaOrganizacionRepository.class))
                .withBean(CategoriaRepository.class, () -> mock(CategoriaRepository.class))
                .withBean(EspacioRepository.class, () -> mock(EspacioRepository.class))
                .withBean(EventoRepository.class, () -> mock(EventoRepository.class))
                .withBean(ConvocatoriaRepository.class, () -> mock(ConvocatoriaRepository.class))
                .withBean(PostulacionStaffRepository.class, () -> mock(PostulacionStaffRepository.class))
                .withBean(AsignacionStaffRepository.class, () -> mock(AsignacionStaffRepository.class))
                .withBean(InscripcionRepository.class, () -> mock(InscripcionRepository.class))
                .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
                .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class))
                .run(context -> assertThat(context).doesNotHaveBean(ApplicationRunner.class));
    }

    @Test
    void propertyEnablesTheDemoSeederRunner() {
        new ApplicationContextRunner()
                .withUserConfiguration(DemoDatosInicializador.class)
                .withPropertyValues("demo.seed.enabled=true")
                .withBean(OrganizacionRepository.class, () -> mock(OrganizacionRepository.class))
                .withBean(UsuarioRepository.class, () -> mock(UsuarioRepository.class))
                .withBean(RolRepository.class, () -> mock(RolRepository.class))
                .withBean(MembresiaOrganizacionRepository.class, () -> mock(MembresiaOrganizacionRepository.class))
                .withBean(CategoriaRepository.class, () -> mock(CategoriaRepository.class))
                .withBean(EspacioRepository.class, () -> mock(EspacioRepository.class))
                .withBean(EventoRepository.class, () -> mock(EventoRepository.class))
                .withBean(ConvocatoriaRepository.class, () -> mock(ConvocatoriaRepository.class))
                .withBean(PostulacionStaffRepository.class, () -> mock(PostulacionStaffRepository.class))
                .withBean(AsignacionStaffRepository.class, () -> mock(AsignacionStaffRepository.class))
                .withBean(InscripcionRepository.class, () -> mock(InscripcionRepository.class))
                .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
                .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class))
                .run(context -> assertThat(context).hasSingleBean(ApplicationRunner.class));
    }

    @Test
    void rerunUsesDeterministicFindersAndDoesNotDuplicateTenantOwnedRows() {
        OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        RolRepository roles = mock(RolRepository.class);
        MembresiaOrganizacionRepository membresias = mock(MembresiaOrganizacionRepository.class);
        CategoriaRepository categorias = mock(CategoriaRepository.class);
        EspacioRepository espacios = mock(EspacioRepository.class);
        EventoRepository eventos = mock(EventoRepository.class);
        ConvocatoriaRepository convocatorias = mock(ConvocatoriaRepository.class);
        PostulacionStaffRepository postulaciones = mock(PostulacionStaffRepository.class);
        AsignacionStaffRepository asignaciones = mock(AsignacionStaffRepository.class);
        InscripcionRepository inscripciones = mock(InscripcionRepository.class);
        PasswordEncoder codificador = mock(PasswordEncoder.class);
        when(codificador.encode(DemoDatosInicializador.CLAVE_DEMO)).thenReturn("demo-hash");

        Rol admin = rol(1L, "ADMINISTRADOR");
        Rol organizador = rol(2L, "ORGANIZADOR");
        Rol staff = rol(3L, "STAFF");
        Rol participante = rol(4L, "PARTICIPANTE");
        when(roles.findByNombre("ADMINISTRADOR")).thenReturn(Optional.of(admin));
        when(roles.findByNombre("ORGANIZADOR")).thenReturn(Optional.of(organizador));
        when(roles.findByNombre("STAFF")).thenReturn(Optional.of(staff));
        when(roles.findByNombre("PARTICIPANTE")).thenReturn(Optional.of(participante));

        prepararTenantExistente("demo-universidad-norte", 10L, "Demo Norte", "Capacitacion Academica", "Aula Magna Norte", "Jornada Docente Demo Norte",
                organizaciones, usuarios, membresias, categorias, espacios, eventos, convocatorias, postulaciones, asignaciones, inscripciones);
        prepararTenantExistente("demo-centro-cultural-sur", 20L, "Demo Sur", "Cultura Comunitaria", "Sala Experimental Sur", "Festival Comunitario Demo Sur",
                organizaciones, usuarios, membresias, categorias, espacios, eventos, convocatorias, postulaciones, asignaciones, inscripciones);

        new DemoDatosInicializador().crearDatosDemo(organizaciones, usuarios, roles, membresias, categorias, espacios, eventos,
                convocatorias, postulaciones, asignaciones, inscripciones, codificador);

        verify(organizaciones, never()).save(any());
        verify(categorias, never()).save(any());
        verify(espacios, never()).save(any());
        verify(eventos, never()).save(any());
        verify(convocatorias, never()).save(any());
        verify(postulaciones, never()).save(any());
        verify(asignaciones, never()).save(any(AsignacionStaff.class));
        verify(inscripciones, never()).save(any(Inscripcion.class));
    }

    private void prepararTenantExistente(
            String slug,
            Long orgId,
            String prefijo,
            String nombreCategoria,
            String nombreEspacio,
            String nombreEvento,
            OrganizacionRepository organizaciones,
            UsuarioRepository usuarios,
            MembresiaOrganizacionRepository membresias,
            CategoriaRepository categorias,
            EspacioRepository espacios,
            EventoRepository eventos,
            ConvocatoriaRepository convocatorias,
            PostulacionStaffRepository postulaciones,
            AsignacionStaffRepository asignaciones,
            InscripcionRepository inscripciones
    ) {
        Organizacion org = org(orgId, slug);
        Categoria categoria = categoria(100L + orgId, nombreCategoria, org);
        Espacio espacio = espacio(200L + orgId, nombreEspacio, org);
        Evento evento = evento(300L + orgId, nombreEvento, categoria, espacio, org);
        Convocatoria convocatoria = convocatoria(400L + orgId, evento, org);
        Usuario organizador = usuario(500L + orgId, prefijo, "Organizador", correo(slug, "organizador"));
        Usuario staff = usuario(600L + orgId, prefijo, "Staff", correo(slug, "staff"));
        Usuario participante = usuario(700L + orgId, prefijo, "Participante", correo(slug, "participante"));
        PostulacionStaff postulacion = postulacion(800L + orgId, convocatoria, staff, org);
        Inscripcion inscripcion = new Inscripcion(evento, participante, "demo-" + slug + "-" + participante.getCorreo());
        inscripcion.asignarOrganizacion(org);
        ReflectionTestUtils.setField(inscripcion, "id", 900L + orgId);

        when(organizaciones.findBySlug(slug)).thenReturn(Optional.of(org));
        when(usuarios.findByCorreoConRoles(organizador.getCorreo())).thenReturn(Optional.of(organizador));
        when(usuarios.findByCorreoConRoles(staff.getCorreo())).thenReturn(Optional.of(staff));
        when(usuarios.findByCorreoConRoles(participante.getCorreo())).thenReturn(Optional.of(participante));
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> invocation.getArgument(0)).when(membresias).save(any(MembresiaOrganizacion.class));
        when(membresias.findByUsuarioIdAndOrganizacionIdConRoles(eq(organizador.getId()), eq(orgId))).thenReturn(Optional.of(new MembresiaOrganizacion(organizador, org)));
        when(membresias.findByUsuarioIdAndOrganizacionIdConRoles(eq(staff.getId()), eq(orgId))).thenReturn(Optional.of(new MembresiaOrganizacion(staff, org)));
        when(membresias.findByUsuarioIdAndOrganizacionIdConRoles(eq(participante.getId()), eq(orgId))).thenReturn(Optional.of(new MembresiaOrganizacion(participante, org)));
        when(categorias.findFirstByNombreIgnoreCaseAndOrganizacionId(nombreCategoria, orgId)).thenReturn(Optional.of(categoria));
        when(espacios.findFirstByNombreIgnoreCaseAndOrganizacionId(nombreEspacio, orgId)).thenReturn(Optional.of(espacio));
        when(eventos.findFirstByNombreIgnoreCaseAndOrganizacionId(nombreEvento, orgId)).thenReturn(Optional.of(evento));
        when(convocatorias.findFirstByEventoIdAndOrganizacionIdAndTipoPersonal(evento.getId(), orgId, TipoPersonal.STAFF)).thenReturn(Optional.of(convocatoria));
        when(postulaciones.findByConvocatoriaIdAndUsuarioIdAndOrganizacionId(convocatoria.getId(), staff.getId(), orgId)).thenReturn(Optional.of(postulacion));
        when(asignaciones.existsByPostulacionIdAndOrganizacionIdAndEstado(postulacion.getId(), orgId, EstadoAsignacionStaff.ASIGNADA)).thenReturn(true);
        when(inscripciones.findByEventoIdAndParticipanteIdAndOrganizacionId(evento.getId(), participante.getId(), orgId)).thenReturn(Optional.of(inscripcion));
    }

    private static String correo(String slug, String rol) {
        return switch (slug) {
            case "demo-universidad-norte" -> "demo.norte." + rol + "@plataformaeventos.local";
            case "demo-centro-cultural-sur" -> "demo.sur." + rol + "@plataformaeventos.local";
            default -> throw new IllegalArgumentException(slug);
        };
    }

    private static Rol rol(Long id, String nombre) {
        Rol rol = new Rol(nombre, nombre, EstadoRol.ACTIVO);
        ReflectionTestUtils.setField(rol, "id", id);
        return rol;
    }

    private static Organizacion org(Long id, String slug) {
        Organizacion org = new Organizacion("Organizacion " + slug, slug);
        ReflectionTestUtils.setField(org, "id", id);
        return org;
    }

    private static Usuario usuario(Long id, String nombres, String apellidos, String correo) {
        Usuario usuario = new Usuario(nombres, apellidos, correo, "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private static Categoria categoria(Long id, String nombre, Organizacion org) {
        Categoria categoria = new Categoria(nombre, "Demo", org);
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    private static Espacio espacio(Long id, String nombre, Organizacion org) {
        Espacio espacio = new Espacio(nombre, "Demo", 120);
        espacio.asignarOrganizacion(org);
        ReflectionTestUtils.setField(espacio, "id", id);
        return espacio;
    }

    private static Evento evento(Long id, String nombre, Categoria categoria, Espacio espacio, Organizacion org) {
        Evento evento = new Evento(nombre, "Demo", OffsetDateTime.now().plusMonths(6), OffsetDateTime.now().plusMonths(6).plusHours(4),
                espacio.getNombre(), EstadoEvento.PUBLICADO, categoria, espacio, 80);
        evento.asignarOrganizacion(org);
        ReflectionTestUtils.setField(evento, "id", id);
        return evento;
    }

    private static Convocatoria convocatoria(Long id, Evento evento, Organizacion org) {
        Convocatoria convocatoria = new Convocatoria(evento, TipoPersonal.STAFF, "Demo", 2, java.time.LocalDate.now().plusMonths(5));
        convocatoria.asignarOrganizacion(org);
        ReflectionTestUtils.setField(convocatoria, "id", id);
        return convocatoria;
    }

    private static PostulacionStaff postulacion(Long id, Convocatoria convocatoria, Usuario usuario, Organizacion org) {
        PostulacionStaff postulacion = new PostulacionStaff(convocatoria, usuario, org, "Demo");
        ReflectionTestUtils.setField(postulacion, "id", id);
        postulacion.aceptar();
        return postulacion;
    }
}
