package com.plataformaeventos.reportes.servicio;

import com.plataformaeventos.reportes.dto.ExportarReporteDinamicoSolicitud;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportadorReporteService {

    private static final Color COLOR_PRIMARIO = new Color(18, 60, 51);       // #123c33
    private static final Color COLOR_ACENTO = new Color(25, 113, 92);        // #19715c
    private static final Color COLOR_FONDO_SUAVE = new Color(243, 247, 245); // #f3f7f5
    private static final Color COLOR_ZEBRA = new Color(247, 250, 249);       // #f7faf9
    private static final Color COLOR_BORDE = new Color(214, 224, 220);       // #d6e0dc
    private static final Color COLOR_TEXTO = new Color(28, 37, 34);          // #1c2522
    private static final Color COLOR_MUTED = new Color(95, 110, 105);        // #5f6e69

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] exportarPdf(ExportarReporteDinamicoSolicitud req, String nombreOrganizacion) {
        String titulo = textoSeguro(req.titulo(), "Informe Ejecutivo");
        String subtitulo = textoSeguro(req.subtitulo(), "");
        String periodo = textoSeguro(req.periodo(), "Datos vigentes");
        String org = textoSeguro(nombreOrganizacion, "Organizacion");
        String fechaGen = LocalDateTime.now().format(FORMATO_FECHA);

        List<String> columnas = req.columnas() != null ? req.columnas() : List.of();
        List<List<String>> filas = req.filas() != null ? req.filas() : List.of();
        boolean horizontal = columnas.size() >= 6;
        PDRectangle formatoPagina = horizontal
                ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
                : PDRectangle.A4;

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float margen = 36f;
            float anchoUtil = formatoPagina.getWidth() - (margen * 2);
            float altoPagina = formatoPagina.getHeight();
            float yMinimo = 48f;

            PDPage pagina = new PDPage(formatoPagina);
            doc.addPage(pagina);
            PDPageContentStream cs = new PDPageContentStream(doc, pagina);

            // Banner superior corporativo
            float altoBanner = 68f;
            cs.setNonStrokingColor(COLOR_PRIMARIO);
            cs.addRect(0, altoPagina - altoBanner, formatoPagina.getWidth(), altoBanner);
            cs.fill();

            escribirTexto(cs, fontBold, 15, Color.WHITE, margen, altoPagina - 28, truncarTexto(titulo, fontBold, 15, anchoUtil - 140));
            String meta = org + "  |  " + periodo + "  |  Generado: " + fechaGen;
            escribirTexto(cs, fontRegular, 8.5f, new Color(210, 232, 224), margen, altoPagina - 46, truncarTexto(meta, fontRegular, 8.5f, anchoUtil));
            if (!subtitulo.isBlank()) {
                escribirTexto(cs, fontRegular, 8f, new Color(185, 215, 205), margen, altoPagina - 59, truncarTexto(subtitulo, fontRegular, 8f, anchoUtil));
            }

            float y = altoPagina - altoBanner - 20f;

            // KPIs en tarjetas
            List<ExportarReporteDinamicoSolicitud.KpiItem> kpis = req.kpis() != null ? req.kpis() : List.of();
            if (!kpis.isEmpty()) {
                int colsKpi = Math.min(kpis.size(), 4);
                float gap = 10f;
                float anchoCard = (anchoUtil - gap * (colsKpi - 1)) / colsKpi;
                float altoCard = 46f;

                for (int i = 0; i < colsKpi; i++) {
                    ExportarReporteDinamicoSolicitud.KpiItem kpi = kpis.get(i);
                    float xCard = margen + i * (anchoCard + gap);
                    cs.setNonStrokingColor(COLOR_FONDO_SUAVE);
                    cs.addRect(xCard, y - altoCard, anchoCard, altoCard);
                    cs.fill();

                    cs.setStrokingColor(COLOR_BORDE);
                    cs.setLineWidth(0.6f);
                    cs.addRect(xCard, y - altoCard, anchoCard, altoCard);
                    cs.stroke();

                    cs.setNonStrokingColor(COLOR_ACENTO);
                    cs.addRect(xCard, y - altoCard, 3f, altoCard);
                    cs.fill();

                    escribirTexto(cs, fontRegular, 7.5f, COLOR_MUTED, xCard + 9, y - 13,
                            truncarTexto(textoSeguro(kpi.etiqueta(), ""), fontRegular, 7.5f, anchoCard - 16));
                    escribirTexto(cs, fontBold, 12.5f, COLOR_PRIMARIO, xCard + 9, y - 28,
                            truncarTexto(textoSeguro(kpi.valor(), "-"), fontBold, 12.5f, anchoCard - 16));
                    if (kpi.detalle() != null && !kpi.detalle().isBlank()) {
                        escribirTexto(cs, fontRegular, 7f, COLOR_MUTED, xCard + 9, y - 39,
                                truncarTexto(textoSeguro(kpi.detalle(), ""), fontRegular, 7f, anchoCard - 16));
                    }
                }
                y -= (altoCard + 16f);
            }

            // Resumen Ejecutivo
            if (req.resumenEjecutivo() != null && !req.resumenEjecutivo().isBlank()) {
                List<String> lineasResumen = envolverTexto(req.resumenEjecutivo(), fontRegular, 9.2f, anchoUtil - 20);
                float altoBox = 22f + lineasResumen.size() * 12.5f + 8f;
                if (y - altoBox < yMinimo) {
                    cs.close();
                    pagina = new PDPage(formatoPagina);
                    doc.addPage(pagina);
                    cs = new PDPageContentStream(doc, pagina);
                    y = altoPagina - margen;
                }
                cs.setNonStrokingColor(COLOR_FONDO_SUAVE);
                cs.addRect(margen, y - altoBox, anchoUtil, altoBox);
                cs.fill();
                cs.setStrokingColor(COLOR_BORDE);
                cs.setLineWidth(0.5f);
                cs.addRect(margen, y - altoBox, anchoUtil, altoBox);
                cs.stroke();

                escribirTexto(cs, fontBold, 9.5f, COLOR_PRIMARIO, margen + 10, y - 15, "RESUMEN EJECUTIVO");
                float yLinea = y - 29;
                for (String linea : lineasResumen) {
                    escribirTexto(cs, fontRegular, 9.2f, COLOR_TEXTO, margen + 10, yLinea, linea);
                    yLinea -= 12.5f;
                }
                y -= (altoBox + 16f);
            }

            // Tabla de Datos
            if (!columnas.isEmpty()) {
                float anchoCol = anchoUtil / columnas.size();
                float altoCabecera = 22f;
                float altoFila = 19f;

                if (y - altoCabecera - altoFila < yMinimo) {
                    cs.close();
                    pagina = new PDPage(formatoPagina);
                    doc.addPage(pagina);
                    cs = new PDPageContentStream(doc, pagina);
                    y = altoPagina - margen;
                }

                dibujarCabeceraTabla(cs, fontBold, columnas, margen, y, anchoUtil, anchoCol, altoCabecera);
                y -= altoCabecera;

                if (filas.isEmpty()) {
                    escribirTexto(cs, fontRegular, 9f, COLOR_MUTED, margen + 8, y - 13, "No hay registros disponibles para los filtros aplicados.");
                    y -= 24f;
                } else {
                    for (int idx = 0; idx < filas.size(); idx++) {
                        if (y - altoFila < yMinimo) {
                            cs.close();
                            pagina = new PDPage(formatoPagina);
                            doc.addPage(pagina);
                            cs = new PDPageContentStream(doc, pagina);
                            y = altoPagina - margen;
                            dibujarCabeceraTabla(cs, fontBold, columnas, margen, y, anchoUtil, anchoCol, altoCabecera);
                            y -= altoCabecera;
                        }
                        if (idx % 2 == 1) {
                            cs.setNonStrokingColor(COLOR_ZEBRA);
                            cs.addRect(margen, y - altoFila, anchoUtil, altoFila);
                            cs.fill();
                        }
                        List<String> fila = filas.get(idx);
                        for (int c = 0; c < columnas.size(); c++) {
                            String val = c < fila.size() ? textoSeguro(fila.get(c), "-") : "-";
                            escribirTexto(cs, fontRegular, 8f, COLOR_TEXTO,
                                    margen + c * anchoCol + 5, y - 13,
                                    truncarTexto(val, fontRegular, 8f, anchoCol - 10));
                        }
                        cs.setStrokingColor(COLOR_BORDE);
                        cs.setLineWidth(0.35f);
                        cs.moveTo(margen, y - altoFila);
                        cs.lineTo(margen + anchoUtil, y - altoFila);
                        cs.stroke();
                        y -= altoFila;
                    }
                }
                y -= 16f;
            }

            // Conclusiones y Recomendaciones
            List<String> conclusiones = req.conclusiones() != null ? req.conclusiones() : List.of();
            List<String> recomendaciones = req.recomendaciones() != null ? req.recomendaciones() : List.of();

            if (!conclusiones.isEmpty()) {
                y = escribirSeccionLista(doc, cs, formatoPagina, fontBold, fontRegular, "HALLAZGOS Y CONCLUSIONES", conclusiones, margen, anchoUtil, y, yMinimo);
                cs = null; // escribirSeccionLista devuelve stream en pagina actual si hace falta -> manejamos con wrapper
            }
            if (cs != null) {
                cs.close();
            }

            // Si había conclusiones o recomendaciones, lo hacemos con helper seguro sobre el documento
            if (!recomendaciones.isEmpty()) {
                agregarSeccionEnUltimaPagina(doc, formatoPagina, fontBold, fontRegular, "RECOMENDACIONES ACCIONABLES", recomendaciones, margen, anchoUtil, y, yMinimo);
            }

            // Pie de página con numeración en todas las páginas
            int totalPaginas = doc.getNumberOfPages();
            for (int p = 0; p < totalPaginas; p++) {
                PDPage pag = doc.getPage(p);
                try (PDPageContentStream footer = new PDPageContentStream(doc, pag, PDPageContentStream.AppendMode.APPEND, true)) {
                    footer.setStrokingColor(COLOR_BORDE);
                    footer.setLineWidth(0.5f);
                    footer.moveTo(margen, 34);
                    footer.lineTo(formatoPagina.getWidth() - margen, 34);
                    footer.stroke();

                    escribirTexto(footer, fontRegular, 7.5f, COLOR_MUTED, margen, 21,
                            "Plataforma de Gestion de Eventos  |  " + org);
                    String pagStr = "Pagina " + (p + 1) + " de " + totalPaginas;
                    float wPag = fontRegular.getStringWidth(pdfSeguro(pagStr)) * 7.5f / 1000f;
                    escribirTexto(footer, fontRegular, 7.5f, COLOR_MUTED, formatoPagina.getWidth() - margen - wPag, 21, pagStr);
                }
            }

            doc.save(salida);
            return salida.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible generar el reporte en PDF: " + ex.getMessage(), ex);
        }
    }

    public byte[] exportarXlsx(ExportarReporteDinamicoSolicitud req, String nombreOrganizacion) {
        String titulo = textoSeguro(req.titulo(), "Informe Ejecutivo");
        String periodo = textoSeguro(req.periodo(), "Datos vigentes");
        String org = textoSeguro(nombreOrganizacion, "Organizacion");
        String fechaGen = LocalDateTime.now().format(FORMATO_FECHA);

        List<String> columnas = req.columnas() != null ? req.columnas() : List.of();
        List<List<String>> filas = req.filas() != null ? req.filas() : List.of();

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            // Estilos reutilizables
            XSSFCellStyle estiloTitulo = wb.createCellStyle();
            XSSFFont fuenteTitulo = wb.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());
            estiloTitulo.setFont(fuenteTitulo);
            estiloTitulo.setFillForegroundColor(new XSSFColor(new byte[]{18, 60, 51}, null));
            estiloTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloTitulo.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFCellStyle estiloMeta = wb.createCellStyle();
            XSSFFont fuenteMeta = wb.createFont();
            fuenteMeta.setItalic(true);
            fuenteMeta.setFontHeightInPoints((short) 10);
            fuenteMeta.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
            estiloMeta.setFont(fuenteMeta);

            XSSFCellStyle estiloCabecera = wb.createCellStyle();
            XSSFFont fuenteCabecera = wb.createFont();
            fuenteCabecera.setBold(true);
            fuenteCabecera.setFontHeightInPoints((short) 10);
            fuenteCabecera.setColor(IndexedColors.WHITE.getIndex());
            estiloCabecera.setFont(fuenteCabecera);
            estiloCabecera.setFillForegroundColor(new XSSFColor(new byte[]{25, 113, 92}, null));
            estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloCabecera.setAlignment(HorizontalAlignment.LEFT);
            estiloCabecera.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordes(estiloCabecera);

            XSSFCellStyle estiloCelda = wb.createCellStyle();
            estiloCelda.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordes(estiloCelda);

            XSSFCellStyle estiloZebra = wb.createCellStyle();
            estiloZebra.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 243, (byte) 247, (byte) 245}, null));
            estiloZebra.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloZebra.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordes(estiloZebra);

            XSSFCellStyle estiloSeccion = wb.createCellStyle();
            XSSFFont fuenteSeccion = wb.createFont();
            fuenteSeccion.setBold(true);
            fuenteSeccion.setFontHeightInPoints((short) 11);
            estiloSeccion.setFont(fuenteSeccion);
            estiloSeccion.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 225, (byte) 238, (byte) 233}, null));
            estiloSeccion.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // HOJA 1: Datos del Reporte
            Sheet hojaDatos = wb.createSheet("Datos del Reporte");
            int numCols = Math.max(columnas.size(), 2);

            Row rTitulo = hojaDatos.createRow(0);
            rTitulo.setHeightInPoints(28);
            Cell cTitulo = rTitulo.createCell(0);
            cTitulo.setCellValue(titulo);
            cTitulo.setCellStyle(estiloTitulo);
            if (numCols > 1) {
                hojaDatos.addMergedRegion(new CellRangeAddress(0, 0, 0, numCols - 1));
            }

            Row rMeta = hojaDatos.createRow(1);
            Cell cMeta = rMeta.createCell(0);
            cMeta.setCellValue(org + " · " + periodo + " · Generado: " + fechaGen);
            cMeta.setCellStyle(estiloMeta);
            if (numCols > 1) {
                hojaDatos.addMergedRegion(new CellRangeAddress(1, 1, 0, numCols - 1));
            }

            int filaActual = 3;
            if (!columnas.isEmpty()) {
                Row rCab = hojaDatos.createRow(filaActual++);
                rCab.setHeightInPoints(22);
                for (int i = 0; i < columnas.size(); i++) {
                    Cell cell = rCab.createCell(i);
                    cell.setCellValue(columnas.get(i));
                    cell.setCellStyle(estiloCabecera);
                }

                for (int idx = 0; idx < filas.size(); idx++) {
                    Row r = hojaDatos.createRow(filaActual++);
                    List<String> valores = filas.get(idx);
                    CellStyle st = (idx % 2 == 1) ? estiloZebra : estiloCelda;
                    for (int c = 0; c < columnas.size(); c++) {
                        Cell cell = r.createCell(c);
                        String v = c < valores.size() && valores.get(c) != null ? valores.get(c) : "-";
                        escribirValorTipado(cell, v);
                        cell.setCellStyle(st);
                    }
                }

                hojaDatos.setAutoFilter(new CellRangeAddress(3, Math.max(3, filaActual - 1), 0, columnas.size() - 1));
                hojaDatos.createFreezePane(0, 4);

                for (int i = 0; i < columnas.size(); i++) {
                    hojaDatos.autoSizeColumn(i);
                    int ancho = hojaDatos.getColumnWidth(i);
                    hojaDatos.setColumnWidth(i, Math.min(Math.max(ancho + 800, 3500), 14000));
                }
            }

            // HOJA 2: Resumen Ejecutivo (si tiene contenido analítico)
            boolean tieneResumen = (req.resumenEjecutivo() != null && !req.resumenEjecutivo().isBlank())
                    || (req.kpis() != null && !req.kpis().isEmpty())
                    || (req.conclusiones() != null && !req.conclusiones().isEmpty())
                    || (req.recomendaciones() != null && !req.recomendaciones().isEmpty());

            if (tieneResumen) {
                Sheet hojaRes = wb.createSheet("Resumen Ejecutivo");
                int rowIdx = 0;

                Row rt = hojaRes.createRow(rowIdx++);
                rt.setHeightInPoints(26);
                Cell ct = rt.createCell(0);
                ct.setCellValue(titulo + " — Análisis Ejecutivo");
                ct.setCellStyle(estiloTitulo);
                hojaRes.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

                rowIdx++;
                if (req.resumenEjecutivo() != null && !req.resumenEjecutivo().isBlank()) {
                    Row rs = hojaRes.createRow(rowIdx++);
                    Cell csTitle = rs.createCell(0);
                    csTitle.setCellValue("RESUMEN EJECUTIVO");
                    csTitle.setCellStyle(estiloSeccion);
                    hojaRes.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));

                    Row rBody = hojaRes.createRow(rowIdx++);
                    Cell cBody = rBody.createCell(0);
                    cBody.setCellValue(req.resumenEjecutivo());
                    XSSFCellStyle wrap = wb.createCellStyle();
                    wrap.setWrapText(true);
                    cBody.setCellStyle(wrap);
                    hojaRes.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
                    rBody.setHeightInPoints(48);
                    rowIdx++;
                }

                if (req.kpis() != null && !req.kpis().isEmpty()) {
                    Row rk = hojaRes.createRow(rowIdx++);
                    Cell ck0 = rk.createCell(0); ck0.setCellValue("Indicador (KPI)"); ck0.setCellStyle(estiloCabecera);
                    Cell ck1 = rk.createCell(1); ck1.setCellValue("Valor"); ck1.setCellStyle(estiloCabecera);
                    Cell ck2 = rk.createCell(2); ck2.setCellValue("Detalle"); ck2.setCellStyle(estiloCabecera);

                    for (ExportarReporteDinamicoSolicitud.KpiItem kpi : req.kpis()) {
                        Row r = hojaRes.createRow(rowIdx++);
                        Cell a = r.createCell(0); a.setCellValue(textoSeguro(kpi.etiqueta(), "")); a.setCellStyle(estiloCelda);
                        Cell b = r.createCell(1); b.setCellValue(textoSeguro(kpi.valor(), "-")); b.setCellStyle(estiloCelda);
                        Cell c = r.createCell(2); c.setCellValue(textoSeguro(kpi.detalle(), "")); c.setCellStyle(estiloCelda);
                    }
                    rowIdx++;
                }

                if (req.conclusiones() != null && !req.conclusiones().isEmpty()) {
                    Row rc = hojaRes.createRow(rowIdx++);
                    Cell cc = rc.createCell(0); cc.setCellValue("HALLAZGOS Y CONCLUSIONES"); cc.setCellStyle(estiloSeccion);
                    hojaRes.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
                    for (String item : req.conclusiones()) {
                        Row r = hojaRes.createRow(rowIdx++);
                        r.createCell(0).setCellValue("• " + item);
                        hojaRes.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
                    }
                    rowIdx++;
                }

                if (req.recomendaciones() != null && !req.recomendaciones().isEmpty()) {
                    Row rr = hojaRes.createRow(rowIdx++);
                    Cell cr = rr.createCell(0); cr.setCellValue("RECOMENDACIONES ACCIONABLES"); cr.setCellStyle(estiloSeccion);
                    hojaRes.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
                    for (String item : req.recomendaciones()) {
                        Row r = hojaRes.createRow(rowIdx++);
                        r.createCell(0).setCellValue("• " + item);
                        hojaRes.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 2));
                    }
                }

                hojaRes.setColumnWidth(0, 9000);
                hojaRes.setColumnWidth(1, 5500);
                hojaRes.setColumnWidth(2, 11000);
            }

            wb.write(salida);
            return salida.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible generar el reporte en XLSX: " + ex.getMessage(), ex);
        }
    }

    private void dibujarCabeceraTabla(
            PDPageContentStream cs, PDType1Font fontBold, List<String> columnas,
            float margen, float y, float anchoUtil, float anchoCol, float altoCabecera
    ) throws IOException {
        cs.setNonStrokingColor(COLOR_PRIMARIO);
        cs.addRect(margen, y - altoCabecera, anchoUtil, altoCabecera);
        cs.fill();
        for (int c = 0; c < columnas.size(); c++) {
            escribirTexto(cs, fontBold, 8.2f, Color.WHITE,
                    margen + c * anchoCol + 5, y - 14,
                    truncarTexto(textoSeguro(columnas.get(c), ""), fontBold, 8.2f, anchoCol - 10));
        }
    }

    private float escribirSeccionLista(
            PDDocument doc, PDPageContentStream csActual, PDRectangle formato,
            PDType1Font fontBold, PDType1Font fontRegular,
            String tituloSeccion, List<String> items,
            float margen, float anchoUtil, float yActual, float yMinimo
    ) throws IOException {
        PDPageContentStream cs = csActual;
        float y = yActual;
        if (y - 35 < yMinimo) {
            cs.close();
            PDPage nueva = new PDPage(formato);
            doc.addPage(nueva);
            cs = new PDPageContentStream(doc, nueva);
            y = formato.getHeight() - margen;
        }
        escribirTexto(cs, fontBold, 9.5f, COLOR_PRIMARIO, margen, y - 10, tituloSeccion);
        y -= 24f;
        for (String item : items) {
            List<String> lineas = envolverTexto("- " + item, fontRegular, 8.8f, anchoUtil - 8);
            for (String l : lineas) {
                if (y - 14 < yMinimo) {
                    cs.close();
                    PDPage nueva = new PDPage(formato);
                    doc.addPage(nueva);
                    cs = new PDPageContentStream(doc, nueva);
                    y = formato.getHeight() - margen;
                }
                escribirTexto(cs, fontRegular, 8.8f, COLOR_TEXTO, margen + 6, y, l);
                y -= 12.5f;
            }
            y -= 2f;
        }
        cs.close();
        return y - 8f;
    }

    private void agregarSeccionEnUltimaPagina(
            PDDocument doc, PDRectangle formato,
            PDType1Font fontBold, PDType1Font fontRegular,
            String tituloSeccion, List<String> items,
            float margen, float anchoUtil, float yActual, float yMinimo
    ) throws IOException {
        PDPage ultima = doc.getPage(doc.getNumberOfPages() - 1);
        PDPageContentStream cs;
        float y = yActual;
        if (y - 40 < yMinimo) {
            PDPage nueva = new PDPage(formato);
            doc.addPage(nueva);
            cs = new PDPageContentStream(doc, nueva);
            y = formato.getHeight() - margen;
        } else {
            cs = new PDPageContentStream(doc, ultima, PDPageContentStream.AppendMode.APPEND, true);
        }
        escribirSeccionLista(doc, cs, formato, fontBold, fontRegular, tituloSeccion, items, margen, anchoUtil, y, yMinimo);
    }

    private void escribirTexto(PDPageContentStream cs, PDType1Font font, float size, Color color, float x, float y, String texto) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(pdfSeguro(texto));
        cs.endText();
    }

    private String truncarTexto(String texto, PDType1Font font, float size, float maxWidth) throws IOException {
        String limpio = pdfSeguro(texto);
        if (font.getStringWidth(limpio) * size / 1000f <= maxWidth) {
            return limpio;
        }
        String sufijo = "...";
        int fin = limpio.length();
        while (fin > 1 && font.getStringWidth(limpio.substring(0, fin) + sufijo) * size / 1000f > maxWidth) {
            fin--;
        }
        return limpio.substring(0, Math.max(1, fin)) + sufijo;
    }

    private List<String> envolverTexto(String texto, PDType1Font font, float size, float maxWidth) throws IOException {
        List<String> lineas = new ArrayList<>();
        String restante = pdfSeguro(texto).trim();
        while (!restante.isEmpty()) {
            int fin = restante.length();
            while (fin > 1 && font.getStringWidth(restante.substring(0, fin)) * size / 1000f > maxWidth) {
                fin--;
            }
            if (fin < restante.length()) {
                int espacio = restante.lastIndexOf(' ', fin);
                if (espacio > 0) fin = espacio;
            }
            lineas.add(restante.substring(0, fin).stripTrailing());
            restante = restante.substring(fin).stripLeading();
        }
        return lineas;
    }

    private String pdfSeguro(String texto) {
        if (texto == null) return "";
        String limpio = texto
                .replace("•", "-")
                .replace("—", "-")
                .replace("–", "-")
                .replace("·", "|")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("‘", "'")
                .replace("’", "'")
                .replaceAll("[\\r\\n\\t]", " ");
        Charset winAnsi = Charset.forName("windows-1252");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limpio.length(); i++) {
            char c = limpio.charAt(i);
            String s = String.valueOf(c);
            byte[] encoded = s.getBytes(winAnsi);
            String decoded = new String(encoded, winAnsi);
            if (decoded.equals(s) && c >= 32) {
                sb.append(c);
            } else if (c == ' ') {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private String textoSeguro(String valor, String porDefecto) {
        return (valor == null || valor.isBlank()) ? porDefecto : valor.trim();
    }

    private void aplicarBordes(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private void escribirValorTipado(Cell cell, String valor) {
        String trimmed = valor.trim();
        if (trimmed.matches("^-?\\d+$")) {
            try {
                cell.setCellValue(Long.parseLong(trimmed));
                return;
            } catch (NumberFormatException ignored) {}
        }
        if (trimmed.matches("^-?\\d+\\.\\d+$")) {
            try {
                cell.setCellValue(Double.parseDouble(trimmed));
                return;
            } catch (NumberFormatException ignored) {}
        }
        cell.setCellValue(valor);
    }
}
