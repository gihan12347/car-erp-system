package com.carsale.erp.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * A pooled MySQL socket can already be closed when a request starts a transaction.
 * Hikari discards that socket. One retry opens a new connection.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DeadConnectionRetryAspect {

    private static final Logger log = LoggerFactory.getLogger(DeadConnectionRetryAspect.class);

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object retryIfConnectionClosed(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (CannotCreateTransactionException ex) {
            log.warn("Database connection was closed before {} started. Retrying once.",
                    joinPoint.getSignature().toShortString());
            return joinPoint.proceed();
        }
    }
}
