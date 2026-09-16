package com.plataformaeventos.postulacionesstaff.dto;
import com.plataformaeventos.postulacionesstaff.modelo.*;
import java.time.OffsetDateTime;
public record PostulacionStaffRespuesta(Long id,Long idConvocatoria,Long idUsuario,EstadoPostulacionStaff estado,String motivacion,OffsetDateTime fechaPostulacion) { public static PostulacionStaffRespuesta desde(PostulacionStaff p){return new PostulacionStaffRespuesta(p.getId(),p.getConvocatoria().getId(),p.getUsuario().getId(),p.getEstado(),p.getMotivacion(),p.getFechaPostulacion());} }
