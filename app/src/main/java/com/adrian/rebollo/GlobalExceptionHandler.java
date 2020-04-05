package com.adrian.rebollo;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Global Exception handler for uncaught exceptions inside the service.
 */
@Slf4j
@Aspect
@Configuration
public class GlobalExceptionHandler {

    @AfterThrowing(pointcut = "execution(* com.adrian.rebollo.*.*.*(..)) || execution(* com.adrian.rebollo.*.*(..))", throwing = "ex")
    public void log(Throwable ex) {
        LOG.error("Caught Global Exception error caught at GlobalExceptionHandler.", ex);
    }
}