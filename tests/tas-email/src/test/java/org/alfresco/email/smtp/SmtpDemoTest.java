package org.alfresco.email.smtp;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.mail.util.MailConnectException;

import javax.mail.MessagingException;

public class SmtpDemoTest extends SMTPTest
{
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
       testUser = dataUser.getAdminUser();
       testSite = dataSite.usingUser(testUser).createIMAPSite();
       testFolder = dataContent.usingAdmin().usingSite(testSite).createFolder();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Verify admin cannot connect to SMTP when email server is disabled")
    @Test(groups = { "demo" })
    public void adminShouldConnectToSMTP() throws Exception
    {        
        smtpProtocol.authenticateUser(dataUser.getAdminUser())
            .and().assertThat().smtpIsConnected();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Verify admin can send email and receive it")
    @Test(groups = { "demo" })
    public void adminShouldSendSMTPMails() throws Exception
    {
        /*
         * Adding one Email alias to a folder, every mails that will be sent to that alias will be saved into folder
         */
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        
        /*
         * now sending one generate mail to our folder, using the same email alias value
         */
        smtpProtocol.authenticateUser(testUser)
                    .and()
                    .composeMessage()                        
                        .withRecipients(alias + "@tas-alfresco.com")
                        .withSubject("subject")
                        .withBody("body")
                        .sendMail();
        
        imapProtocol.authenticateUser(testUser)
                .usingSite(testSite).usingResource(testFolder).assertThat().hasNewMessages();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Verify admin cannot connect to SMTP when email server is disabled")
    @Test(groups = { "demo" }, expectedExceptions = { MailConnectException.class, MessagingException.class})
    public void adminShouldNotConnectToDisabledServer() throws Exception
    {
        smtpProtocol.withJMX().disableSmtpEmailServer();
        smtpProtocol.authenticateUser(dataUser.getAdminUser());
    }
}
