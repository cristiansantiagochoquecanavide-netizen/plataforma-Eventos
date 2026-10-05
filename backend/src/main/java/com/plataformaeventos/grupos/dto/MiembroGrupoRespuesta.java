package com.plataformaeventos.grupos.dto;

import com.plataformaeventos.usuarios.modelo.Usuario;

public record MiembroGrupoRespuesta(Long idUsuario, String nombres, String apellidos, String correo) {
    public static MiembroGrupoRespuesta desde(Usuario usuario) {
        return new MiembroGrupoRespuesta(usuario.getId(), usuario.getNombres(), usuario.getApellidos(), usuario.getCorreo());
    }
}
