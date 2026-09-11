package com.optiplant.inventario.config;

import com.optiplant.inventario.security.auth.AuthController;
import com.optiplant.inventario.usuario.entity.Rol;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Traduce la anotación {@code @PreAuthorize} real de cada endpoint (método o,
 * si no la tiene, clase) a una descripción en español dentro del spec de
 * OpenAPI, para que la documentación de roles nunca pueda quedar desactualizada
 * respecto de la autorización efectiva (Módulo 9 · Fase A, RNF-04/Sección 4.2).
 */
@Component
public class RoleDescriptionOperationCustomizer implements OperationCustomizer {

    private static final Pattern ROL_PATTERN = Pattern.compile("'([A-Z_]+)'");

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        String acceso;
        if (handlerMethod.getBeanType() == AuthController.class) {
            // Único endpoint público de la API (SecurityConfig.PUBLIC_ENDPOINTS):
            // no exige el token que el resto de operaciones sí requiere.
            operation.setSecurity(List.of(new SecurityRequirement()));
            acceso = "Público: no requiere token.";
        } else {
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (preAuthorize == null) {
                preAuthorize = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);
            }
            acceso = preAuthorize != null
                    ? describirRoles(preAuthorize.value())
                    : "Cualquier usuario autenticado.";
        }

        String existente = operation.getDescription();
        operation.setDescription(
                (existente != null && !existente.isBlank() ? existente + "\n\n" : "") + "**Acceso:** " + acceso);
        return operation;
    }

    private String describirRoles(String expresionSpel) {
        Matcher matcher = ROL_PATTERN.matcher(expresionSpel);
        Set<String> roles = new LinkedHashSet<>();
        while (matcher.find()) {
            roles.add(matcher.group(1));
        }
        if (roles.isEmpty()) {
            return "Requiere autenticación.";
        }
        String etiquetas = roles.stream()
                .map(nombre -> Rol.valueOf(nombre).etiqueta())
                .collect(Collectors.joining(" o "));
        return "Solo " + etiquetas + ".";
    }
}
