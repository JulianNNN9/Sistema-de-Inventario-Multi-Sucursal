package com.optiplant.inventario.producto.dto;

import com.optiplant.inventario.producto.UnidadesMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/v1/products}. */
public record ProductoRequest(

        @NotBlank(message = "el sku es obligatorio")
        @Size(max = 60, message = "el sku no puede superar 60 caracteres")
        String sku,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 160, message = "el nombre no puede superar 160 caracteres")
        String nombre,

        @NotBlank(message = "la unidad de medida base es obligatoria")
        @Pattern(regexp = UnidadesMedida.REGEX, message = "la unidad de medida base no es una unidad válida")
        String unidadMedidaBase
) {
}
