package com.github.aiassistant;

public interface DAOProvider {
    <T> T getMapper(Class<T> mapperClass);
}
