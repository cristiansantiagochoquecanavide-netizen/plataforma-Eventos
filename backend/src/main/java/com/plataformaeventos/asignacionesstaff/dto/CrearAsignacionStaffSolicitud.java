package com.plataformaeventos.asignacionesstaff.dto;
import jakarta.validation.constraints.*; import java.time.OffsetDateTime;
public record CrearAsignacionStaffSolicitud(@NotNull Long idPostulacion,@NotBlank @Size(max=255) String tarea,OffsetDateTime turnoInicio,OffsetDateTime turnoFin,@Size(max=1000) String observaciones) {}
