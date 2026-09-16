package com.plataformaeventos.audit;

import com.plataformaeventos.audit.controlador.BitacoraController;
import com.plataformaeventos.audit.repositorio.BitacoraRepository;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;

class BitacoraApiTest {
    @Test
    void bitacoraSoloExponeLecturaYRepositorioNoTieneBorrado() {
        assertThat(java.util.Arrays.stream(BitacoraController.class.getDeclaredMethods())
                .map(Method::getName).filter(n -> n.equals("actualizar") || n.equals("eliminar"))).isEmpty();
        assertThat(java.util.Arrays.stream(BitacoraRepository.class.getDeclaredMethods())
                .map(Method::getName).filter(n -> n.startsWith("delete") || n.startsWith("update"))).isEmpty();
    }
}
