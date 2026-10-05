package com.plataformaeventos.participantes;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.participantes.dto.ColumnaReporteParticipantes;
import com.plataformaeventos.participantes.dto.FormatoReporte;
import com.plataformaeventos.participantes.dto.ParticipanteResumenRespuesta;
import com.plataformaeventos.participantes.dto.ReporteParticipantesSolicitud;
import com.plataformaeventos.participantes.servicio.ParticipanteService;
import com.plataformaeventos.participantes.servicio.ReporteParticipantesService;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReporteParticipantesServiceTest {
    private final ParticipanteService participantes = mock(ParticipanteService.class);
    private final JavaMailSender correo = mock(JavaMailSender.class);
    private final ReporteParticipantesService reportes = new ReporteParticipantesService(
            participantes, correo, "reportes@eventos.local");
    private final Organizacion tenant = new Organizacion("Tenant A", "tenant-a");

    @Test
    void aplicaColumnasSeleccionadasOrdenFiltrosYLimiteDeFilas() {
        when(participantes.listarParaReporte(4L, "ana", EstadoUsuario.ACTIVO, tenant))
                .thenReturn(List.of(fila(12L, "Zoe"), fila(2L, "Ana")));
        ReporteParticipantesSolicitud solicitud = solicitudConFiltros(
                List.of(ColumnaReporteParticipantes.ID, ColumnaReporteParticipantes.NOMBRES),
                ColumnaReporteParticipantes.ID);

        var preview = reportes.vistaPrevia(solicitud, tenant);

        assertThat(preview.columnas()).containsExactly("ID", "Nombres");
        assertThat(preview.filas()).containsExactly(List.of("2", "Ana"), List.of("12", "Zoe"));
        assertThat(preview.totalFilas()).isEqualTo(2);
        assertThat(preview.limiteAplicado()).isFalse();
        verify(participantes).listarParaReporte(4L, "ana", EstadoUsuario.ACTIVO, tenant);
    }

    @Test
    void generaCsvXlsxHtmlYPdf() throws Exception {
        when(participantes.listarParaReporte(null, "", null, tenant))
                .thenReturn(List.of(fila(1L, "<Ana &\"Luis\""), fila(2L, " =1+1")));
        ReporteParticipantesSolicitud solicitud = solicitudSinFiltros(
                List.of(ColumnaReporteParticipantes.ID, ColumnaReporteParticipantes.NOMBRES),
                true);

        var csv = reportes.exportar(solicitud, FormatoReporte.CSV, tenant);
        assertThat(csv.tipoContenido()).contains("text/csv");
        assertThat(csv.contenido()).startsWith(new byte[] {(byte) 0xef, (byte) 0xbb, (byte) 0xbf});
        assertThat(new String(csv.contenido(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("\"ID\",\"Nombres\"")
                .contains("\"1\",\"<Ana &\"\"Luis\"\"\"")
                .contains("\"2\",\"' =1+1\"");

        var html = reportes.exportar(solicitud, FormatoReporte.HTML, tenant);
        assertThat(new String(html.contenido(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("&lt;Ana &amp;&quot;Luis&quot;");

        var xlsx = reportes.exportar(solicitud, FormatoReporte.XLSX, tenant);
        try (var libro = WorkbookFactory.create(new ByteArrayInputStream(xlsx.contenido()))) {
            assertThat(libro.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(libro.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isEqualTo("1");
        }

        var pdf = reportes.exportar(solicitud, FormatoReporte.PDF, tenant);
        try (var documento = Loader.loadPDF(pdf.contenido())) {
            assertThat(documento.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(documento)).contains("Ana");
        }
    }

    @Test
    void correoAdjuntaElFormatoSolicitado() throws Exception {
        when(participantes.listarParaReporte(null, "", null, tenant)).thenReturn(List.of(fila(1L, "Ana")));
        MimeMessage mensaje = new MimeMessage(Session.getInstance(new Properties()));
        when(correo.createMimeMessage()).thenReturn(mensaje);
        ReporteParticipantesSolicitud reporte = solicitudSinFiltros(List.of(ColumnaReporteParticipantes.NOMBRES), true);

        reportes.enviar(new com.plataformaeventos.participantes.dto.EnviarReporteParticipantesSolicitud(
                "ana@ejemplo.com", FormatoReporte.XLSX, reporte), tenant);

        verify(correo).send(mensaje);
        Multipart partes = (Multipart) mensaje.getContent();
        assertThat(partes.getCount()).isEqualTo(2);
        assertThat(partes.getBodyPart(1).getFileName()).isEqualTo("participantes.xlsx");
    }

    @Test
    void limitaLaVistaPreviaYExigeFiltrosParaExportacionesGrandes() {
        List<ParticipanteResumenRespuesta> muchasFilas = java.util.stream.LongStream.rangeClosed(1, 5_001)
                .mapToObj(id -> fila(id, "Participante " + id)).toList();
        when(participantes.listarParaReporte(null, "", null, tenant)).thenReturn(muchasFilas);
        ReporteParticipantesSolicitud reporte = new ReporteParticipantesSolicitud(
                null, "", null, List.of(ColumnaReporteParticipantes.ID), ColumnaReporteParticipantes.ID, false);

        var preview = reportes.vistaPrevia(reporte, tenant);

        assertThat(preview.filas()).hasSize(5_000);
        assertThat(preview.filas().get(0)).containsExactly("5001");
        assertThat(preview.limiteAplicado()).isTrue();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reportes.exportar(reporte, FormatoReporte.CSV, tenant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5000");
    }

    private ReporteParticipantesSolicitud solicitudConFiltros(List<ColumnaReporteParticipantes> columnas,
                                                               ColumnaReporteParticipantes ordenarPor) {
        return new ReporteParticipantesSolicitud(4L, "ana", EstadoUsuario.ACTIVO, columnas, ordenarPor, true);
    }

    private ReporteParticipantesSolicitud solicitudSinFiltros(List<ColumnaReporteParticipantes> columnas,
                                                               boolean ascendente) {
        return new ReporteParticipantesSolicitud(null, "", null, columnas, null, ascendente);
    }

    private ParticipanteResumenRespuesta fila(Long id, String nombre) {
        return new ParticipanteResumenRespuesta(id, nombre, "Apellido", "ana@ejemplo.com", EstadoUsuario.ACTIVO,
                OffsetDateTime.parse("2026-01-02T03:04:00Z"), 3, 2, 1,
                OffsetDateTime.parse("2026-02-03T04:05:00Z"));
    }
}
