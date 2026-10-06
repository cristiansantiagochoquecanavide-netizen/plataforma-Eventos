package com.plataformaeventos.saas.repositorio;

import com.plataformaeventos.saas.modelo.StripeWebhookEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookEventoRepository extends JpaRepository<StripeWebhookEvento, String> { }
