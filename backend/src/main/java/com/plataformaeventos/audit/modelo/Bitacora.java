package com.plataformaeventos.audit.modelo;

import com.plataformaeventos.audit.cifrado.BitacoraPayloadCifrado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bitacora")
public class Bitacora {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_organizacion", nullable = false)
    private Organizacion organizacion;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_actor") private Usuario actor;
    @Column private String accion;
    @Column private String modulo;
    @Column private String entidad;
    @Column(name = "id_entidad") private Long idEntidad;
    @Column(nullable = false) private OffsetDateTime fecha = OffsetDateTime.now();
    @Column(length = 64) private String ip;
    @Column private Boolean exito;
    @Column(length = 1000) private String resultado;

    @Column(name = "payload_ciphertext", columnDefinition = "TEXT") private String payloadCiphertext;
    @Column(name = "payload_dek_cifrada", columnDefinition = "TEXT") private String payloadDekCifrada;
    @Column(name = "payload_nonce", length = 64) private String payloadNonce;
    @Column(name = "crypto_version") private Integer cryptoVersion;
    @Column(name = "idx_actor", length = 64) private String indiceActor;
    @Column(name = "idx_ip", length = 64) private String indiceIp;
    @Column(name = "idx_accion", length = 64) private String indiceAccion;
    @Column(name = "idx_modulo", length = 64) private String indiceModulo;
    @Column(name = "idx_entidad", length = 64) private String indiceEntidad;
    @Column(name = "idx_id_entidad", length = 64) private String indiceIdEntidad;
    @Column(name = "idx_exito", length = 64) private String indiceExito;

    protected Bitacora() { }

    /** Legacy constructor for existing plaintext rows and tests only. New writes must use cifrada(...). */
    public Bitacora(Usuario actor, Organizacion organizacion, String accion, String modulo,
                    String entidad, Long idEntidad, String ip, boolean exito, String resultado) {
        this.actor = actor; this.organizacion = organizacion; this.accion = accion;
        this.modulo = modulo; this.entidad = entidad; this.idEntidad = idEntidad;
        this.ip = ip; this.exito = exito; this.resultado = resultado;
    }

    public Bitacora(Usuario actor, Organizacion organizacion, String accion, String modulo,
                    String entidad, Long idEntidad, boolean exito, String resultado) {
        this(actor, organizacion, accion, modulo, entidad, idEntidad, null, exito, resultado);
    }

    public static Bitacora cifrada(Organizacion organizacion, BitacoraPayloadCifrado payload,
                                   String indiceActor, String indiceIp, String indiceAccion, String indiceModulo,
                                   String indiceEntidad, String indiceIdEntidad, String indiceExito) {
        Bitacora bitacora = new Bitacora();
        bitacora.organizacion = organizacion;
        bitacora.actor = null;
        bitacora.accion = null;
        bitacora.modulo = null;
        bitacora.entidad = null;
        bitacora.idEntidad = null;
        bitacora.ip = null;
        bitacora.exito = null;
        bitacora.resultado = null;
        bitacora.payloadCiphertext = payload.ciphertext();
        bitacora.payloadDekCifrada = payload.dataKeyCiphertext();
        bitacora.payloadNonce = payload.nonce();
        bitacora.cryptoVersion = payload.cryptoVersion();
        bitacora.indiceActor = indiceActor;
        bitacora.indiceIp = indiceIp;
        bitacora.indiceAccion = indiceAccion;
        bitacora.indiceModulo = indiceModulo;
        bitacora.indiceEntidad = indiceEntidad;
        bitacora.indiceIdEntidad = indiceIdEntidad;
        bitacora.indiceExito = indiceExito;
        return bitacora;
    }

    @PreUpdate @PreRemove
    private void impedirMutacion() { throw new IllegalStateException("La bitacora es inmutable"); }

    public Long getId() { return id; }
    public Organizacion getOrganizacion() { return organizacion; }
    public Usuario getActor() { return actor; }
    public String getAccion() { return accion; }
    public String getModulo() { return modulo; }
    public String getEntidad() { return entidad; }
    public Long getIdEntidad() { return idEntidad; }
    public OffsetDateTime getFecha() { return fecha; }
    public String getIp() { return ip; }
    public Boolean getExito() { return exito; }
    public boolean isExito() { return Boolean.TRUE.equals(exito); }
    public String getResultado() { return resultado; }
    public String getPayloadCiphertext() { return payloadCiphertext; }
    public String getPayloadDekCifrada() { return payloadDekCifrada; }
    public String getPayloadNonce() { return payloadNonce; }
    public Integer getCryptoVersion() { return cryptoVersion; }
    public String getIndiceActor() { return indiceActor; }
    public String getIndiceIp() { return indiceIp; }
    public String getIndiceAccion() { return indiceAccion; }
    public String getIndiceModulo() { return indiceModulo; }
    public String getIndiceEntidad() { return indiceEntidad; }
    public String getIndiceIdEntidad() { return indiceIdEntidad; }
    public String getIndiceExito() { return indiceExito; }
    public boolean estaCifrada() {
        return payloadCiphertext != null && payloadNonce != null && cryptoVersion != null
                && (payloadDekCifrada != null || cryptoVersion == 2);
    }
}
