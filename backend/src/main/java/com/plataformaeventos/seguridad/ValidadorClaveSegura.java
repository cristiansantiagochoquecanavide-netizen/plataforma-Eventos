package com.plataformaeventos.seguridad;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidadorClaveSegura implements ConstraintValidator<ClaveSegura, String> {

    private static final int MINIMO_CARACTERES = 8;
    private static final int MINIMO_NUMEROS = 2;

    @Override
    public boolean isValid(String clave, ConstraintValidatorContext contexto) {
        if (clave == null || clave.isBlank()) {
            return true;
        }

        int numeros = 0;
        boolean tieneMayuscula = false;
        boolean tieneMinuscula = false;

        for (int indice = 0; indice < clave.length(); indice++) {
            char caracter = clave.charAt(indice);
            if (caracter >= 'A' && caracter <= 'Z') {
                tieneMayuscula = true;
            }
            if (caracter >= 'a' && caracter <= 'z') {
                tieneMinuscula = true;
            }
            if (caracter >= '0' && caracter <= '9') {
                numeros++;
            }
        }

        return clave.length() >= MINIMO_CARACTERES
                && tieneMayuscula
                && tieneMinuscula
                && numeros >= MINIMO_NUMEROS;
    }
}
