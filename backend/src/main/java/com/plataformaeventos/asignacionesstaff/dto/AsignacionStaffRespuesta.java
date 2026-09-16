package com.plataformaeventos.asignacionesstaff.dto;
import com.plataformaeventos.asignacionesstaff.modelo.*; import java.time.OffsetDateTime;
public record AsignacionStaffRespuesta(Long id,Long idPostulacion,Long idUsuario,String tarea,OffsetDateTime turnoInicio,OffsetDateTime turnoFin,EstadoAsignacionStaff estado){public static AsignacionStaffRespuesta desde(AsignacionStaff a){return new AsignacionStaffRespuesta(a.getId(),a.getPostulacion().getId(),a.getPostulacion().getUsuario().getId(),a.getTarea(),a.getTurnoInicio(),a.getTurnoFin(),a.getEstado());}}
