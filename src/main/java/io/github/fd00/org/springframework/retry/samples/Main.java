package io.github.fd00.org.springframework.retry.samples;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Main {
    static public void main(String[] args) throws Throwable {
        RetryTemplate retryTemplate = new RetryTemplate();

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(RetryableException.class, true);
        retryableExceptions.put(UnretryableException.class, false);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(500);
        exponentialBackOffPolicy.setMultiplier(2);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);

        log.info("Success");
        int resultSuccess = retryTemplate.execute(new RetryCallback<Integer, Throwable>() {

            @Override
            public Integer doWithRetry(RetryContext context) throws Throwable {
                return 42;
            }
        });
        log.info("  Result = {}", resultSuccess);

        log.info("Success After Failure");
        int resultSuccessAfterFailure = retryTemplate.execute(new RetryCallback<Integer, RetryableException>() {
            FrequencyRestriction frequencyRestriction = new FrequencyRestriction(3);

            @Override
            public Integer doWithRetry(RetryContext context) throws RetryableException {

                if (!frequencyRestriction.isLimited()) {
                    log.info("  trial = {}", frequencyRestriction.getCurrent());
                    throw new RetryableException();
                }

                return 42;
            }
        });
        log.info("  Result = {}", resultSuccessAfterFailure);

        log.info("Failure");
        try {
            retryTemplate.execute(new RetryCallback<Integer, RetryableException>() {

                private int current = 0;

                @Override
                public Integer doWithRetry(RetryContext context) throws RetryableException {
                    log.info("  Trial = {}", ++current);
                    throw new RetryableException();
                }
            });
        } catch (RetryableException e) {
            log.info("  Failure");
        }

        log.info("Failure Immediately");
        try {
            retryTemplate.execute(new RetryCallback<Integer, UnretryableException>() {

                @Override
                public Integer doWithRetry(RetryContext context) throws UnretryableException {
                    log.info("  Trial = {}", 1);
                    throw new UnretryableException();
                }
            });
        } catch (UnretryableException e) {
            log.info("  Failure");
        }

        log.info("Recovery");
        int resultRecovery = retryTemplate.execute(new RetryCallback<Integer, UnretryableException>() {

            @Override
            public Integer doWithRetry(RetryContext context) throws UnretryableException {
                log.info("  trial = {}", 1);
                throw new UnretryableException();
            }
        }, new RecoveryCallback<Integer>() {
            @Override
            public Integer recover(RetryContext context) throws Exception {
                return 42;
            }
        });
        log.info("  Result = {}", resultRecovery);
    }
}
