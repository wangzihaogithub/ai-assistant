package com.github.aiassistant.entity.model.user;

import java.io.Serializable;

// @Data
public class AiAccessUserVO {
    private Serializable id;

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
