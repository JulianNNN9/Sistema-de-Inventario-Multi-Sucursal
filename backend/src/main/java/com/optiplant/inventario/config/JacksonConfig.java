package com.optiplant.inventario.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Serializa todo {@link Instant} en la API con el formato ISO-8601 UTC exacto
 * exigido por la regla global 7: {@code yyyy-MM-dd'T'HH:mm:ss'Z'} (sin milisegundos).
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    @Bean
    public Module isoUtcInstantModule() {
        SimpleModule module = new SimpleModule("isoUtcInstantModule");
        module.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeString(ISO_UTC.format(value));
            }
        });
        return module;
    }
}
