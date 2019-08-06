package org.alfresco.email.smtp;

import javax.mail.AuthenticationFailedException;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.jolokia.client.exception.J4pRemoteException;
import org.testng.annotations.Test;


/**
 * Created by Claudia Agache on 2/13/2017.
 */
public class SmtpEnableOutboundEmailTests extends SMTPTest
{
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify outbound email port cannot be set to '65536'(required range is between 1 and 65535)")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL },
            expectedExceptions = J4pRemoteException.class,
            expectedExceptionsMessageRegExp = ".*Failed to send email to:.*",
            enabled = false)
    public void outboundEmailPortCannotBeOutsideTheRequiredRange() throws Exception
    {
        smtpProtocol.withJMX().enableMailTestMessageSend();
        smtpProtocol.withJMX().updateMailPort("65536");
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify outbound email port cannot be set to empty port")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL },
            expectedExceptions = J4pRemoteException.class,
            expectedExceptionsMessageRegExp = ".*Failed to convert property value of type 'java.lang.String' to required type 'int' for property 'port'.*For input string: \"\".*",
            enabled = false)
    public void outboundEmailPortCannotBeEmptyString() throws Exception
    {
        smtpProtocol.withJMX().enableMailTestMessageSend();
        smtpProtocol.withJMX().updateMailPort("");
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify outbound email port cannot be set to string value")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL },
            expectedExceptions = J4pRemoteException.class,
            expectedExceptionsMessageRegExp = ".*Failed to convert property value of type 'java.lang.String' to required type 'int' for property 'port'.*For input string: \"twentyfive\".*",
            enabled = false)
    public void outboundEmailPortCannotBeString() throws Exception
    {
        smtpProtocol.withJMX().enableMailTestMessageSend();
        smtpProtocol.withJMX().updateMailPort("twentyfive");
    }
    
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION, description = "Verify outbound email name and password empty")
    @Test(groups = {TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL}, expectedExceptions = AuthenticationFailedException.class, enabled=false)
    public void outboundEmailEmptyUser() throws Exception{
        smtpProtocol.withJMX().enableMailTestMessageSend();
        smtpProtocol.withJMX().updateMailUsername("");
        smtpProtocol.withJMX().updateMailPassword("");
    }
}
