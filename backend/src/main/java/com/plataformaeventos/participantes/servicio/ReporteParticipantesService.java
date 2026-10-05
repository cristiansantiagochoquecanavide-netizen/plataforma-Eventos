package com.plataformaeventos.participantes.servicio;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.participantes.dto.ColumnaReporteParticipantes;
import com.plataformaeventos.participantes.dto.EnviarReporteParticipantesSolicitud;
import com.plataformaeventos.participantes.dto.FormatoReporte;
import com.plataformaeventos.participantes.dto.ParticipanteResumenRespuesta;
import com.plataformaeventos.participantes.dto.ReporteParticipantesSolicitud;
import com.plataformaeventos.participantes.dto.VistaPreviaReporteParticipantesRespuesta;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReporteParticipantesService {

    private final ParticipanteService participantes;
    private final JavaMailSender correo;
    private final String remitente;

    public ReporteParticipantesService(
            ParticipanteService participantes,
            JavaMailSender correo,
            @Value("${spring.mail.properties.mail.smtp.from:${SMTP_FROM:no-reply@eventos.local}}") String remitente
    ) {
        this.participantes = participantes;
        this.correo = correo;
        this.remitente = remitente;
    }

    @Transactional(readOnly = true)
    public VistaPreviaReporteParticipantesRespuesta vistaPrevia(ReporteParticipantesSolicitud solicitud,
                                                                 Organizacion organizacion) {
        return preparar(solicitud, organizacion);
    }

    @Transactional(readOnly = true)
    public ArchivoReporte exportar(ReporteParticipantesSolicitud solicitud,
                                    FormatoReporte formato,
                                    Organizacion organizacion) {
        VistaPreviaReporteParticipantesRespuesta datos = preparar(solicitud, organizacion);
        if (datos.limiteAplicado()) {
            throw new IllegalArgumentException("El reporte supera el límite de 5000 participantes; aplica más filtros");
        }
        byte[] contenido = switch (formato) {
            case CSV -> csv(datos);
            case XLSX -> xlsx(datos);
            case HTML -> html(datos);
            case PDF -> pdf(datos);
        };
        return new ArchivoReporte("participantes." + formato.extension(), formato.contentType(), contenido);
    }

    @Transactional(readOnly = true)
    public void enviar(EnviarReporteParticipantesSolicitud solicitud, Organizacion organizacion) {
        ArchivoReporte archivo = exportar(solicitud.reporte(), solicitud.formato(), organizacion);
        try {
            var mensaje = correo.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remitente);
            helper.setTo(solicitud.correoDestino());
            helper.setSubject("Reporte de participantes - " + organizacion.getNombre());
            helper.setText("Se adjunta el reporte de participantes de la organización "
                    + organizacion.getNombre() + ".\n\nEl archivo contiene datos personales; compártelo solo con personas autorizadas.");
            helper.addAttachment(archivo.nombre(), new ByteArrayResource(archivo.contenido()), archivo.tipoContenido());
            correo.send(mensaje);
        } catch (Exception excepcion) {
            throw new IllegalStateException("No fue posible enviar el reporte por correo", excepcion);
        }
    }

    private VistaPreviaReporteParticipantesRespuesta preparar(ReporteParticipantesSolicitud solicitud,
                                                               Organizacion organizacion) {
        List<ColumnaReporteParticipantes> columnas = solicitud.columnas().stream().distinct().toList();
        if (columnas.isEmpty()) throw new IllegalArgumentException("Selecciona al menos una columna");
        List<ParticipanteResumenRespuesta> filas = participantes.listarParaReporte(
                solicitud.eventoId(), solicitud.busqueda(), solicitud.estado(), organizacion);
        boolean limiteAplicado = filas.size() > 5_000;
        if (solicitud.ordenarPor() != null) {
            Comparator<ParticipanteResumenRespuesta> comparador = (a, b) -> comparar(
                    solicitud.ordenarPor().valor(a), solicitud.ordenarPor().valor(b));
            filas = filas.stream().sorted(solicitud.ascendente() ? comparador : comparador.reversed()).toList();
        }
        if (limiteAplicado) filas = new ArrayList<>(filas.subList(0, 5_000));
        List<List<String>> valores = filas.stream()
                .map(fila -> columnas.stream().map(columna -> columna.valor(fila)).toList())
                .toList();
        return new VistaPreviaReporteParticipantesRespuesta(
                columnas.stream().map(ColumnaReporteParticipantes::encabezado).toList(),
                valores,
                valores.size(),
                limiteAplicado
        );
    }

    private byte[] csv(VistaPreviaReporteParticipantesRespuesta datos) {
        StringBuilder salida = new StringBuilder("\uFEFF");
        salida.append(datos.columnas().stream().map(this::csvCampo).collect(Collectors.joining(","))).append("\r\n");
        datos.filas().forEach(fila -> salida.append(
                fila.stream().map(this::csvCampo).collect(Collectors.joining(","))
        ).append("\r\n"));
        return salida.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvCampo(String valor) {
        String seguro = valor == null ? "" : valor;
        int primerCaracter = 0;
        while (primerCaracter < seguro.length() && Character.isWhitespace(seguro.charAt(primerCaracter))) primerCaracter++;
        if (primerCaracter < seguro.length() && "=+-@".indexOf(seguro.charAt(primerCaracter)) >= 0) seguro = "'" + seguro;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }

    private int comparar(String a, String b) {
        try {
            return Long.compare(Long.parseLong(a), Long.parseLong(b));
        } catch (NumberFormatException ignored) {
            return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        }
    }

    private byte[] xlsx(VistaPreviaReporteParticipantesRespuesta datos) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Participantes");
            escribirFila(hoja.createRow(0), datos.columnas());
            for (int i = 0; i < datos.filas().size(); i++) escribirFila(hoja.createRow(i + 1), datos.filas().get(i));
            libro.write(salida);
            return salida.toByteArray();
        } catch (Exception excepcion) {
            throw new IllegalStateException("No fue posible generar XLSX", excepcion);
        }
    }

    private void escribirFila(Row fila, List<String> valores) {
        for (int i = 0; i < valores.size(); i++) fila.createCell(i).setCellValue(valores.get(i));
    }

    private byte[] html(VistaPreviaReporteParticipantesRespuesta datos) {
        StringBuilder salida = new StringBuilder("<!doctype html><html lang=\"es\"><meta charset=\"utf-8\"><title>Reporte de participantes</title><table><thead><tr>");
        datos.columnas().forEach(c -> salida.append("<th>").append(htmlEscapar(c)).append("</th>"));
        salida.append("</tr></thead><tbody>");
        datos.filas().forEach(fila -> {
            salida.append("<tr>");
            fila.forEach(valor -> salida.append("<td>").append(htmlEscapar(valor)).append("</td>"));
            salida.append("</tr>");
        });
        salida.append("</tbody></table></html>");
        return salida.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(VistaPreviaReporteParticipantesRespuesta datos) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            List<String> lineas = new ArrayList<>();
            PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            agregarLineasPdf(lineas, "Reporte de participantes", fuente);
            agregarLineasPdf(lineas, String.join(" | ", datos.columnas()), fuente);
            for (List<String> fila : datos.filas()) agregarLineasPdf(lineas, String.join(" | ", fila), fuente);
            for (int inicio = 0; inicio < lineas.size(); inicio += 48) {
                PDPage pagina = new PDPage(PDRectangle.A4);
                documento.addPage(pagina);
                try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                    contenido.beginText();
                    contenido.setFont(fuente, 8);
                    contenido.newLineAtOffset(32, 805);
                    for (String linea : lineas.subList(inicio, Math.min(inicio + 48, lineas.size()))) {
                        contenido.showText(pdfSeguro(linea));
                        contenido.newLineAtOffset(0, -15);
                    }
                    contenido.endText();
                }
            }
            documento.save(salida);
            return salida.toByteArray();
        } catch (Exception excepcion) {
            throw new IllegalStateException("No fue posible generar PDF", excepcion);
        }
    }

    private String pdfSeguro(String texto) {
        Charset winAnsi = Charset.forName("windows-1252");
        return new String(texto.getBytes(winAnsi), winAnsi).replaceAll("[\\r\\n\\t]", " ");
    }

    private void agregarLineasPdf(List<String> destino, String texto, PDType1Font fuente) throws java.io.IOException {
        String restante = pdfSeguro(texto);
        final float anchoDisponible = PDRectangle.A4.getWidth() - 64;
        final float tamanoFuente = 8;
        while (!restante.isEmpty()) {
            int fin = restante.length();
            while (fin > 1 && fuente.getStringWidth(restante.substring(0, fin)) * tamanoFuente / 1000 > anchoDisponible) fin--;
            if (fin < restante.length()) {
                int espacio = restante.lastIndexOf(' ', fin);
                if (espacio > 0) fin = espacio;
            }
            destino.add(restante.substring(0, fin).stripTrailing());
            restante = restante.substring(fin).stripLeading();
        }
        if (texto.isEmpty()) destino.add("");
    }

    private String htmlEscapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public record ArchivoReporte(String nombre, String tipoContenido, byte[] contenido) { }
}
