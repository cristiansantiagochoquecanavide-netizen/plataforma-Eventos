package com.plataformaeventos.aforo;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

@Component
public class AforoExportador {
    public byte[] csv(List<AforoService.Fila> filas, AforoFiltro filtro) {
        var out = new StringBuilder("\uFEFF");
        out.append("\"Nómina de asistencia\";\"Generada ").append(OffsetDateTime.now()).append("\"\r\n");
        out.append(celda(descripcion(filtro))).append("\r\n");
        out.append("Inscripción;Nombre;Correo;Evento;Sala;Categoría de entrada;Estado;Ingreso (con zona);Operador\r\n");
        for (var f : filas) out.append(f.inscripcionId()).append(';').append(celda(f.nombre())).append(';')
            .append(celda(f.correo())).append(';').append(celda(f.evento())).append(';').append(celda(f.sala())).append(';')
            .append(celda(f.categoria() == null ? "Sin clasificar" : f.categoria())).append(';')
            .append(f.ingreso() == null ? "Ausente" : "Asistió").append(';')
            .append(celda(f.ingreso() == null ? "" : f.ingreso().toString())).append(';').append(celda(f.staff())).append("\r\n");
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }
    static String celda(String valor) {
        String s = valor == null ? "" : valor;
        String inicio = s.stripLeading();
        if (!inicio.isEmpty() && "=+-@".indexOf(inicio.charAt(0)) >= 0 || s.startsWith("\t") || s.startsWith("\r") || s.startsWith("\n")) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    private String descripcion(AforoFiltro f) {
        return "Evento: " + (f.eventoId() == null ? "todos" : f.eventoId()) + "; Sala: " + (f.salaId() == null ? "todas" : f.salaId())
            + "; Categoría: " + (f.categoria() == null ? "todas" : f.categoria().isEmpty() ? "Sin clasificar" : f.categoria())
            + "; Desde (incluido): " + (f.desde() == null ? "sin límite" : f.desde())
            + "; Hasta (excluido): " + (f.hasta() == null ? "sin límite" : f.hasta());
    }
    public byte[] pdf(List<AforoService.Fila> filas, AforoFiltro filtro) {
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream();
             var fuente = PDType0Font.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
            if (fuente == null) throw new IllegalStateException("Fuente PDF no disponible");
            var font = PDType0Font.load(doc, fuente);
            try (var tabla = new Tabla(doc, font, descripcion(filtro))) {
                for (var f : filas) tabla.fila(List.of("#" + f.inscripcionId() + " " + f.nombre() + "\n" + f.correo(),
                    f.evento() + "\n" + (f.sala() == null ? "Sin sala" : f.sala()),
                    f.categoria() == null ? "Sin clasificar" : f.categoria(),
                    f.ingreso() == null ? "Ausente" : "Asistió\n" + f.ingreso(), f.staff() == null ? "—" : f.staff()));
                if (filas.isEmpty()) tabla.fila(List.of("Sin registros para estos filtros", "", "", "", ""));
            }
            doc.save(out); return out.toByteArray();
        } catch (IOException ex) { throw new IllegalStateException("No se pudo generar el PDF", ex); }
    }
    private static final class Tabla implements AutoCloseable {
        private final PDDocument doc; private final PDType0Font font; private final String filtros;
        private final float[] widths = {225, 170, 100, 145, 120};
        private PDPageContentStream stream; private float y; private int pagina;
        Tabla(PDDocument doc, PDType0Font font, String filtros) throws IOException { this.doc=doc; this.font=font; this.filtros=filtros; nuevaPagina(); }
        private void texto(String s, float x, float y, float size) throws IOException {
            stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(x, y); stream.showText(s); stream.endText();
        }
        private String seguro(String s) throws IOException {
            var out = new StringBuilder();
            for (int cp : s.codePoints().toArray()) {
                if (cp == '\n') { out.append('\n'); continue; }
                String c = new String(Character.toChars(cp));
                try { font.encode(c); out.append(c); } catch (IllegalArgumentException ex) { out.append('?'); }
            }
            return out.toString();
        }
        private List<String> envolver(String value, float width) throws IOException {
            var result = new ArrayList<String>();
            for (String line : seguro(value).split("\n", -1)) {
                String current = "";
                for (int cp : line.codePoints().toArray()) {
                    String next = new String(Character.toChars(cp));
                    if (!current.isEmpty() && font.getStringWidth(current+next)*9/1000 > width-10) { result.add(current); current=""; }
                    current += next;
                }
                result.add(current);
            }
            return result;
        }
        private void nuevaPagina() throws IOException {
            if (stream != null) stream.close();
            var page = new PDPage(new PDRectangle(842,595)); doc.addPage(page);
            stream = new PDPageContentStream(doc,page); pagina++;
            texto("Nómina de asistencia",40,560,15);
            texto("Página " + pagina + " · Generada " + OffsetDateTime.now(),40,542,9);
            y=526;
            for (String line: envolver(filtros,760)) { texto(line,40,y,9); y-=12; }
            texto("Asistió indica ingreso registrado; no acredita permanencia física. Horas con zona explícita.",40,y,9); y-=22;
            dibujar(List.of(List.of("Participante / correo"),List.of("Evento / sala"),List.of("Entrada"),List.of("Estado / ingreso"),List.of("Operador")),24);
        }
        void fila(List<String> values) throws IOException {
            var lines = new ArrayList<List<String>>(); int max=1;
            for(int i=0;i<values.size();i++) { var ls=envolver(values.get(i),widths[i]); lines.add(ls); max=Math.max(max,ls.size()); }
            float height=max*12+12;
            if(y-height<35) nuevaPagina();
            dibujar(lines,height);
        }
        private void dibujar(List<List<String>> lines,float height) throws IOException {
            float x=40;
            for(int i=0;i<lines.size();i++) {
                float yy=y-14;
                for(String line:lines.get(i)) { texto(line,x+4,yy,9); yy-=12; }
                stream.setStrokingColor(0.8f); stream.addRect(x,y-height,widths[i],height); stream.stroke(); x+=widths[i];
            }
            y-=height;
        }
        @Override public void close() throws IOException { if(stream!=null) stream.close(); }
    }
}
