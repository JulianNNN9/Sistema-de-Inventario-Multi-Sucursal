package com.optiplant.inventario.usuario.entity;

import com.optiplant.inventario.sucursal.entity.Sucursal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario del sistema. {@code sucursal} es {@code null} únicamente para el rol
 * {@link Rol#ADMIN_GENERAL} (ver Sección 3 del roadmap).
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    /**
     * Contador de versión de token (mitigación de robo/fuga de JWT): el JWT
     * lleva este valor como claim; si no coincide con el valor actual aquí,
     * {@code JwtAuthenticationFilter} rechaza la petición aunque el token no
     * haya expirado. Se incrementa en {@code POST /api/v1/auth/logout}.
     */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;
}
