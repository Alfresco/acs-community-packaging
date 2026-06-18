/*
 * #%L
 * Alfresco Tas Elasticsearch
 * %%
 * Copyright (C) 2026 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.elasticsearch.retry;

import java.util.ConcurrentModificationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAnalyzer.class);
    private static final int RETRY_LIMIT = 3;
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
