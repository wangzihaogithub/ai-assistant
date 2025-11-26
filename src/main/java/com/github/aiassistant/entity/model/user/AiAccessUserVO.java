package com.github.aiassistant.entity.model.user;

import com.github.aiassistant.enums.RequestAttrKeyEnum;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// @Data
public class AiAccessUserVO implements Serializable {
    private final Map<RequestAttrKeyEnum, Object> requestAttributes = new ConcurrentHashMap<>();
    private Serializable id;

    public Map<RequestAttrKeyEnum, Object> getRequestAttributes() {
        return requestAttributes;
    }

    public <T> T getRequestAttribute(RequestAttrKeyEnum<T> key) {
        Object o = requestAttributes.get(key);
        return key.cast(o);
    }

    public <T> T setRequestAttribute(RequestAttrKeyEnum<T> key, T value) {
        return key.cast(requestAttributes.put(key, value));
    }

    public Serializable getId() {
        return id;
    }

    public void setId(Serializable id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "AiAccessUserVO{" +
                "id=" + id +
                '}';
    }
}
