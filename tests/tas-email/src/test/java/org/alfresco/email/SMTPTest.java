package org.alfresco.email;

import org.alfresco.email.dsl.ServerConfiguration;
import org.alfresco.utility.LogFactory;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;

public abstract class SMTPTest extends EmailTest
{
    static Logger LOG = LogFactory.getLogger();

    @AfterMethod(alwaysRun = true)
    public void resetServerConfiguration() throws Exception
    {
        String jmxUseJolokiaAgent = System.getProperty("jmx.useJolokiaAgent");
        if ("true".equals(jmxUseJolokiaAgent))
        {
            ServerConfiguration.restore(smtpProtocol.withJMX());
        }
        else
        {
            LOG.warn("*** Jolokia is not used! To use jolokia, please add next system property when running the tests: jmx.useJolokiaAgent=true ***");
        }
    }
}
