package com.plataformaeventos.aforo;

import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;

class AforoExportadorTest {
    @Test void csvEscapaFormulasYPdfConservaAcentosYVariasPaginas() throws Exception {
        var exportador=new AforoExportador();var filtro=new AforoFiltro(1L,null,null,null,null);
        var filas=IntStream.range(1,65).mapToObj(i->new AforoService.Fila(i,"Ángela Muñoz "+i,"persona"+i+"@test.local","Congreso de investigación","Auditorio principal","Estudiante",OffsetDateTime.parse("2026-10-26T10:30:00-04:00"),"José Pérez")).toList();
        String csv=new String(exportador.csv(filas,filtro),StandardCharsets.UTF_8);
        assertThat(csv).contains("Ángela Muñoz 64","2026-10-26T10:30-04:00");
        assertThat(AforoExportador.celda("  =SUM(A1:A2)")).startsWith("\"'  =");
        assertThat(AforoExportador.celda("a;\"b")).isEqualTo("\"a;\"\"b\"");
        byte[] pdf=exportador.pdf(filas,filtro);
        try(var doc=Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            String texto=new PDFTextStripper().getText(doc);
            assertThat(texto).contains("Ángela Muñoz 1","Ángela Muñoz 64","José Pérez");
            var path=Path.of("target","cu24-qa");Files.createDirectories(path);Files.write(path.resolve("asistencia.pdf"),pdf);
            var renderer=new PDFRenderer(doc);
            for(int i=0;i<doc.getNumberOfPages();i++)javax.imageio.ImageIO.write(renderer.renderImageWithDPI(i,100),"png",path.resolve("pagina-"+(i+1)+".png").toFile());
        }
    }
}
