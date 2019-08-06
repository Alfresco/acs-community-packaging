package org.alfresco.email.smtp;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SmtpOverwriteDuplicatesTests extends SMTPTest
{
    private UserModel adminUser;
    private String alias;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Files with the same subject are overwritten after the email is sent when Overwrite Duplicates is enabled")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY })
    public void overwriteFileWhenOverwriteDuplicatesIsEnabled() throws Exception
    {
        smtpProtocol.withJMX().enableSmtpOverwriteDuplicates();
        
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());

        for (int i = 0; i < 3; i++)
        {
            smtpProtocol.authenticateUser(adminUser).and()
            .composeMessage()
            .withRecipients(alias + "@tas-alfresco.com")
            .withSubject("subject")
            .withBody("body")
            .sendMail();
        }

        imapProtocol.authenticateUser(testUser).usingResource(testFolder).assertThat().countMessagesIs(1)
            .and().assertThat().messageSubjectIs("subject");
    }
    
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.SANITY,
            description = "Files with the same subject are not overwritten after the email is sent when Overwrite Duplicates is disabled")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.SANITY })
    public void overwriteFileWhenOverwriteDuplicatesIsDisabled() throws Exception
    {
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());
        
        smtpProtocol.withJMX().disableSmtpOverwriteDuplicates();

        for (int i = 0; i < 3; i++)
        {
            smtpProtocol.authenticateUser(adminUser).and()
            .composeMessage()
            .withRecipients(alias + "@tas-alfresco.com")
            .withSubject("subject")
            .withBody("body")
            .sendMail();
        }
        
        imapProtocol.authenticateUser(testUser).usingResource(testFolder).assertThat().countMessagesIs(3);
    }
}
