package org.alfresco.elasticsearch.parallel;

import java.util.ConcurrentModificationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Sometimes REST tests fail when run in parallel with:
 * 
 * <pre>
 *     java.lang.IllegalStateException: Invalid use of BasicClientConnManager: connection still allocated.
 *     Make sure to release the connection before allocating another one.
 * </pre>
 * 
 * Other times restassured fails with <code>java.util.ConcurrentModificationException</code>. If we detect a test failed due to these timing issues then we rerun it up to three times.
 */
public class RetryAnalyzer implements IRetryAnalyzer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAnalyzer.class);
    private static final int RETRY_LIMIT = 1;
    private int retryNumber = 0;

    @Override
    public boolean retry(ITestResult testResult)
    {
        retryNumber++;
        Throwable throwable = testResult.getThrowable();
        if (retryNumber == RETRY_LIMIT)
        {
            LOGGER.info("Retry limit reached: {}, shouldRetry: {}", retryNumber, false, throwable);
            return false;
        }
        else
        {
            boolean shouldRetry = throwable != null
                    && (throwable instanceof IllegalStateException
                            && throwable.getMessage().contains("connection still allocated"))
                    || (throwable instanceof ConcurrentModificationException)
                    || (throwable instanceof AssertionError
                            && throwable.getMessage().contains("Maximum retry period reached"));
            LOGGER.info("Retry: {}, shouldRetry: {}", retryNumber, shouldRetry, throwable);
            return shouldRetry;
        }
    }
}
