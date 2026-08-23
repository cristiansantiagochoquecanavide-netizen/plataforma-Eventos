package com.plataformaeventos.excepciones;

public class ExcepcionNoEncontrado extends RuntimeException {

    public ExcepcionNoEncontrado(String mensaje) {
        super(mensaje);
    }
}
