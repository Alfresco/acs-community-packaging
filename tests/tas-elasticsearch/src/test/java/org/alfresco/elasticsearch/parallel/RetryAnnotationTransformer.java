package org.alfresco.elasticsearch.parallel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * Add the {@link RetryAnalyzer} to each test.
 */
public class RetryAnnotationTransformer implements IAnnotationTransformer
{
    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod)
    {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
