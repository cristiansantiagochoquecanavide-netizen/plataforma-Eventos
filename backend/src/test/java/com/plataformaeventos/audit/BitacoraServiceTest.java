package com.plataformaeventos.audit;

import com.plataformaeventos.audit.repositorio.BitacoraRepository;
import com.plataformaeventos.audit.servicio.BitacoraService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.time.OffsetDateTime;
import static org.mockito.Mockito.*;

class BitacoraServiceTest {
    @Test
    void siempreEnvíaLaOrganizacionActivaAlFiltro() {
        BitacoraRepository repository = mock(BitacoraRepository.class);
        when(repository.buscar(eq(7L), eq("EVENTOS"), eq("EVENTO"), eq("CREAR"), eq(true), eq(3L),
                any(OffsetDateTime.class), any(OffsetDateTime.class), any())).thenReturn(Page.empty());
        new BitacoraService(repository).listar(7L, "EVENTOS", "EVENTO", "CREAR", true, 3L,
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now(), PageRequest.of(0, 10));
        verify(repository).buscar(eq(7L), eq("EVENTOS"), eq("EVENTO"), eq("CREAR"), eq(true), eq(3L),
                any(), any(), any());
    }
}
