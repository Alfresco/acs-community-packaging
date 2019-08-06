package org.alfresco.email.smtp;

import com.sun.mail.util.MailConnectException;
import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.Test;

import javax.mail.MessagingException;

public class SmtpDisableInboundEmailTests extends SMTPTest
{
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Verify connection fails when trying to send email with inbound disabled")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY },
            expectedExceptions = { MailConnectException.class, MessagingException.class})
    public void randomUserCannotConnectWithInboundDisabled() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        smtpProtocol.withJMX().disableSmtpEmailServer();
        smtpProtocol.authenticateUser(testUser);
    }
}
