package com.plataformaeventos.aforo;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.mockito.Mockito.*;

class AforoNotificadorTest {
    @Configuration @EnableTransactionManagement @Import(AforoNotificador.class)
    static class Config {
        @Bean(destroyMethod="shutdown") EmbeddedDatabase db(){return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();}
        @Bean DataSourceTransactionManager tm(EmbeddedDatabase db){return new DataSourceTransactionManager(db);}
        @Bean SimpMessagingTemplate mensajes(){return mock(SimpMessagingTemplate.class);}
    }
    @Test void publicaSoloDespuesDelCommitYNuncaTrasRollback() {
        try(var ctx=new AnnotationConfigApplicationContext(Config.class)) {
            var mensajes=ctx.getBean(SimpMessagingTemplate.class);var tx=new TransactionTemplate(ctx.getBean(DataSourceTransactionManager.class));
            tx.executeWithoutResult(status->{ctx.publishEvent(new AforoActualizado(1L,5L));verifyNoInteractions(mensajes);});
            verify(mensajes).convertAndSend(eq("/topic/aforo/1"),eq(java.util.Map.of("eventoId",5L)));
            clearInvocations(mensajes);
            tx.executeWithoutResult(status->{ctx.publishEvent(new AforoActualizado(1L,5L));status.setRollbackOnly();});
            verifyNoInteractions(mensajes);
        }
    }
}
