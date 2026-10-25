ALTER TABLE postulacion_staff
    ADD CONSTRAINT uq_postulacion_id_organizacion_usuario
        UNIQUE (id_postulacion, id_organizacion, id_usuario);

ALTER TABLE asignacion_staff
    ADD CONSTRAINT fk_asignacion_postulacion_usuario_tenant
        FOREIGN KEY (id_postulacion, id_organizacion, id_usuario)
        REFERENCES postulacion_staff (id_postulacion, id_organizacion, id_usuario);
