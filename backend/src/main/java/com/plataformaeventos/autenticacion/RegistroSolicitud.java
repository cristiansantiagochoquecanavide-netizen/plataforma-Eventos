package com.plataformaeventos.autenticacion;

import com.plataformaeventos.seguridad.ClaveSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroSolicitud(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no deben superar 100 caracteres")
        String nombres,
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no deben superar 100 caracteres")
        String apellidos,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 254, message = "El correo no debe superar 254 caracteres")
        String correo,
        @NotBlank(message = "La clave es obligatoria")
        @Size(max = 72, message = "La clave no debe superar 72 caracteres")
        @ClaveSegura
        String clave
) {
}
