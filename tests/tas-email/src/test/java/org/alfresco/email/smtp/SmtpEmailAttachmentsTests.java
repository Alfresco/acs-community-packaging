package org.alfresco.email.smtp;

import org.alfresco.email.SMTPTest;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.Test;

import java.io.File;

public class SmtpEmailAttachmentsTests extends SMTPTest
{
    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.SMTP }, executionType = ExecutionType.REGRESSION,
            description = "Verify that email attachment is present in the repository")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.SMTP, TestGroup.REQUIRE_JMX, TestGroup.CORE })
    public void sendEmailWithAttachmentAndVerifyFileIsPresentInRepository() throws Exception
    {
        File attachment = Utility.getResourceTestDataFile("imap-resource");
        FileModel fileModel = FileModel.getFileModelBasedOnTestDataFile("imap-resource");
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        fileModel.setCmisLocation(Utility.buildPath(testFolder.getCmisLocation(), "imap-resource"));
        String alias = dataContent.usingSite(testSite).usingResource(testFolder).addEmailAlias("alias" + System.currentTimeMillis());

        smtpProtocol.authenticateUser(testUser).and()
                .composeMessage()
                .withRecipients(alias + "@tas-alfresco.com")
                .withSubject("subject")
                .withBody("body")
                .withAttachments(attachment)
                .sendMail();

        imapProtocol.authenticateUser(testUser).usingResource(fileModel).assertThat().existsInImap()
                .then().assertThat().existsInRepo();
    }
}
