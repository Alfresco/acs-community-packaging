package org.alfresco.email.smtp;

import com.sun.mail.smtp.SMTPSendFailedException;
import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.GroupModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.Test;

public class SmtpFolderAliasTests extends SMTPTest
{
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Send email from user that has an alias folder. Email is sent and received")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY })
    public void randomUserWithAliasFolderReceivesEmail() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingAdmin().usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        dataGroup.usingUser(testUser).addUserToGroup(GroupModel.getEmailContributorsGroup());
        smtpProtocol.withJMX().updateSmtpUnknownUser(testUser.getUsername());
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

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify that email is not sent from a user that does not email alias")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions = SMTPSendFailedException.class,
            expectedExceptionsMessageRegExp = "554 The email address .* does not reference a valid accessible node.*")
    public void verifyEmailIsNotSentFromUserThatDoesNotHaveEmailAlias() throws Exception
    {
        testSite = dataSite.usingAdmin().createIMAPSite();
        testFolder = dataContent.usingAdmin().usingSite(testSite).createFolder();
        String recipient = dataContent.usingAdmin().usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());

        testUser = dataUser.createRandomTestUser();
        dataGroup.usingUser(testUser).addUserToGroup(GroupModel.getEmailContributorsGroup());
        smtpProtocol.withJMX().updateSmtpUnknownUser(testUser.getUsername());
        smtpProtocol.authenticateUser(testUser)
                .and()
                .composeMessage()
                .withRecipients(recipient)
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
}
