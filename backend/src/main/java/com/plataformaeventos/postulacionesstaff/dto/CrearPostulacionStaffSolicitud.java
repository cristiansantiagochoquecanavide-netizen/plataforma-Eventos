package com.plataformaeventos.postulacionesstaff.dto;
import jakarta.validation.constraints.Size;
public record CrearPostulacionStaffSolicitud(@Size(max=1000,message="La motivacion no puede superar 1000 caracteres") String motivacion) {}
