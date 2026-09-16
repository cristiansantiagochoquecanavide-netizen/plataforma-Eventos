package com.plataformaeventos.organizaciones.repositorio;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> { Optional<Organizacion> findBySlug(String slug); }
