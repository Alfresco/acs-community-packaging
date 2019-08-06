package org.alfresco.email.smtp;

import com.sun.mail.smtp.SMTPSendFailedException;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.Test;

public class SmtpEmailAuthenticationGroupTests extends SMTPTest
{
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify that email is not sent from a user that is not in the Email Authentication Group")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions = SMTPSendFailedException.class,
            expectedExceptionsMessageRegExp = "554 The user .* in not in the email contributor group.*")
    public void verifyEmailIsNotSentFromUserThatIsNotInEmailAuthenticationGroup() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpUnknownUser(testUser.getUsername());
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients("admin@alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify that email is not sent when unknown user is set to an inexistent user")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL }, expectedExceptions = SMTPSendFailedException.class,
            expectedExceptionsMessageRegExp = "554 The user .* in not in the email contributor group.*")
    public void verifyEmailIsNotSentWhenUnknownUserIsSetToAnInexistentUser() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpUnknownUser("inexistentUser123@test.com");
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients("admin@alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify that email from admin is not sent for an inexistent Email Authentication Group")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions = SMTPSendFailedException.class,
            expectedExceptionsMessageRegExp = "554 The email address .* does not reference a valid accessible node.*")
    public void verifyEmailIsNotSentForInexistentEmailAuthenticationGroup() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        smtpProtocol.withJMX().enableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpEmailAuthenticationGroup(RandomData.getRandomName("Group"));
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients("admin@alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
}