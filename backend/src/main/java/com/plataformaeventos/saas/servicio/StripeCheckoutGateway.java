package com.plataformaeventos.saas.servicio;

import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import java.util.UUID;

public interface StripeCheckoutGateway {
    StripeCheckoutResultado crearSesion(UUID altaId, String correo, PlanSuscripcion plan);
}
