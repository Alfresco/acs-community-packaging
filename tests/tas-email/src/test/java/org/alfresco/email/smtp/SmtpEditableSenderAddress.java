package org.alfresco.email.smtp;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class SmtpEditableSenderAddress extends SMTPTest
{
    private String alias;
    
    @BeforeMethod(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Uncheck EditableSenderAddress and set no value to Default Sender's Address. Email from current user is received.")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL})
    public void emailIsReceivedWhenSenderAddressIsNotEditableAndNoDefaultSender() throws Exception
    {
        smtpProtocol.withJMX().disableMailFrom();
        smtpProtocol.withJMX().updateMailFromDefault("");
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("EditableSenderAddress")
                .withBody("body")
                .sendMail();
        
        imapProtocol.authenticateUser(testUser).usingResource(testFolder).assertThat().countMessagesIs(1)
            .and().assertThat().messageSenderNameIs("EditableSenderAddress", testUser.getUsername());
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Check EditableSenderAddress and set no value to Default Sender's Address. Email from current user is received.")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL})
    public void emailIsReceivedWhenSenderAddressIsEditableAndNoDefaultSender() throws Exception
    {
        smtpProtocol.withJMX().enableMailFrom();
        smtpProtocol.withJMX().updateMailFromDefault("");
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("EditableSenderAddress")
                .withBody("body")
                .sendMail();
        
        imapProtocol.authenticateUser(testUser).usingResource(testFolder).assertThat().countMessagesIs(1)
            .and().assertThat().messageSenderNameIs("EditableSenderAddress", testUser.getUsername());
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Uncheck EditableSenderAddress and set invalid email format to Default Sender's Address. Email from current user is received.")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL})
    public void emailIsReceivedWhenSenderAddressIsNotEditableWithInvalidSender() throws Exception
    {
        smtpProtocol.withJMX().disableMailFrom();
        smtpProtocol.withJMX().updateMailFromDefault("testDefaultSender");
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("EditableSenderAddress")
                .withBody("body")
                .sendMail();
        
        imapProtocol.authenticateUser(testUser).usingResource(testFolder).assertThat().countMessagesIs(1)
            .and().assertThat().messageSenderNameIs("EditableSenderAddress", testUser.getUsername());
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Uncheck EditableSenderAddress and set valid email to Default Sender's Address. Email from current user is received.")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL})
    public void emailIsReceivedWhenSenderAddressIsNotEditableWithValidSender() throws Exception
    {
        smtpProtocol.withJMX().disableMailFrom();
        smtpProtocol.withJMX().updateMailFromDefault("alfresco.cloud@gmail.com");
        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("EditableSenderAddress")
                .withBody("body")
                .sendMail();
        
        imapProtocol.authenticateUser(testUser).usingResource(testFolder).assertThat().countMessagesIs(1)
            .and().assertThat().messageSenderNameIs("EditableSenderAddress", testUser.getUsername());
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Check EditableSenderAddress and set invalid email format to Default Sender's Address. Create some activities and check no email is received.")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.FULL})
    public void emailsAreNotReceivedWhenSenderAddressIsEditableWithInvalidSender() throws Exception
    {      
        smtpProtocol.withJMX().enableMailFrom();
        smtpProtocol.withJMX().updateMailFromDefault("testDefaultSender");
        
        SiteModel testSite = dataSite.usingUser(testUser).createIMAPSite();
        FolderModel testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        
        imapProtocol.authenticateUser(testUser).usingResource(testFolder).assertThat().countMessagesIs(0);
    }
}
