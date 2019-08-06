package org.alfresco.email.imap;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.email.EmailTest;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ImapDemoTest extends EmailTest
{   
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.IMAP }, executionType = ExecutionType.SANITY,
            description = "Verify user can connect successfully to IMAP client")
    @Test(groups = { "demo" })
    public void userShouldConnectThroughIMAPSuccessful() throws Exception
    {
        imapProtocol.authenticateUser(testUser).assertThat().userIsConnected()
            .disconnect()
            .assertThat().userIsNotConnected();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.IMAP }, executionType = ExecutionType.SANITY,
            description = "Verify user can create and delete a folder from UserHome")
    @Test(groups = { "demo" })
    public void userShouldCreateAndDeleteFolderInUserHomeRoot() throws Exception
    {
        FolderModel folderToCreate = FolderModel.getRandomFolderModel();

        imapProtocol.authenticateUser(testUser)
            .usingUserHome().createFolder(folderToCreate)
                .assertThat().existsInRepo()
            .usingResource(folderToCreate).delete()
            .assertThat().doesNotExistInRepo();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.IMAP }, executionType = ExecutionType.SANITY,
            description = "Verify user sees the unread messages")
    @Test(groups = { "demo" })
    public void userVerifiesNewMessage() throws Exception
    {
        imapProtocol.authenticateUser(testUser)
            .usingDataDictionary().usingPath("Imap Configs/Templates").assertThat().hasNewMessages();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.IMAP }, executionType = ExecutionType.SANITY,
            description = "Verify user creates new folder in an IMAP site")
    @Test(groups = { "demo" })
    public void userCreatesFolderInIMAPSite() throws Exception
    {
        FolderModel folderToCreate = FolderModel.getRandomFolderModel();

        imapProtocol.authenticateUser(testUser)
            .usingSite(testSite).createFolder(folderToCreate)
            .usingResource(folderToCreate)
            .assertThat().existsInRepo();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.IMAP }, executionType = ExecutionType.SANITY,
            description = "Verify the number of messages(files) from a specific folder")
    @Test(groups = { "demo" })
    public void userVerifiesNoOfMessagesFromFolder() throws Exception
    {
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        dataContent.usingResource(testFolder).createContent(DocumentType.TEXT_PLAIN);
        dataContent.createContent(DocumentType.TEXT_PLAIN);

        imapProtocol.authenticateUser(testUser)
            .usingSite(testSite).usingResource(testFolder)
            .assertThat().countMessagesIs(2);
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.IMAP }, executionType = ExecutionType.SANITY,
            description = "Verify user successful deletes message(file) from a folder")
    @Test(groups = { "demo" })
    public void userDeletesMessageFromFolder() throws Exception
    {
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        dataContent.usingResource(testFolder).createContent(DocumentType.TEXT_PLAIN);
        contentModel = dataContent.usingResource(testFolder).createContent(DocumentType.TEXT_PLAIN);

        imapProtocol.authenticateUser(testUser)
            .usingSite(testSite).usingResource(testFolder)
            .assertThat().countMessagesIs(2)
            .usingResource(contentModel).deleteMessage()
            .assertThat().countMessagesIs(1)
            .usingResource(contentModel).assertThat().doesNotExistInRepo();
    }
}
