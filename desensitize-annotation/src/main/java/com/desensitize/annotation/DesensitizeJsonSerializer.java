package com.desensitize.annotation;

import com.desensitize.core.engine.DesensitizeUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class DesensitizeJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private String typeId;

    public DesensitizeJsonSerializer() {
    }

    public DesensitizeJsonSerializer(String typeId) {
        this.typeId = typeId;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (typeId == null || typeId.isBlank()) {
            gen.writeString(value);
            return;
        }

        try {
            String masked = DesensitizeUtil.mask(value, typeId);
            gen.writeString(masked);
        } catch (Exception e) {
            log.warn("注解脱敏失败 typeId={}, value={}: {}", typeId, value, e.getMessage());
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return this;
        }

        Desensitize annotation = property.getAnnotation(Desensitize.class);
        if (annotation == null) {
            annotation = property.getContextAnnotation(Desensitize.class);
        }

        if (annotation != null) {
            return new DesensitizeJsonSerializer(annotation.type());
        }

        return this;
    }
}
