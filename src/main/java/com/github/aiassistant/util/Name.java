package com.github.aiassistant.util;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 在Spring 6中，如果你在使用 @RequestParam 或 @PathVariable 注解时没有指定 name 属性，可能会导致内部服务器错误。
 * 这是因为从Java 8开始，Java编译器默认不再保留参数名称，而Spring框架在处理参数注解时需要这些参数名称来匹配URL路径中的变量。
 * 当你不指定name属性时，Spring框架会尝试从方法参数中获取参数名称。如果这些名称由于编译器设置而没有被保留，Spring框架就无法正确地将URL路径中的变量映射到方法参数上，从而导致这样的错误：
 * Request processing failed:
 * java.lang.IllegalArgumentException: Name for argument of type [java.lang.Integer] not specified,
 * and parameter name information not available via reflection.
 * Ensure that the compiler uses the '-parameters' flag.
 * 为了解决这个问题，你可以采取以下措施：
 * 在注解中指定 name属性：明确指定参数名称，这样即使编译器没有保留参数名称信息，Spring也能够正确地识别和映射参数。例如：
 * 这样，即使参数名称信息没有被保留，Spring也能够通过name属性正确地将URL路径中的userId变量映射到方法参数上。
 * 2. 启用编译时参数名称保留：在编译Java代码时，可以通过设置-parameters选项来保留参数名称信息。这样，即使没有在@PathVariable注解中指定name属性，Spring也能够通过反射获取参数名称。例如，在Maven的 pom.xml 文件中，可以这样配置：
 */
@Retention(RUNTIME)
@Target({PARAMETER})
public @interface Name {

    /**
     * 参数名称（这是因为从Java 8开始，Java编译器默认不再保留参数名称）
     *
     * @return 参数名称
     */
    String value();

}
