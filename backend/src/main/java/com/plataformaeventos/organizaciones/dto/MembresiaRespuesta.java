package com.plataformaeventos.organizaciones.dto;

import java.util.List;

public record MembresiaRespuesta(Long idUsuario, String nombres, String apellidos, String correo, boolean activo, boolean cuentaActiva, List<String> roles) { }
