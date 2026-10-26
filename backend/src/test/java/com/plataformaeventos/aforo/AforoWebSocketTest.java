package com.plataformaeventos.aforo;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.seguridad.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes=AforoWebSocketTest.App.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"seguridad.cors.origenes=http://localhost:4200","spring.main.banner-mode=off"})
class AforoWebSocketTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,FlywayAutoConfiguration.class})
    @Import({AforoWebSocket.class,AforoController.class,SeguridadConfiguracion.class,JwtFiltro.class,ManejadorAccesoNoAutorizado.class,ManejadorAccesoDenegado.class})
    static class App {
        @Bean JwtService jwt(){return mock(JwtService.class);}
        @Bean DetallesUsuarioService usuarios(){return mock(DetallesUsuarioService.class);}
        @Bean(name="contextoOrganizacion") ContextoOrganizacion contexto(){return mock(ContextoOrganizacion.class);}
        @Bean AforoService servicio(){return mock(AforoService.class);}
        @Bean AforoExportador exportador(){return new AforoExportador();}
    }
    @LocalServerPort int port;
    @Autowired JwtService jwt; @Autowired DetallesUsuarioService usuarios; @Autowired ContextoOrganizacion contexto;
    @Autowired SimpMessagingTemplate mensajes;
    @Autowired org.springframework.boot.test.web.client.TestRestTemplate rest;
    @Autowired AforoService servicio;
    WebSocketStompClient client; ThreadPoolTaskScheduler scheduler;
    final List<StompSession> sesiones=new ArrayList<>();
    @BeforeEach void preparar() {
        reset(jwt,usuarios,contexto);
        when(jwt.extraerCorreo("token-a")).thenReturn("a@test");
        when(jwt.extraerCorreo("token-b")).thenReturn("b@test");
        when(jwt.extraerCorreo("invalido")).thenThrow(new IllegalArgumentException("Token invalido"));
        when(usuarios.loadUserByUsername(anyString())).thenAnswer(i->User.withUsername(i.getArgument(0)).password("unused").authorities("ORGANIZADOR").build());
        when(contexto.resolverCabecera(any(),anyString())).thenAnswer(i->{
            String name=((org.springframework.security.core.Authentication)i.getArgument(0)).getName();
            long id=name.equals("a@test")?1:2;
            if(!Long.toString(id).equals(i.getArgument(1)))throw new org.springframework.security.access.AccessDeniedException("Otra organizacion");
            var org=mock(Organizacion.class);when(org.getId()).thenReturn(id);return org;
        });
        when(contexto.tieneAutoridadEnOrganizacion(any(),anyString(),eq("ASISTENCIA_CONSULTAR"))).thenAnswer(i->
            (((org.springframework.security.core.Authentication)i.getArgument(0)).getName().equals("a@test")?"1":"2").equals(i.getArgument(1)));
        scheduler=new ThreadPoolTaskScheduler();scheduler.initialize();
        client=new WebSocketStompClient(new StandardWebSocketClient());client.setMessageConverter(new StringMessageConverter());client.setTaskScheduler(scheduler);
    }
    @AfterEach void cerrar(){for(var s:sesiones)if(s.isConnected())s.disconnect();client.stop();scheduler.shutdown();}
    StompSession conectar(String token,String org,BlockingQueue<String> errores) throws Exception {
        var headers=new StompHeaders();headers.add("Authorization","Bearer "+token);headers.add("X-Organization-Id",org);
        var session=client.connectAsync("ws://localhost:"+port+"/api/ws-aforo",new org.springframework.web.socket.WebSocketHttpHeaders(),headers,new StompSessionHandlerAdapter(){
            @Override public Type getPayloadType(StompHeaders h){return String.class;}
            @Override public void handleFrame(StompHeaders h,Object payload){errores.add(payload.toString());}
            @Override public void handleTransportError(StompSession session,Throwable ex){errores.offer("cerrado");}
        }).get(10,TimeUnit.SECONDS);sesiones.add(session);return session;
    }
    BlockingQueue<String> suscribir(StompSession s,String destino) throws Exception {
        var cola=new LinkedBlockingQueue<String>();var ready=new CountDownLatch(1);
        var h=new StompHeaders();h.setDestination(destino);h.setReceipt(UUID.randomUUID().toString());
        s.subscribe(h,new StompFrameHandler(){
            @Override public Type getPayloadType(StompHeaders headers){return String.class;}
            @Override public void handleFrame(StompHeaders headers,Object body){cola.add(body.toString());}
        }).addReceiptTask(ready::countDown);
        assertThat(ready.await(5,TimeUnit.SECONDS)).as("Suscripción confirmada antes de obtener instantánea").isTrue();return cola;
    }
    @Test void transmiteSoloAlTenantAutorizadoYRecuperaTrasReconectar() throws Exception {
        var a=conectar("token-a","1",new LinkedBlockingQueue<>());var b=conectar("token-b","2",new LinkedBlockingQueue<>());
        var qa=suscribir(a,"/topic/aforo/1");var qb=suscribir(b,"/topic/aforo/2");
        mensajes.convertAndSend("/topic/aforo/1","cambio-1");
        assertThat(qa.poll(5,TimeUnit.SECONDS)).isEqualTo("cambio-1");assertThat(qb.poll(300,TimeUnit.MILLISECONDS)).isNull();
        a.disconnect();var nueva=conectar("token-a","1",new LinkedBlockingQueue<>());var qn=suscribir(nueva,"/topic/aforo/1");
        mensajes.convertAndSend("/topic/aforo/1","cambio-2");assertThat(qn.poll(5,TimeUnit.SECONDS)).isEqualTo("cambio-2");
        when(jwt.extraerCorreo("token-a")).thenThrow(new IllegalArgumentException("Token expirado"));
        mensajes.convertAndSend("/topic/aforo/1","privado");assertThat(qn.poll(300,TimeUnit.MILLISECONDS)).isNull();
    }
    @Test void rechazaOtraOrganizacionDestinosAjenosYPublicaciones() throws Exception {
        assertThatThrownBy(()->conectar("token-a","2",new LinkedBlockingQueue<>())).isInstanceOf(Exception.class);
        assertThatThrownBy(()->conectar("invalido","1",new LinkedBlockingQueue<>())).isInstanceOf(Exception.class);
        var errores=new LinkedBlockingQueue<String>();var a=conectar("token-a","1",errores);
        a.subscribe("/topic/aforo/2",new StompFrameHandler(){ public Type getPayloadType(StompHeaders h){return String.class;} public void handleFrame(StompHeaders h,Object o){} });
        assertThat(errores.poll(5,TimeUnit.SECONDS)).isNotNull();
        var erroresSend=new LinkedBlockingQueue<String>();var s=conectar("token-a","1",erroresSend);
        s.send("/topic/aforo/1","falso");assertThat(erroresSend.poll(5,TimeUnit.SECONDS)).isNotNull();
    }
    @Test void restYExportacionesRequierenPermisoYTenant() {
        var headers=new org.springframework.http.HttpHeaders();headers.setBearerAuth("token-a");headers.set("X-Organization-Id","1");
        var entidad=new org.springframework.http.HttpEntity<Void>(headers);
        assertThat(rest.getForEntity("/api/aforo",String.class).getStatusCode().value()).isEqualTo(401);
        for(String ruta:List.of("/api/aforo","/api/aforo/exportar?formato=csv","/api/aforo/exportar?formato=pdf"))
            assertThat(rest.exchange(ruta,org.springframework.http.HttpMethod.GET,entidad,String.class).getStatusCode().value()).isEqualTo(403);
        when(contexto.tieneAutoridad(any(),any(),eq("ASISTENCIA_CONSULTAR"))).thenAnswer(i->"1".equals(((jakarta.servlet.http.HttpServletRequest)i.getArgument(1)).getHeader("X-Organization-Id")));
        var organizacionDemo=mock(Organizacion.class);when(organizacionDemo.getId()).thenReturn(1L);when(contexto.resolver(any(),any())).thenReturn(organizacionDemo);
        when(servicio.panel(eq(1L),any(),eq(0))).thenReturn(new AforoService.Panel(List.of(),new AforoService.Nomina(List.of(),0,0,25),java.time.OffsetDateTime.now()));
        assertThat(rest.exchange("/api/aforo",org.springframework.http.HttpMethod.GET,entidad,String.class).getStatusCode().value()).isEqualTo(200);
        headers.set("X-Organization-Id","2");
        assertThat(rest.exchange("/api/aforo/exportar?formato=csv",org.springframework.http.HttpMethod.GET,new org.springframework.http.HttpEntity<Void>(headers),String.class).getStatusCode().value()).isEqualTo(403);
    }
}
