package com.plataformaeventos.saas.servicio;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.saas.configuracion.StripeSaasProperties;
import com.plataformaeventos.saas.dto.AltaSaasIniciadaRespuesta;
import com.plataformaeventos.saas.dto.ActivarPropietarioSaasSolicitud;
import com.plataformaeventos.saas.dto.CrearAltaSaasSolicitud;
import com.plataformaeventos.saas.dto.PlanSaasPublico;
import com.plataformaeventos.saas.dto.VerificarAltaSaasRespuesta;
import com.plataformaeventos.saas.modelo.EstadoSolicitudAltaSaas;
import com.plataformaeventos.saas.modelo.EstadoSuscripcion;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.modelo.SolicitudAltaSaas;
import com.plataformaeventos.saas.modelo.SuscripcionOrganizacion;
import com.plataformaeventos.saas.repositorio.SolicitudAltaSaasRepository;
import com.plataformaeventos.saas.repositorio.SuscripcionOrganizacionRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AltaSaasService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final int MINUTOS_VERIFICACION = 30;
    private static final int HORAS_ACTIVACION = 48;
    private static final Collection<EstadoSolicitudAltaSaas> ALTAS_EN_CURSO = List.of(
            EstadoSolicitudAltaSaas.EMAIL_PENDIENTE,
            EstadoSolicitudAltaSaas.CHECKOUT_PENDIENTE,
            EstadoSolicitudAltaSaas.ACTIVACION_PENDIENTE
    );

    private final SolicitudAltaSaasRepository altas;
    private final OrganizacionRepository organizaciones;
    private final MembresiaOrganizacionRepository membresias;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final SuscripcionOrganizacionRepository suscripciones;
    private final CatalogoPlanesSaasService catalogo;
    private final StripeCheckoutGateway stripe;
    private final StripeSaasProperties stripeProperties;
    private final CorreoAltaSaas correo;
    private final PasswordEncoder passwordEncoder;

    public AltaSaasService(
            SolicitudAltaSaasRepository altas,
            OrganizacionRepository organizaciones,
            MembresiaOrganizacionRepository membresias,
            UsuarioRepository usuarios,
            RolRepository roles,
            SuscripcionOrganizacionRepository suscripciones,
            CatalogoPlanesSaasService catalogo,
            StripeCheckoutGateway stripe,
            StripeSaasProperties stripeProperties,
            CorreoAltaSaas correo,
            PasswordEncoder passwordEncoder
    ) {
        this.altas = altas;
        this.organizaciones = organizaciones;
        this.membresias = membresias;
        this.usuarios = usuarios;
        this.roles = roles;
        this.suscripciones = suscripciones;
        this.catalogo = catalogo;
        this.stripe = stripe;
        this.stripeProperties = stripeProperties;
        this.correo = correo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public java.util.List<PlanSaasPublico> planes() {
        return catalogo.listarPublicos();
    }

    @Transactional
    public AltaSaasIniciadaRespuesta iniciar(CrearAltaSaasSolicitud solicitud) {
        String correoNormalizado = normalizarCorreo(solicitud.correoContacto());
        if (altas.existsByCorreoContactoIgnoreCaseAndEstadoIn(correoNormalizado, ALTAS_EN_CURSO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un proceso de alta en curso para ese correo. Revisa tu bandeja de entrada.");
        }
        UUID id = UUID.randomUUID();
        String token = tokenSeguro();
        OffsetDateTime ahora = OffsetDateTime.now();
        SolicitudAltaSaas alta = new SolicitudAltaSaas(
                id,
                solicitud.nombreEmpresa().trim(),
                normalizarSlugOpcional(solicitud.slugSugerido()),
                solicitud.nombresContacto().trim(),
                solicitud.apellidosContacto().trim(),
                correoNormalizado,
                solicitud.plan(),
                hash(token),
                ahora.plusMinutes(MINUTOS_VERIFICACION)
        );
        try {
            altas.saveAndFlush(alta);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un proceso de alta en curso para ese correo. Revisa tu bandeja de entrada.", e);
        }
        correo.enviarVerificacion(alta, token);
        return new AltaSaasIniciadaRespuesta(id,
                "Revisa tu correo para verificarlo y continuar con el alta de la empresa.");
    }

    @Transactional
    public VerificarAltaSaasRespuesta verificarCorreo(String token) {
        SolicitudAltaSaas alta = altas.buscarPorTokenVerificacionParaActualizar(hash(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "El enlace de verificación no es válido o venció"));
        OffsetDateTime ahora = OffsetDateTime.now();

        if (alta.getEstado() == EstadoSolicitudAltaSaas.CHECKOUT_PENDIENTE
                && alta.getStripeCheckoutUrl() != null && alta.getStripeCheckoutUrl().length() > 0) {
            return new VerificarAltaSaasRespuesta(true, alta.getStripeCheckoutUrl(), "Continúa al pago seguro.");
        }
        if (!alta.tokenVerificacionVigente(ahora)) {
            throw new ResponseStatusException(HttpStatus.GONE, "El enlace de verificación no es válido o venció");
        }
        if (usuarios.findByCorreo(alta.getCorreoContacto()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este correo ya tiene una cuenta. Inicia sesión; por seguridad no asociamos organizaciones automáticamente.");
        }
        alta.verificarEmail(ahora);

        if (alta.getPlan() == PlanSuscripcion.BASICO) {
            provisionar(alta, null, null, null, ahora, null);
            return new VerificarAltaSaasRespuesta(false, null,
                    "El plan Básico quedó listo. Revisa tu correo para activar el acceso del propietario.");
        }

        if (!stripeProperties.checkoutReady()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El checkout está en modo de prueba sin credenciales configuradas.");
        }
        StripeCheckoutResultado checkout = stripe.crearSesion(alta.getId(), alta.getCorreoContacto(), alta.getPlan());
        alta.checkoutCreado(checkout.sessionId(), checkout.url(), ahora);
        altas.save(alta);
        return new VerificarAltaSaasRespuesta(true, checkout.url(), "Correo verificado. Continúa al pago seguro.");
    }

    @Transactional
    public void confirmarPagoStripe(UUID altaId, String sessionId, String customerId,
                                    String subscriptionId, String currency, Long totalMinor,
                                    OffsetDateTime eventoStripeEn) {
        SolicitudAltaSaas alta = altas.buscarPorIdParaActualizar(altaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alta SaaS no encontrada"));
        if (alta.getEstado() == EstadoSolicitudAltaSaas.ACTIVACION_PENDIENTE
                || alta.getEstado() == EstadoSolicitudAltaSaas.ACTIVA) return;
        if (alta.getEstado() != EstadoSolicitudAltaSaas.CHECKOUT_PENDIENTE
                || !sessionId.equals(alta.getStripeCheckoutSessionId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sesión no corresponde al alta pendiente");
        }
        if (!"bob".equalsIgnoreCase(currency) || totalMinor == null
                || totalMinor.longValue() != catalogo.importeMinor(alta.getPlan())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El importe/moneda del checkout no coincide con el plan");
        }
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sesión pagada no incluye suscripción mensual");
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        alta.pagoConfirmado(customerId, subscriptionId, ahora);
        provisionar(alta, customerId, subscriptionId, totalMinor, ahora, eventoStripeEn);
    }

    @Transactional
    public void checkoutExpirado(String sessionId) {
        altas.findByStripeCheckoutSessionId(sessionId).ifPresent(alta -> alta.checkoutCancelado(OffsetDateTime.now()));
    }

    @Transactional
    public void fallarCheckout(String sessionId) {
        altas.findByStripeCheckoutSessionId(sessionId).ifPresent(alta -> {
            if (alta.getEstado() == EstadoSolicitudAltaSaas.CHECKOUT_PENDIENTE) {
                alta.marcarPagoFallido(OffsetDateTime.now());
            }
        });
    }

    @Transactional
    public void activarPropietario(ActivarPropietarioSaasSolicitud solicitud) {
        SolicitudAltaSaas alta = altas.buscarPorTokenActivacionParaActualizar(hash(solicitud.token()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "El enlace de activación no es válido o venció"));
        OffsetDateTime ahora = OffsetDateTime.now();
        if (!alta.tokenActivacionVigente(ahora)) {
            throw new ResponseStatusException(HttpStatus.GONE, "El enlace de activación no es válido o venció");
        }
        Usuario propietario = usuarios.findById(alta.getIdUsuarioPropietario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta propietaria no existe"));
        propietario.actualizarClaveHash(passwordEncoder.encode(solicitud.nuevaClave()));
        propietario.actualizarEstado(EstadoUsuario.ACTIVO);
        MembresiaOrganizacion.Clave clave = new MembresiaOrganizacion.Clave(propietario.getId(), alta.getIdOrganizacion());
        MembresiaOrganizacion membresia = membresias.findById(clave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La membresía de propietario no existe"));
        membresia.activar();
        alta.activar(ahora);
    }

    @Transactional
    public void procesarEventoPagoFallido(String stripeSubscriptionId, OffsetDateTime eventoStripeEn) {
        suscripciones.buscarPorStripeSubscriptionIdParaActualizar(stripeSubscriptionId).ifPresent(suscripcion -> {
            if (suscripcion.getEstado() != EstadoSuscripcion.VENCIDA) {
                suscripcion.actualizarDesdeStripe(EstadoSuscripcion.SUSPENDIDA,
                        suscripcion.getFechaInicio(), suscripcion.getFechaFin(), eventoStripeEn);
            }
        });
    }

    @Transactional
    public void procesarRenovacionPagada(String stripeSubscriptionId, OffsetDateTime periodoFin,
                                         OffsetDateTime eventoStripeEn) {
        suscripciones.buscarPorStripeSubscriptionIdParaActualizar(stripeSubscriptionId).ifPresent(suscripcion -> {
            if (suscripcion.getEstado() != EstadoSuscripcion.VENCIDA) {
                suscripcion.actualizarDesdeStripe(EstadoSuscripcion.ACTIVA,
                        suscripcion.getFechaInicio(), periodoFin, eventoStripeEn);
            }
        });
    }

    @Transactional
    public void procesarPagoFallido(String stripeSubscriptionId, OffsetDateTime eventoStripeEn) {
        procesarEventoPagoFallido(stripeSubscriptionId, eventoStripeEn);
    }

    @Transactional
    public void procesarSuscripcionEliminada(String stripeSubscriptionId, OffsetDateTime eventoStripeEn) {
        suscripciones.buscarPorStripeSubscriptionIdParaActualizar(stripeSubscriptionId).ifPresent(suscripcion ->
                suscripcion.actualizarDesdeStripe(com.plataformaeventos.saas.modelo.EstadoSuscripcion.VENCIDA,
                        suscripcion.getFechaInicio(), OffsetDateTime.now(), eventoStripeEn));
    }

    private void provisionar(SolicitudAltaSaas alta, String stripeCustomerId, String stripeSubscriptionId,
                             Long importeMinor, OffsetDateTime ahora, OffsetDateTime eventoStripeEn) {
        if (alta.getIdOrganizacion() != null) return;

        String slug = slugDisponible(alta.getSlugSugerido(), alta.getNombreEmpresa(), alta.getId());
        Organizacion organizacion = organizaciones.saveAndFlush(new Organizacion(alta.getNombreEmpresa(), slug));
        if (usuarios.findByCorreo(alta.getCorreoContacto()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este correo ya tiene una cuenta. La organización requiere autorización de esa cuenta para asociarse.");
        }
        boolean requiereActivacion = true;
        Usuario propietario = crearPropietarioPendiente(alta);

        Rol rolAdministrador = roles.findByNombre(ROL_ADMINISTRADOR)
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR"));
        MembresiaOrganizacion membresia = new MembresiaOrganizacion(propietario, organizacion);
        membresia.asignarRoles(Set.of(rolAdministrador));
        if (requiereActivacion) membresia.inactivar();
        membresias.save(membresia);

        OffsetDateTime fechaFin = alta.getPlan() == PlanSuscripcion.BASICO ? null : ahora.plusMonths(1);
        SuscripcionOrganizacion suscripcion = new SuscripcionOrganizacion(
                organizacion, alta.getPlan(), EstadoSuscripcion.ACTIVA, ahora, fechaFin);
        if (stripeSubscriptionId != null) {
            suscripcion.vincularStripe(stripeCustomerId, stripeSubscriptionId,
                    importeMinor == null ? 0L : importeMinor, eventoStripeEn);
        }
        suscripciones.save(suscripcion);

        String tokenActivacion = requiereActivacion ? tokenSeguro() : null;
        alta.tenantProvisionado(organizacion.getId(), propietario.getId(),
                requiereActivacion ? hash(tokenActivacion) : null,
                requiereActivacion ? ahora.plusHours(HORAS_ACTIVACION) : null,
                requiereActivacion, ahora);
        altas.save(alta);
        if (requiereActivacion) correo.enviarActivacion(alta, tokenActivacion);
        else correo.enviarOrganizacionAsociada(alta);
    }

    private Usuario crearPropietarioPendiente(SolicitudAltaSaas alta) {
        String claveTemporalNoCompartida = tokenSeguro();
        Usuario usuario = new Usuario(alta.getNombresContacto(), alta.getApellidosContacto(),
                alta.getCorreoContacto(), passwordEncoder.encode(claveTemporalNoCompartida), EstadoUsuario.INACTIVO);
        return usuarios.saveAndFlush(usuario);
    }

    private String slugDisponible(String sugerido, String nombre, UUID altaId) {
        String base = normalizarSlug(sugerido == null || sugerido.isBlank() ? nombre : sugerido);
        if (base.isBlank()) base = "empresa";
        if (base.length() > 85) base = base.substring(0, 85).replaceAll("-$", "");
        if (organizaciones.findBySlug(base).isEmpty()) return base;
        String sufijo = altaId.toString().substring(0, 8);
        String conSufijo = base + "-" + sufijo;
        if (conSufijo.length() > 100) conSufijo = conSufijo.substring(0, 100);
        return conSufijo;
    }

    private String normalizarSlug(String valor) {
        String ascii = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String normalizarSlugOpcional(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String slug = normalizarSlug(valor.trim());
        return slug.isBlank() ? null : slug;
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private String tokenSeguro() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
