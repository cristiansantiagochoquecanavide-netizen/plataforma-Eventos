package com.plataformaeventos.asistencias.servicio;

public class AsistenciaDuplicadaException extends RuntimeException {
    public AsistenciaDuplicadaException() { super("La asistencia de esta inscripción ya fue registrada"); }
}
