package org.alfresco.email.smtp;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.GroupModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.Test;

import com.sun.mail.smtp.SMTPSendFailedException;

public class SmtpBlockedSendersTests extends SMTPTest
{
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Set Blocked Senders: to a valid user (valid user email in Alfresco). Send email fails for the blocked user")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY }, expectedExceptions=SMTPSendFailedException.class)
    public void sendMailIsDeniedForBlockedUser() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        testUser.setDomain("tas-automation.org");
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpBlockedSenders(testUser.getEmailAddress());
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients("admin@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Set Blocked Senders to .* Try to send emails with any users. Access is denied for everyone")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY }, expectedExceptions=SMTPSendFailedException.class)
    public void sendMailIsDeniedForAllUsers() throws Exception
    {
        testUser = dataContent.getAdminUser();
        smtpProtocol.withJMX().updateSmtpBlockedSenders(".*");
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients("admin@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Set Blocked Senders to valid domain .*@alfresco.com." +
                    "Try to send email with users in @alfresco.com domain. Access is denied for each one")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY }, expectedExceptions=SMTPSendFailedException.class)
    public void sendMailIsDeniedForUsersBelongingToDomain() throws Exception
    {
        testUser = dataUser.getAdminUser();
        testUser.setDomain("tas-automation.org");
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpBlockedSenders(".*@alfresco.com");
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients("admin@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Set Email Authentication Group to EMAIL_CONTRIBUTORS with From user added to this group." +
                    "Email is sent successfully with that user")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY })
    public void emailSentSuccessfullyWithUserAddedToEmailContributorsGroup() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        dataGroup.usingUser(testUser).addUserToGroup(GroupModel.getEmailContributorsGroup());
        smtpProtocol.withJMX().enableSmtpAuthentication();
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set allowed and blocked senders to a valid domain and check that emil cannot be sent")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL }, expectedExceptions=SMTPSendFailedException.class,
            expectedExceptionsMessageRegExp = "554 .* has been denied access.\n")
    public void allowedAndBlockedSendersTheSameEmailNotSentTest() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        testUser.setDomain("tas-automation.org");
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpBlockedSenders(testUser.getEmailAddress());
        smtpProtocol.withJMX().updateSmtpAllowedSenders(testUser.getEmailAddress());
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients("admin@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
}