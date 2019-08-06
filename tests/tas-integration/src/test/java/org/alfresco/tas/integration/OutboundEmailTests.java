package org.alfresco.tas.integration;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import org.alfresco.email.EmailProperties;
import org.alfresco.email.dsl.ServerConfiguration;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataEmail;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.*;

public class OutboundEmailTests extends IntegrationTest
{
    @Autowired
    DataEmail dataEmail;

    UserModel autoTestUser;
    SiteModel testSite;
    FileModel fileModel;

    String subject;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception
    {
        smtpProtocol.withJMX().updateMailPassword(emailProperties.getMailPassword());
        ServerConfiguration.save(smtpProtocol.withJMX(), emailProperties);

        if (dataUser.isUserInRepo("autotest"))
            dataUser.deleteUser(new UserModel("autotest", ""));

        autoTestUser = dataUser.createUserWithCustomEmailAddressInAlfresco("autotest", "test", "alfness.com");

        subject = "You have been assigned a task";
    }

    @Test(groups = { TestGroup.INTEGRATION, TestGroup.REQUIRE_JMX, TestGroup.SMTP, TestGroup.FULL})
    @TestRail(section = { TestGroup.INTEGRATION, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify site manager receives e-mail notification when user requests to join a moderated site")
    public void siteManagerShouldReceiveEmailNotificationForJoinRequestOnModeratedSite() throws Exception
    {
        testSite = dataSite.createPublicRandomSite();
        fileModel = dataContent.usingSite(testSite).createContent(FileModel.getRandomFileModel(FileType.TEXT_PLAIN));
        dataWorkflow.usingSite(testSite).usingResource(fileModel).createSingleReviewerTaskAndAssignTo(autoTestUser);

        autoTestUser.setDomain("alfness.com");
        Message message = dataEmail.assertEmailHasBeenReceived(autoTestUser, smtpProtocol.withJMX().getMailHost(), 143, "imap", subject)[0];

        Assert.assertTrue(message.getContent().toString().contains(fileModel.getName()));
    }

    @Test(groups = { TestGroup.INTEGRATION, TestGroup.REQUIRE_JMX, TestGroup.SMTP, TestGroup.FULL})
    @TestRail(section = { TestGroup.INTEGRATION, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify e-mail content using other valid encoding: UTF-16")
    public void verifyEmailContentUsingOtherValidEncoding() throws Exception
    {
        smtpProtocol.withJMX().updateMailEncoding("UTF-16");

        testSite = dataSite.createPublicRandomSite();
        fileModel = dataContent.usingSite(testSite).createContent(FileModel.getRandomFileModel(FileType.TEXT_PLAIN));
        dataWorkflow.usingSite(testSite).usingResource(fileModel).createSingleReviewerTaskAndAssignTo(autoTestUser);

        autoTestUser.setDomain("alfness.com");
        Message message = dataEmail.assertEmailHasBeenReceived(autoTestUser, smtpProtocol.withJMX().getMailHost(), 143, "imap", subject)[0];

        Assert.assertTrue(message.getContentType().contains("UTF-16"));
        Assert.assertTrue(message.getContent().toString().contains(fileModel.getName()));
    }

    @TestRail(section = { TestGroup.INTEGRATION, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify that with EditableSenderAddress unchecked the email FROM is the Default Sender's Address")
    @Test(groups = { TestGroup.INTEGRATION, TestGroup.REQUIRE_JMX, TestGroup.SMTP, TestGroup.FULL})
    public void verifyFROMHeaderWithEditableSenderAddressUnchecked() throws Exception
    {
        smtpProtocol.withJMX().disableMailFrom();
        smtpProtocol.withJMX().updateMailFromDefault("testDefaultSender@alfness.com");

        testSite = dataSite.createPublicRandomSite();
        fileModel = dataContent.usingSite(testSite).createContent(FileModel.getRandomFileModel(FileType.TEXT_PLAIN));
        dataWorkflow.usingSite(testSite).usingResource(fileModel).createSingleReviewerTaskAndAssignTo(autoTestUser);

        autoTestUser.setDomain("alfness.com");
        Message message = dataEmail.assertEmailHasBeenReceived(autoTestUser, smtpProtocol.withJMX().getMailHost(), 143, "imap", subject)[0];

        Assert.assertTrue(((InternetAddress) message.getFrom()[0]).getAddress().contains(smtpProtocol.withJMX().getMailFromDefault()));
        Assert.assertTrue(message.getContent().toString().contains(fileModel.getName()));
    }

    @TestRail(section = { TestGroup.INTEGRATION, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify that with EditableSenderAddress checked the email FROM is not Default Sender's Address")
    @Test(groups = { TestGroup.INTEGRATION, TestGroup.REQUIRE_JMX, TestGroup.SMTP, TestGroup.FULL})
    public void verifyFROMHeaderWithEditableSenderAddressChecked() throws Exception
    {
        testSite = dataSite.createPublicRandomSite();
        fileModel = dataContent.usingSite(testSite).createContent(FileModel.getRandomFileModel(FileType.TEXT_PLAIN));
        dataWorkflow.usingSite(testSite).usingResource(fileModel).createSingleReviewerTaskAndAssignTo(autoTestUser);

        autoTestUser.setDomain("alfness.com");
        Message message = dataEmail.assertEmailHasBeenReceived(autoTestUser, smtpProtocol.withJMX().getMailHost(),  143, "imap", subject)[0];

        Assert.assertTrue(((InternetAddress) message.getFrom()[0]).getAddress().contains("admin@alfresco.com"));
        Assert.assertTrue(message.getContent().toString().contains(fileModel.getName()));
    }

    @Test(groups = { TestGroup.INTEGRATION, TestGroup.REQUIRE_JMX, TestGroup.FULL, TestGroup.EXTENTION_POINTS })
    @TestRail(section = { TestGroup.INTEGRATION, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Outbound Email is not sent when SMTPS protocol is used")
    public void sendOutboundEmailUsingSMTPSProtocol() throws Exception
    {
        smtpProtocol.withJMX().updateMailProtocol("smtps");
        smtpProtocol.withJMX().updateMailTimeout(1);

        testSite = dataSite.createPublicRandomSite();
        fileModel = dataContent.usingSite(testSite).createContent(FileModel.getRandomFileModel(FileType.TEXT_PLAIN));
        dataWorkflow.usingSite(testSite).usingResource(fileModel).createSingleReviewerTaskAndAssignTo(autoTestUser);

        Utility.waitToLoopTime(5);

        dataUser.usingLastServerLogLines(1000).assertLogLineIs("Failed to send email to [autotest] : " +
                "org.springframework.mail.MailSendException: Mail server connection failed; nested exception is " +
                "javax.mail.MessagingException: Could not connect to SMTP host: 172.29.100.164, port: 25;");
    }

    @AfterMethod(alwaysRun = true)
    public void testCleanup() throws Exception
    {
        autoTestUser.setDomain(null);
        ServerConfiguration.restore(smtpProtocol.withJMX());
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception
    {
        smtpProtocol.withJMX().updateMailPassword("invalidPassword");
    }

}
