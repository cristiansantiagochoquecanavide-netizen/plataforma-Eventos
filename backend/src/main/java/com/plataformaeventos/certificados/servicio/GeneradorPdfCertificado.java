package com.plataformaeventos.certificados.servicio;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.plataformaeventos.certificados.modelo.Certificado;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;

@Component
public class GeneradorPdfCertificado {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generar(Certificado certificado, String baseUrlVerificacion) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            float width = PDRectangle.A4.getHeight();
            float height = PDRectangle.A4.getWidth();
            PDRectangle landscape = new PDRectangle(width, height);
            PDPage page = new PDPage(landscape);
            doc.addPage(page);

            PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font timesBold = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            PDType1Font timesItalic = new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);

            var inscripcion = certificado.getInscripcion();
            var evento = inscripcion.getEvento();
            var participante = inscripcion.getParticipante();
            var organizacion = certificado.getOrganizacion();

            String orgNombre = normalizar(organizacion.getNombre().toUpperCase());
            String participanteNombre = normalizar(participante.getNombres() + " " + participante.getApellidos()).toUpperCase();
            String eventoTitulo = normalizar(evento.getNombre());
            String fechaEmisionStr = certificado.getFechaEmision().format(FORMATO_FECHA);
            String codigoVerif = certificado.getCodigoVerificacion();
            String hashSeguridad = certificado.getHashSeguridad();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Marco exterior decorativo (Azul noche)
                cs.setStrokingColor(0.08f, 0.18f, 0.36f); // #152e5c
                cs.setLineWidth(4f);
                cs.addRect(24, 24, width - 48, height - 48);
                cs.stroke();

                // Marco interior fino (Dorado)
                cs.setStrokingColor(0.83f, 0.68f, 0.21f); // #d4af37
                cs.setLineWidth(1.5f);
                cs.addRect(30, 30, width - 60, height - 60);
                cs.stroke();

                // Encabezado de la Organización
                cs.setNonStrokingColor(0.2f, 0.25f, 0.3f);
                dibujarTextoCentrado(cs, helveticaBold, 14, orgNombre, width, height - 70);

                // Título Principal
                cs.setNonStrokingColor(0.08f, 0.18f, 0.36f);
                dibujarTextoCentrado(cs, timesBold, 28, "CERTIFICADO DE ASISTENCIA", width, height - 120);

                // Subtítulo
                cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
                dibujarTextoCentrado(cs, timesItalic, 13, "Se hace constar y se certifica que:", width, height - 165);

                // Nombre del Participante
                cs.setNonStrokingColor(0.05f, 0.1f, 0.25f);
                dibujarTextoCentrado(cs, helveticaBold, 22, participanteNombre, width, height - 210);

                // Línea divisoria bajo el nombre
                cs.setStrokingColor(0.83f, 0.68f, 0.21f);
                cs.setLineWidth(1.2f);
                cs.moveTo(width / 2 - 160, height - 225);
                cs.lineTo(width / 2 + 160, height - 225);
                cs.stroke();

                // Cuerpo del certificado
                cs.setNonStrokingColor(0.25f, 0.25f, 0.25f);
                dibujarTextoCentrado(cs, helvetica, 12, "Ha participado y cumplido satisfactoriamente con la asistencia al evento:", width, height - 260);

                // Nombre del Evento
                cs.setNonStrokingColor(0.08f, 0.18f, 0.36f);
                dibujarTextoCentrado(cs, helveticaBold, 18, eventoTitulo, width, height - 295);

                // Fecha de emisión
                cs.setNonStrokingColor(0.35f, 0.35f, 0.35f);
                dibujarTextoCentrado(cs, helvetica, 11, "Emitido el: " + fechaEmisionStr, width, height - 340);

                // QR Code
                String urlVerificacion = baseUrlVerificacion + "/verificar-certificado/" + codigoVerif;
                BufferedImage qrImg = generarQr(urlVerificacion, 110, 110);
                PDImageXObject pdQr = LosslessFactory.createFromImage(doc, qrImg);
                float qrX = width - 170;
                float qrY = 48;
                cs.drawImage(pdQr, qrX, qrY, 100, 100);

                // Pie de página con código y hash
                cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
                cs.beginText();
                cs.setFont(helveticaBold, 9);
                cs.newLineAtOffset(50, 95);
                cs.showText("Codigo de Verificacion: " + codigoVerif);
                cs.newLineAtOffset(0, -14);
                cs.setFont(helvetica, 7.5f);
                cs.showText("Hash Criptografico SHA-256: " + hashSeguridad.substring(0, Math.min(hashSeguridad.length(), 48)) + "...");
                cs.newLineAtOffset(0, -14);
                cs.showText("Para validar la autenticidad de este documento escanee el codigo QR o ingrese el codigo en el portal oficial.");
                cs.endText();
            }

            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el certificado PDF", e);
        }
    }

    private void dibujarTextoCentrado(PDPageContentStream cs, PDType1Font font, float fontSize, String text, float pageWidth, float y) throws Exception {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (pageWidth - textWidth) / 2;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private BufferedImage generarQr(String contenido, int ancho, int alto) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, ancho, alto);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String descompuesto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        String sinDiacriticos = descompuesto.replaceAll("\\p{M}", "");
        Charset winAnsi = StandardCharsets.ISO_8859_1;
        return new String(sinDiacriticos.getBytes(winAnsi), winAnsi).replaceAll("[\\r\\n\\t]", " ");
    }
}
