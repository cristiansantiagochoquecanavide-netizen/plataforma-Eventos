package com.plataformaeventos.aforo;

import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.seguridad.DetallesUsuarioService;
import com.plataformaeventos.seguridad.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class AforoWebSocket implements WebSocketMessageBrokerConfigurer {
    private final JwtService jwt;
    private final DetallesUsuarioService usuarios;
    private final ContextoOrganizacion contexto;
    private final String[] origenes;
    private final java.util.Map<String, java.util.Map<String,Object>> sesiones = new java.util.concurrent.ConcurrentHashMap<>();
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("clientOutboundChannel")
    private org.springframework.beans.factory.ObjectProvider<MessageChannel> salida;

    public AforoWebSocket(JwtService jwt, DetallesUsuarioService usuarios, ContextoOrganizacion contexto,
                         @Value("${seguridad.cors.origenes}") String origenes) {
        this.jwt = jwt; this.usuarios = usuarios; this.contexto = contexto;
        this.origenes = java.util.Arrays.stream(origenes.split(",")).map(String::trim).toArray(String[]::new);
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/ws-aforo").setAllowedOrigins(origenes);
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic/aforo").setTaskScheduler(aforoHeartbeat()).setHeartbeatValue(new long[]{10000,10000});
        registry.setPreservePublishOrder(true);
    }
    @org.springframework.context.annotation.Bean
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler aforoHeartbeat() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("aforo-heartbeat-"); scheduler.setPoolSize(1); return scheduler;
    }
    @org.springframework.context.event.EventListener
    public void desconectado(org.springframework.web.socket.messaging.SessionDisconnectEvent event) {
        sesiones.remove(event.getSessionId());
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ExecutorChannelInterceptor() {
            @Override public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
                var h = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (ex == null && handler instanceof org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler
                        && h != null && h.getCommand() == StompCommand.SUBSCRIBE && h.getReceipt() != null) {
                    // El broker simple no genera receipts; confirmamos cuando ya guardó la suscripción.
                    var receipt = StompHeaderAccessor.create(StompCommand.RECEIPT);
                    receipt.setSessionId(h.getSessionId()); receipt.setReceiptId(h.getReceipt());
                    salida.getObject().send(MessageBuilder.createMessage(new byte[0], receipt.getMessageHeaders()));
                }
            }
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var h = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (h == null || h.getCommand() == null) return message;
                if (h.getCommand() == StompCommand.CONNECT) {
                    String bearer = h.getFirstNativeHeader("Authorization");
                    if (bearer == null || !bearer.startsWith("Bearer ")) throw denegado();
                    String token = bearer.substring(7);
                    Authentication auth = autenticar(token);
                    var org = contexto.resolverCabecera(auth, h.getFirstNativeHeader("X-Organization-Id"));
                    if (!contexto.tieneAutoridadEnOrganizacion(auth, org.getId().toString(), "ASISTENCIA_CONSULTAR")) throw denegado();
                    h.setUser(auth);
                    h.getSessionAttributes().put("aforoToken", token);
                    h.getSessionAttributes().put("aforoOrg", org.getId().toString());
                    sesiones.put(h.getSessionId(), java.util.Map.of("aforoToken", token, "aforoOrg", org.getId().toString()));
                } else if (h.getCommand() == StompCommand.SUBSCRIBE) {
                    validar(h);
                    if (!("/topic/aforo/" + h.getSessionAttributes().get("aforoOrg")).equals(h.getDestination())) throw denegado();
                } else if (h.getCommand() != StompCommand.DISCONNECT && h.getCommand() != StompCommand.UNSUBSCRIBE) {
                    throw denegado(); // No SEND, transacciones ni destinos arbitrarios del cliente.
                }
                return message;
            }
        });
    }
    @Override public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                var h = StompHeaderAccessor.wrap(message);
                if (h.getMessageType() == org.springframework.messaging.simp.SimpMessageType.MESSAGE) {
                    try { validar(h); } catch (RuntimeException ex) { return null; }
                }
                return message;
            }
        });
    }
    private Authentication autenticar(String token) {
        var user = usuarios.loadUserByUsername(jwt.extraerCorreo(token));
        if (!user.isEnabled()) throw denegado();
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
    private void validar(StompHeaderAccessor h) {
        var attrs = sesiones.get(h.getSessionId());
        if (attrs == null || !(attrs.get("aforoToken") instanceof String token)) throw denegado();
        var auth = autenticar(token);
        if (!contexto.tieneAutoridadEnOrganizacion(auth, (String) attrs.get("aforoOrg"), "ASISTENCIA_CONSULTAR")) throw denegado();
    }
    private AccessDeniedException denegado() { return new AccessDeniedException("Monitor de aforo no autorizado"); }
}
