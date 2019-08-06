package org.alfresco.email.smtp;

import java.net.BindException;
import java.rmi.UnmarshalException;

import javax.mail.AuthenticationFailedException;
import javax.management.RuntimeMBeanException;

import junit.framework.Assert;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.GroupModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.jolokia.client.exception.J4pRemoteException;
import org.testng.annotations.Test;

import com.sun.mail.smtp.SMTPSendFailedException;
/**
 * 
 * @author Cristina Axinte
 *
 */
public class SmtpEnableInboundEmailTests extends SMTPTest
{    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email port cannot be set to invalid port number")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE })
    public void inboundEmailPortCannotBeInvalidPort() throws Exception
    {
        try{
            smtpProtocol.withJMX().updateSmtpServerPort(70000);
        }
        catch(J4pRemoteException e)
        {
            Assert.assertTrue(e.getMessage().contains("Property 'port' is incorrect"));
        }
        catch(UnmarshalException e)
        {
            Assert.assertTrue(e.getMessage().contains("no security manager: RMI class loader disabled"));
        }
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email port cannot be set to empty port")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions = {J4pRemoteException.class, RuntimeMBeanException.class},
                                                                            expectedExceptionsMessageRegExp = ".*Failed to convert property value of type 'java.lang.String' to required type 'int' for property 'port'.*")
    public void inboundEmailPortCannotBeEmptyString() throws Exception
    {
        smtpProtocol.withJMX().updateSmtpServerPort("");
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email port cannot be set to negative number")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE })
    public void inboundEmailPortCannotBeNegativeNumber() throws Exception
    {
        try
        {
            smtpProtocol.withJMX().updateSmtpServerPort(-2);
        }
        catch(J4pRemoteException e)
        {
            Assert.assertTrue(e.getMessage().contains("Property 'port' is incorrect"));
        }
        catch(UnmarshalException e)
        {
            Assert.assertTrue(e.getMessage().contains("no security manager: RMI class loader disabled"));
        }
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email port cannot be set to '65536'(required range is between 1 and 65535)")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions = {J4pRemoteException.class, RuntimeMBeanException.class},
            expectedExceptionsMessageRegExp = ".*Property 'port' is incorrect")
    public void inboundEmailPortCannotBeOutsideTheRequiredRange() throws Exception
    {
        smtpProtocol.withJMX().updateSmtpServerPort("65536");
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Allowed Senders: to an invalid user (invalid user email in Alfresco). Send mail fails with this invalid user")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions=AuthenticationFailedException.class,
                                                                            expectedExceptionsMessageRegExp = ".* Authentication credentials invalid.*")
    public void sendMailFailedToInvalidAllowedUser() throws Exception
    {    
        testUser = new UserModel("fakeuser", "password");
        testUser.setDomain("alfresco.com");
        
        UserModel recipientUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(recipientUser).createIMAPSite();
        testFolder = dataContent.usingUser(recipientUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.withJMX().updateSmtpAllowedSenders(testUser.getEmailAddress());
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email when Maximum Server Connections is greater than 100")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL })
    public void emailSentSuccessfullyWhenMaximumServerConnectionsIsSetWithHighValue() throws Exception
    {       
        UserModel testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.authenticateUser(testUser).withJMX().updateSmtpMaximumServerConnections(110);
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();       
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email is sent by Manager")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL })
    public void emailSentSuccessfullyByManager() throws Exception
    {       
        UserModel testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();       
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email is send by Collaborator")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL })
    public void emailSentSuccessfullyByCollaborator() throws Exception
    {       
        UserModel testUser1 = dataUser.createRandomTestUser();
        UserModel testUser2 = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        dataUser.addUserToSite(testUser2, testSite, UserRole.SiteCollaborator);
        testFolder = dataContent.usingUser(testUser2).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.authenticateUser(testUser2).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();       
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email is send by Contributor")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL })
    public void emailSentSuccessfullyByContributor() throws Exception
    {       
        UserModel testUser1 = dataUser.createRandomTestUser();
        UserModel testUser2 = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        dataUser.addUserToSite(testUser2, testSite, UserRole.SiteContributor);
        testFolder = dataContent.usingUser(testUser2).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.authenticateUser(testUser2).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();       
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email is send by Consumer")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL }, expectedExceptions = SMTPSendFailedException.class,
            expectedExceptionsMessageRegExp = ".*has been denied access.*")
    public void emailSentSuccessfullyByConsumer() throws Exception
    {       
        UserModel testUser1 = dataUser.createRandomTestUser();
        UserModel testUser2 = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        dataUser.addUserToSite(testUser2, testSite, UserRole.SiteConsumer);
        testFolder = dataContent.usingUser(testUser1).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.authenticateUser(testUser2).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();     
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Blocked Senders: to a invalid user (invalid user email in Alfresco). Send email fails with access denied")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions = SMTPSendFailedException.class,
                                                                            expectedExceptionsMessageRegExp = ".*has been denied access.*")
    public void sendMailIsDeniedForInvalidBlockedUser() throws Exception
    {
        testUser = new UserModel("fakeuser", "password");
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
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Blocked Senders: to a user with invalid domain. Send email fails with access denied")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions = SMTPSendFailedException.class,
            expectedExceptionsMessageRegExp = ".*has been denied access.*")
    public void sendMailIsDeniedForBlockedUserWithInvalidDomain() throws Exception
    {
        UserModel invalidTestUser = dataUser.createRandomTestUser();
        invalidTestUser.setDomain("invalid.com");
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpBlockedSenders(".*@invalid.com");        
        smtpProtocol.authenticateUser(invalidTestUser).and().composeMessage().withRecipients("admin@tas-alfresco.com")
            .withSubject("subject").withBody("body").sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Allowed and Blocked Senders: to a user. Send email fails with access denied")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions = SMTPSendFailedException.class,
            expectedExceptionsMessageRegExp = ".*has been denied access.*")
    public void sendMailIsDeniedForUserSetToAllowedAndBlockedSenders() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        adminUser.setDomain("alfresco.com");
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpBlockedSenders("admin@alfresco.com");
        smtpProtocol.withJMX().updateSmtpAllowedSenders("admin@alfresco.com");
        smtpProtocol.authenticateUser(adminUser).and().composeMessage().withRecipients("admin@alfresco.com")
            .withSubject("subject").withBody("body").sendMail();
    }
     
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Allowed and Blocked Senders to .* Try to send emails with any users. Access is denied for everyone")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions=SMTPSendFailedException.class,
                                                                            expectedExceptionsMessageRegExp = ".*has been denied access.*")
    public void sendMailIsDeniedForAllUsersSetToAllowedAndBlockedSenders() throws Exception
    {
        testUser = dataContent.getAdminUser();
        smtpProtocol.withJMX().updateSmtpBlockedSenders(".*");
        smtpProtocol.withJMX().updateSmtpAllowedSenders(".*");
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients("admin@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Allowed Senders  and Blocked Senders to .* Try to send emails with any users. Access is denied for everyone")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE })
    public void emailSentSuccessfullyForAllowedSendersAndNotForBlockedSenders() throws Exception
    {
        UserModel allowedUser = dataUser.createRandomTestUser();
        UserModel bothUser = dataUser.createRandomTestUser();
        UserModel blockedUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(allowedUser).createIMAPSite();
        testFolder = dataContent.usingUser(allowedUser).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        allowedUser.setDomain("tas-alfresco.com");
        bothUser.setDomain("tas-alfresco.com");
        blockedUser.setDomain("tas-alfresco.com");
        smtpProtocol.withJMX().updateSmtpAllowedSenders(allowedUser.getEmailAddress(), bothUser.getEmailAddress());
        smtpProtocol.withJMX().updateSmtpBlockedSenders(bothUser.getEmailAddress(), blockedUser.getEmailAddress());
        smtpProtocol.withJMX().updateSmtpUnknownUser(dataUser.getAdminUser().getUsername());
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.authenticateUser(allowedUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
        try
        {
            smtpProtocol.authenticateUser(bothUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
        }
        catch (SMTPSendFailedException sendFailedEx)
        {
            Assert.assertTrue(sendFailedEx.getMessage().contains(String.format("'%s' has been denied access.", bothUser.getEmailAddress())));
        }
        
        try
        {
            smtpProtocol.authenticateUser(blockedUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
        }
        catch (SMTPSendFailedException sendFailedEx)
        {
            Assert.assertTrue(sendFailedEx.getMessage().contains(String.format("'%s' has been denied access.", blockedUser.getEmailAddress())));
        }
   
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Email Authentication Group to new group with From user added to this group." +
                    "Email is sent successfully with that user")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE })
    public void emailSentSuccessfullyWithUserAddedToNewGroupSetForAuthGroup() throws Exception
    {
        UserModel testUser1 = dataUser.createRandomTestUser();
        UserModel testUser2 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, testUser1, testUser2);
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        testFolder = dataContent.usingUser(testUser1).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.withJMX().updateSmtpEmailAuthenticationGroup(group.getDisplayName());
        smtpProtocol.authenticateUser(testUser1).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }

    @Bug(id = "ACE-5712")
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Email Authentication Group to EMAIL_CONTRIBUTORS and set SMTP Authentication Enabled to true and add a user to a other group." +
                    "Email is not sent successfully with that user")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions=SMTPSendFailedException.class)
    public void emailIsNotSentSuccessfullyToUserAddedToNewGroupNotSetForAuthGroup1() throws Exception
    {
        UserModel testUser1 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, testUser1);
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        testFolder = dataContent.usingUser(testUser1).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.withJMX().updateSmtpEmailAuthenticationGroup(GroupModel.getEmailContributorsGroup().getDisplayName());
        smtpProtocol.authenticateUser(testUser1).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @Bug(id = "ACE-5712")
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Email Authentication Group to EMAIL_CONTRIBUTORS and set SMTP Authentication Enabled to false and add a user to a other group." +
                    "Email is not sent successfully with that user")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE })
    public void emailIsSentSuccessfullyToUserAddedToNewGroupNotSetForAuthGroup2() throws Exception
    {
        UserModel testUser1 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, testUser1);
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        testFolder = dataContent.usingUser(testUser1).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpEmailAuthenticationGroup(GroupModel.getEmailContributorsGroup().getDisplayName());
        smtpProtocol.authenticateUser(testUser1).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Transport Layer Security (TLS) to Required. Email is not sent.")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE }, expectedExceptions=SMTPSendFailedException.class,
                                                                            expectedExceptionsMessageRegExp = "530 Must issue a STARTTLS command first.*")
    public void emailIsNotSentWhenTLSSupportIsRequired() throws Exception
    {
        UserModel testUser1 = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        testFolder = dataContent.usingUser(testUser1).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().enableSmtpRequireTls();
        smtpProtocol.authenticateUser(testUser1).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Transport Layer Security (TLS) to Disabled. Email is sent.")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL })
    public void emailIsSentWhenTLSSupportIsSetToDisabled() throws Exception
    {
        UserModel testUser1 = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        testFolder = dataContent.usingUser(testUser1).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.withJMX().disableSmtpTls();
        smtpProtocol.authenticateUser(testUser1).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Transport Layer Security (TLS) to Hidden. Email is sent.")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL })
    public void emailIsSentWhenTLSSupportIsSetToHidden() throws Exception
    {
        UserModel testUser1 = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        testFolder = dataContent.usingUser(testUser1).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.withJMX().enableSmtpTlsHidden();
        smtpProtocol.authenticateUser(testUser1).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Set Transport Layer Security (TLS) to Enabled. Email is sent.")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL })
    public void emailIsSentWhenTLSSupportIsSetToEnabled() throws Exception
    {
        UserModel testUser1 = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser1).createIMAPSite();
        testFolder = dataContent.usingUser(testUser1).usingSite(testSite).createFolder();
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        smtpProtocol.withJMX().enableSmtpTls();
        smtpProtocol.authenticateUser(testUser1).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .sendMail();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify inbound email port cannot be set to an used port")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL })
    public void inboundEmailPortWithUsedPort() throws Exception
    {
        try
        {
            smtpProtocol.withJMX().updateSmtpServerPort(465);
        }
        catch(J4pRemoteException e)
        {
            Assert.assertTrue(e.getMessage().contains("Permission denied"));
        }
        catch(BindException e)
        {
            Assert.assertTrue(e.getMessage().contains("Address already in use"));
        }
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify user with domain invalid.com cannot send emails when blocked senders is set to invalid.com")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL }, expectedExceptions = SMTPSendFailedException.class,
            expectedExceptionsMessageRegExp = ".*has been denied access.*")
    public void userCantSendMailIfDomainMatchesBlockedSenders() throws Exception
    {
        UserModel blockedUser = dataUser.createRandomTestUser();
        blockedUser.setDomain("invalid.com");
        smtpProtocol.withJMX().disableSmtpAuthentication();
        smtpProtocol.withJMX().updateSmtpBlockedSenders(".*@invalid.com");
        smtpProtocol.authenticateUser(blockedUser).and()
                .composeMessage()
                    .withRecipients("admin@tas-alfresco.com")
                    .withSubject("subject")
                    .withBody("body")
                .sendMail();
    }
}
