package org.alfresco.email.smtp;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sun.mail.util.MailConnectException;

import javax.mail.MessagingException;

public class SmtpConnectionTests extends SMTPTest
{
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Verify user cannot connect to SMTP when email server is disabled")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY },
            expectedExceptions = { MailConnectException.class, MessagingException.class })
    public void adminShouldNotConnectToDisabledServer() throws Exception
    {
        smtpProtocol.withJMX().disableSmtpEmailServer();
        smtpProtocol.authenticateUser(dataUser.getAdminUser());
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Verify user can connect to SMTP when email server is enabled")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY })
    public void adminShouldConnectToSMTP() throws Exception
    {
        smtpProtocol.authenticateUser(dataUser.getAdminUser()).and().assertThat().smtpIsConnected();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Port is successfully changed to another value than default")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY })
    public void updateSmtpServerPort() throws Exception
    {
        smtpProtocol.withJMX().updateSmtpServerPort(1125);
        Assert.assertEquals(smtpProtocol.withJMX().getSmtpServerPort(), 1125);
        smtpProtocol.authenticateUser(dataUser.getAdminUser(), 1125).and().assertThat().smtpIsConnected();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify user cannot connect to SMTP when maximum server connections is set to a negative value")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL },
            expectedExceptions = { MailConnectException.class, MessagingException.class },
            expectedExceptionsMessageRegExp = ".*response: 421")
    public void verifyUserCannotConnectToSMTPWhenMaximumServerConnectionsHasANegativeValue() throws Exception
    {
        smtpProtocol.withJMX().updateSmtpMaximumServerConnections(-1);
        Assert.assertEquals(smtpProtocol.withJMX().getSmtpMaximumServerConnections(), -1);
        smtpProtocol.authenticateUser(dataUser.getAdminUser());
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify user cannot connect to SMTP when maximum server connections is set to 0")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL },
            expectedExceptions = { MailConnectException.class, MessagingException.class },
            expectedExceptionsMessageRegExp = ".*response: 421")
    public void verifyUserCannotConnectToSMTPWhenMaximumServerConnectionsIs0() throws Exception
    {
        smtpProtocol.withJMX().updateSmtpMaximumServerConnections(0);
        Assert.assertEquals(smtpProtocol.withJMX().getSmtpMaximumServerConnections(), 0);
        smtpProtocol.authenticateUser(dataUser.getAdminUser());
    }
}
