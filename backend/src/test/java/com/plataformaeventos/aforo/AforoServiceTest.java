package com.plataformaeventos.aforo;

import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AforoServiceTest {
    EmbeddedDatabase db; AforoService servicio;
    final AforoFiltro todos = new AforoFiltro(null,null,null,null,null);
    @BeforeEach void preparar() {
        db=new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        var jdbc=new NamedParameterJdbcTemplate(db); var q=jdbc.getJdbcTemplate();
        q.execute("CREATE TABLE espacio(id_espacio BIGINT, id_organizacion BIGINT, nombre VARCHAR, capacidad INT)");
        q.execute("CREATE TABLE evento(id_evento BIGINT,id_organizacion BIGINT,id_espacio BIGINT,nombre VARCHAR,aforo_maximo INT,fecha_inicio TIMESTAMP WITH TIME ZONE)");
        q.execute("CREATE TABLE usuario(id_usuario BIGINT,nombres VARCHAR,apellidos VARCHAR,correo VARCHAR)");
        q.execute("CREATE TABLE inscripcion(id_inscripcion BIGINT,id_evento BIGINT,id_organizacion BIGINT,id_participante BIGINT,estado VARCHAR,categoria_entrada VARCHAR)");
        q.execute("CREATE TABLE asistencia(id_asistencia BIGINT,id_inscripcion BIGINT,id_organizacion BIGINT,id_usuario_staff BIGINT,fecha_hora TIMESTAMP WITH TIME ZONE)");
        q.execute("INSERT INTO espacio VALUES(10,1,'Sala A',2),(20,2,'Sala B',100)");
        q.execute("INSERT INTO evento VALUES(1,1,10,'Congreso',3,CURRENT_TIMESTAMP),(2,2,20,'Privado',100,CURRENT_TIMESTAMP),(3,1,NULL,'Sin sala',NULL,CURRENT_TIMESTAMP)");
        q.execute("INSERT INTO usuario VALUES(1,'Ana','Pérez','ana@test'),(2,'Luis','López','luis@test'),(3,'Eva','Ruiz','eva@test'),(4,'Operador','Uno','staff@test')");
        q.execute("INSERT INTO inscripcion VALUES(1,1,1,1,'CONFIRMADA','VIP'),(2,1,1,2,'CONFIRMADA',NULL),(3,1,1,3,'CANCELADA','VIP'),(4,2,2,1,'CONFIRMADA','VIP'),(5,1,1,1,'EN_ESPERA',NULL)");
        q.execute("INSERT INTO asistencia VALUES(1,1,1,4,TIMESTAMP WITH TIME ZONE '2026-10-26 10:00:00-04:00'),(2,3,1,4,TIMESTAMP WITH TIME ZONE '2026-10-26 11:00:00-04:00'),(3,4,2,4,CURRENT_TIMESTAMP)");
        servicio=new AforoService(jdbc,mock(InscripcionRepository.class));
    }
    @AfterEach void cerrar(){db.shutdown();}
    @Test void cuentaConfirmadasYAsistenciasSinFiltrarOtroTenant() {
        var r=servicio.resumen(1,todos).getFirst();
        assertThat(r.inscritos()).isEqualTo(3); assertThat(r.presentes()).isEqualTo(2);assertThat(r.ausentes()).isEqualTo(1);
        assertThat(r.capacidad()).isEqualTo(2);assertThat(r.alerta()).isEqualTo("COMPLETO");
        assertThat(servicio.nomina(1,todos,0,25).total()).isEqualTo(3);
        assertThat(servicio.resumen(1,todos)).extracting(AforoService.Resumen::evento).doesNotContain("Privado");
        assertThatThrownBy(()->servicio.resumen(1,new AforoFiltro(2L,null,null,null,null))).hasMessage("Evento no encontrado");
        assertThatThrownBy(()->servicio.nomina(1,new AforoFiltro(null,20L,null,null,null),0,25)).hasMessage("Sala no encontrada");
    }
    @Test void franjaSemiabiertaYFiltrosNoReducenElAforo() {
        var f=new AforoFiltro(1L,10L,"VIP",OffsetDateTime.parse("2026-10-26T10:00:00-04:00"),OffsetDateTime.parse("2026-10-26T11:00:00-04:00"));
        var r=servicio.resumen(1,f).getFirst();
        assertThat(r.inscritos()).isEqualTo(2);assertThat(r.presentes()).isEqualTo(2);assertThat(r.ingresosFranja()).isEqualTo(1);
        assertThat(r.ingresosSala()).isEqualTo(2);assertThat(r.porcentajeAforo()).isEqualTo(100);
        var n=servicio.nomina(1,f,0,25);assertThat(n.total()).isEqualTo(1);assertThat(n.filas().getFirst().nombre()).isEqualTo("Ana Pérez");
    }
    @Test void sinClasificarAusentesYEventosVacios() {
        var r=servicio.resumen(1,new AforoFiltro(1L,null,"",null,null)).getFirst();
        assertThat(r.inscritos()).isEqualTo(1); assertThat(r.presentes()).isZero();assertThat(r.ingresosSala()).isEqualTo(2);
        var vacio=servicio.resumen(1,new AforoFiltro(3L,null,null,null,null)).getFirst();
        assertThat(vacio.inscritos()).isZero();assertThat(vacio.porcentajeAsistencia()).isZero();assertThat(vacio.porcentajeAforo()).isNull();
        assertThat(vacio.alerta()).isEqualTo("SIN_CAPACIDAD");
    }
    @Test void paginacionValidacionesYUmbrales() {
        assertThat(servicio.nomina(1,todos,0,1).filas()).hasSize(1);
        assertThat(servicio.nomina(1,todos,1,1).filas().getFirst().inscripcionId()).isNotEqualTo(servicio.nomina(1,todos,0,1).filas().getFirst().inscripcionId());
        assertThatThrownBy(()->servicio.nomina(1,todos,-1,25)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new AforoFiltro(null,null,null,OffsetDateTime.now(),OffsetDateTime.now().minusHours(1))).isInstanceOf(IllegalArgumentException.class);
        double[] puntos={79.99,80,80.01,89.99,90,90.01,99.99,100,100.01};
        String[] niveles={"NORMAL","PRECAUCION","PRECAUCION","PRECAUCION","CRITICO","CRITICO","CRITICO","COMPLETO","COMPLETO"};
        for(int i=0;i<puntos.length;i++)assertThat(AforoService.alerta(puntos[i])).isEqualTo(niveles[i]);
        assertThat(AforoService.porcentaje(12,10)).isEqualTo(120);
    }
}
