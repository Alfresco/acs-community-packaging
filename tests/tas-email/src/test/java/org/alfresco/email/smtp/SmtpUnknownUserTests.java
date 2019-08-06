package org.alfresco.email.smtp;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SmtpUnknownUserTests extends SMTPTest
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        testUser = dataUser.getAdminUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Set Unknown User to 'anonymous' when From user is an existing user email. Email is sent successfully")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY })
    public void emailSentSuccessfullyWithAnExistingUser() throws Exception
    {
        smtpProtocol.withJMX().updateSmtpUnknownUser("anonymous");
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
}
