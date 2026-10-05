package com.plataformaeventos.participantes.dto;

public enum FormatoReporte {
    CSV("text/csv; charset=UTF-8", "csv"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    HTML("text/html; charset=UTF-8", "html"),
    PDF("application/pdf", "pdf");

    private final String contentType;
    private final String extension;

    FormatoReporte(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() { return contentType; }
    public String extension() { return extension; }
}
