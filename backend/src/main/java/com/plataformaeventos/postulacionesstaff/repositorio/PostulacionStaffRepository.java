package com.plataformaeventos.postulacionesstaff.repositorio;
import com.plataformaeventos.postulacionesstaff.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PostulacionStaffRepository extends JpaRepository<PostulacionStaff,Long> { Optional<PostulacionStaff> findByIdAndOrganizacionId(Long id,Long org); Optional<PostulacionStaff> findByConvocatoriaIdAndUsuarioIdAndOrganizacionId(Long c,Long u,Long o); boolean existsByConvocatoriaIdAndUsuarioIdAndOrganizacionId(Long c,Long u,Long o); long countByConvocatoriaIdAndOrganizacionIdAndEstado(Long c,Long o,EstadoPostulacionStaff estado); List<PostulacionStaff> findByUsuarioIdAndOrganizacionIdOrderByFechaPostulacionDesc(Long u,Long o); List<PostulacionStaff> findByConvocatoriaIdAndOrganizacionIdOrderByFechaPostulacionDesc(Long c,Long o); }
