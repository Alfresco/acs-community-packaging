package org.alfresco.email.smtp;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class SmtpAndImapSettings extends SMTPTest
{
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Disable IMAP Server and enable IMAP protocol. Email is successfully send via SMTP")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE })
    public void sendMailSuccessfullyWhenIMAPServerDisabledAndIMAPProtocolEnabled() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        imapProtocol.withJMX().disableImapServer();
        imapProtocol.withJMX().enableImapProtocol();
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Disable IMAP Server and enable IMAP protocol. Email is successfully send via SMTP")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE })
    public void sendMailSuccessfullyWhenIMAPServerEnabledAndIMAPProtocolDisabled() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        imapProtocol.withJMX().enableImapServer();
        imapProtocol.withJMX().disableImapProtocol();
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
}
