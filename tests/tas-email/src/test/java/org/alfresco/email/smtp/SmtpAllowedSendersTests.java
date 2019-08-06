package org.alfresco.email.smtp;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.Test;

import com.sun.mail.smtp.SMTPSendFailedException;

public class SmtpAllowedSendersTests extends SMTPTest
{
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Set Allowed Senders: to a valid user (valid user email in Alfresco). Email is successfully send with this user")
    @Test(groups = { TestGroup.SANITY, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.PROTOCOLS })
    public void sendMailIsSuccessfulForAllowedUser() throws Exception
    {
        testUser = dataUser.getAdminUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        testUser.setDomain("alfresco.com");
        smtpProtocol.withJMX().updateSmtpAllowedSenders(testUser.getEmailAddress());
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Set Allowed Senders to .* Email is successfully send with any user")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY })
    public void sendingMailIsSuccessfulForAllUsers() throws Exception
    {
        testUser = dataUser.getAdminUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        testUser.setDomain("alfresco.com");
        smtpProtocol.withJMX().updateSmtpAllowedSenders(".*");
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients( alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Set Allowed Senders to valid domain .*@alfresco.com. Email is successfully send by any user in @alfresco.com domain")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY })
    public void sendingMailIsSuccessfulForUsersBelongingToDomain() throws Exception
    {
        testUser = dataContent.getAdminUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        testUser.setDomain("alfresco.com");
        smtpProtocol.withJMX().updateSmtpAllowedSenders(".*@alfresco.com");
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that when allowed senders is set to invalid domain the email fails with denied access exception")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL }, expectedExceptions = SMTPSendFailedException.class, 
        expectedExceptionsMessageRegExp = "554 .* has been denied access.*")
    public void verifyEmailIsNotSentWhenAllowedSendersIsSetToInvalidDomain() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpAllowedSenders(".*fake.com");
        smtpProtocol.authenticateUser(testUser).and().composeMessage()
            .withRecipients("admin@alfresco.com")
            .withSubject("subject")
            .withBody("body")
            .sendMail();
    }
}