package com.plataformaeventos.audit;

import com.plataformaeventos.audit.cifrado.BitacoraCifradoProperties;
import com.plataformaeventos.audit.cifrado.BitacoraCifradoService;
import com.plataformaeventos.audit.cifrado.BitacoraIndiceService;
import com.plataformaeventos.audit.cifrado.BitacoraPayload;
import com.plataformaeventos.audit.cifrado.BitacoraPayloadCifrado;
import com.plataformaeventos.audit.modelo.Bitacora;
import com.plataformaeventos.audit.repositorio.BitacoraRepository;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GenerateMacRequest;
import software.amazon.awssdk.services.kms.model.GenerateMacResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BitacoraServiceTest {
    @Test
    void siempreEnviaLaOrganizacionActivaAlFiltroConIndicesExactos() {
        BitacoraRepository repository = mock(BitacoraRepository.class);
        BitacoraCifradoService cifrado = mock(BitacoraCifradoService.class);
        BitacoraIndiceService indices = mock(BitacoraIndiceService.class);
        when(indices.normalizar(" EVENTOS ")).thenReturn("eventos");
        when(indices.normalizar(" EVENTO ")).thenReturn("evento");
        when(indices.normalizar(" CREAR ")).thenReturn("crear");
        when(indices.token(7L, "modulo", " EVENTOS ")).thenReturn("idx-modulo");
        when(indices.token(7L, "entidad", " EVENTO ")).thenReturn("idx-entidad");
        when(indices.token(7L, "accion", " CREAR ")).thenReturn("idx-accion");
        when(indices.token(7L, "exito", true)).thenReturn("idx-exito");
        when(indices.token(7L, "actor", 3L)).thenReturn("idx-actor");
        when(repository.buscar(eq(7L), eq("idx-modulo"), eq("eventos"), eq("idx-entidad"), eq("evento"),
                eq("idx-accion"), eq("crear"), eq("idx-exito"), eq(true), eq("idx-actor"), eq(3L),
                any(OffsetDateTime.class), any(OffsetDateTime.class), any())).thenReturn(Page.empty());

        new BitacoraService(repository, cifrado, indices).listar(7L, " EVENTOS ", " EVENTO ", " CREAR ", true, 3L,
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now(), PageRequest.of(0, 10));

        verify(repository).buscar(eq(7L), eq("idx-modulo"), eq("eventos"), eq("idx-entidad"), eq("evento"),
                eq("idx-accion"), eq("crear"), eq("idx-exito"), eq(true), eq("idx-actor"), eq(3L),
                any(), any(), any());
    }

    @Test
    void registrarPersisteSoloPayloadCifradoYCamposSensiblesLegacyNulos() {
        BitacoraRepository repository = mock(BitacoraRepository.class);
        BitacoraCifradoService cifrado = mock(BitacoraCifradoService.class);
        BitacoraIndiceService indices = mock(BitacoraIndiceService.class);
        Organizacion organizacion = mock(Organizacion.class);
        Usuario actor = mock(Usuario.class);
        when(organizacion.getId()).thenReturn(7L);
        when(actor.getId()).thenReturn(3L);
        when(cifrado.cifrar(eq(7L), any(BitacoraPayload.class)))
                .thenReturn(new BitacoraPayloadCifrado("ciphertext", null, "nonce", 2));
        when(indices.token(eq(7L), any(), any())).thenReturn("idx");

        new BitacoraService(repository, cifrado, indices)
                .registrar(actor, organizacion, "CREAR", "EVENTOS", "EVENTO", 99L, "203.0.113.10", true, "ok");

        ArgumentCaptor<Bitacora> captor = ArgumentCaptor.forClass(Bitacora.class);
        verify(repository).save(captor.capture());
        Bitacora guardada = captor.getValue();
        assertThat(guardada.estaCifrada()).isTrue();
        assertThat(guardada.getPayloadCiphertext()).isEqualTo("ciphertext");
        assertThat(guardada.getActor()).isNull();
        assertThat(guardada.getAccion()).isNull();
        assertThat(guardada.getModulo()).isNull();
        assertThat(guardada.getEntidad()).isNull();
        assertThat(guardada.getIdEntidad()).isNull();
        assertThat(guardada.getIp()).isNull();
        assertThat(guardada.getExito()).isNull();
        assertThat(guardada.getResultado()).isNull();
    }

    @Test
    void registrarFallaCerradoSiKmsNoCifraSinFallbackAPlaintext() {
        BitacoraRepository repository = mock(BitacoraRepository.class);
        BitacoraCifradoService cifrado = mock(BitacoraCifradoService.class);
        BitacoraIndiceService indices = mock(BitacoraIndiceService.class);
        Organizacion organizacion = mock(Organizacion.class);
        when(organizacion.getId()).thenReturn(7L);
        when(cifrado.cifrar(eq(7L), any(BitacoraPayload.class))).thenThrow(new IllegalStateException("kms down"));

        assertThatThrownBy(() -> new BitacoraService(repository, cifrado, indices)
                .registrar(null, organizacion, "CREAR", "EVENTOS", "EVENTO", 99L, "203.0.113.10", true, "ok"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kms down");
        verify(repository, never()).save(any());
    }

    @Test
    void listarDescifraFilasCifradasYConservaCompatibilidadLegacy() {
        BitacoraRepository repository = mock(BitacoraRepository.class);
        BitacoraCifradoService cifrado = mock(BitacoraCifradoService.class);
        BitacoraIndiceService indices = mock(BitacoraIndiceService.class);
        Organizacion organizacion = mock(Organizacion.class);
        when(organizacion.getId()).thenReturn(7L);
        Bitacora cifrada = Bitacora.cifrada(organizacion, new BitacoraPayloadCifrado("cipher", "dek", "nonce", 1),
                null, null, "idx-accion", "idx-modulo", "idx-entidad", null, "idx-exito");
        Bitacora legacy = new Bitacora(null, organizacion, "BORRAR", "EVENTOS", "EVENTO", 100L, "198.51.100.1", false, "fallo");
        when(repository.buscar(eq(7L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cifrada, legacy)));
        when(cifrado.descifrar(eq(7L), any(BitacoraPayloadCifrado.class))).thenReturn(new BitacoraPayload(
                3L, "CREAR", "EVENTOS", "EVENTO", 99L, "203.0.113.10", true, "ok"));

        var respuesta = new BitacoraService(repository, cifrado, indices)
                .listar(7L, null, null, null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(respuesta.getContent()).extracting(com.plataformaeventos.audit.dto.BitacoraRespuesta::accion).containsExactly("CREAR", "BORRAR");
        assertThat(respuesta.getContent().get(0).actor()).isEqualTo(3L);
        assertThat(respuesta.getContent().get(1).resultado()).isEqualTo("fallo");
    }

    @Test
    void indiceHmacNormalizaSeparaDominioTenantYCampoYDevuelveHex() {
        KmsClient kms = mock(KmsClient.class);
        ArgumentCaptor<GenerateMacRequest> captor = ArgumentCaptor.forClass(GenerateMacRequest.class);
        when(kms.generateMac(captor.capture())).thenReturn(GenerateMacResponse.builder()
                .mac(SdkBytes.fromByteArray(new byte[] {0x0a, 0x0b, 0x0c}))
                .build());
        BitacoraIndiceService service = new BitacoraIndiceService(kms,
                new BitacoraCifradoProperties("payload-key", "index-key", "us-east-1", "bitacora-payload"));

        String token = service.token(7L, "Modulo", " EVENTOS ");

        assertThat(token).isEqualTo("0a0b0c");
        String mensaje = new String(captor.getValue().message().asByteArray(), StandardCharsets.UTF_8);
        assertThat(mensaje).contains("bitacora-blind-index-v1", "tenant=7", "field=modulo", "value=eventos")
                .doesNotContain(" EVENTOS ");
    }
}
