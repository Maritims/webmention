package no.clueless.webmention.cli;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(CommandParameters.class)
public @interface CommandParameter {
    String longName();

    String shortName();

    String description();

    boolean required() default false;

    boolean requiresValue() default false;

    String defaultValue() default "";

    Class<?> type();
}

