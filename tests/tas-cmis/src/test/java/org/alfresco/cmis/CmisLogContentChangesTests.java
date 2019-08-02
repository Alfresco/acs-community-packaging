package org.alfresco.cmis;

import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.chemistry.opencmis.commons.enums.ChangeType;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = {TestGroup.REQUIRE_JMX})
public class CmisLogContentChangesTests extends CmisTest
{
    UserModel testUser;
    SiteModel testSite;
    FileModel testFile;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        cmisApi.withJMX().enableCMISAudit();
        testSite = dataSite.usingUser(dataUser.getAdminUser()).createPublicRandomSite();
        cmisApi.authenticateUser(dataUser.getAdminUser());
    }
    
    @AfterClass
    public void disableCMISAudit() throws Exception
    {
        cmisApi.withJMX().disableCMISAudit();
    }
    
    @TestRail(section = {"cmis-api"}, executionType= ExecutionType.SANITY,
            description = "Admin user can verify that document is created in cmis logs")
    @Test(groups = { TestGroup.SANITY, TestGroup.CMIS })
    public void adminCanVerifyCmisLogForCreateDocument() throws Exception
    {
        testFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        cmisApi.usingSite(testSite)
                .createFile(testFile) 
                .assertThat().contentModelHasChanges(testFile, ChangeType.CREATED);
    }
    
    @TestRail(section = {"cmis-api"}, executionType= ExecutionType.SANITY,
            description = "Admin user can verify that document is renamed in cmis logs")
    @Test(groups = { TestGroup.SANITY, TestGroup.CMIS })
    public void adminCanVerifyCmisLogForRenameDocument() throws Exception
    {
        testFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        cmisApi.usingSite(testSite)
                .createFile(testFile).rename(testFile.getName() + "-edit")
                .assertThat().contentModelHasChanges(testFile, ChangeType.CREATED, ChangeType.UPDATED);
    }
    
    @TestRail(section = {"cmis-api"}, executionType= ExecutionType.SANITY,
            description = "Admin user can verify that document is moved in cmis logs")
    @Test(groups = { TestGroup.SANITY, TestGroup.CMIS })
    public void adminCanVerifyCmisLogForMovedDocument() throws Exception
    {
        FolderModel folder = FolderModel.getRandomFolderModel();
        testFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        cmisApi.usingSite(testSite)
                .createFile(testFile).createFolder(folder)
                .then().usingResource(testFile).moveTo(folder)
                .assertThat().contentModelHasChanges(testFile, ChangeType.CREATED, ChangeType.UPDATED);
    }
    
    @TestRail(section = {"cmis-api"}, executionType= ExecutionType.SANITY,
            description = "Admin user can verify that document is deleted in cmis logs")
    @Test(groups = { TestGroup.SANITY, TestGroup.CMIS })
    public void adminCanVerifyCmisLogForDeletedDocument() throws Exception
    {
        FolderModel folder = FolderModel.getRandomFolderModel();
        testFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        cmisApi.usingSite(testSite)
                .createFile(testFile).delete()
                .createFolder(folder).delete()
                .assertThat().contentModelHasChanges(testFile, ChangeType.CREATED, ChangeType.DELETED)
                .and().assertThat().contentModelHasChanges(folder, ChangeType.CREATED, ChangeType.DELETED);
    }
    
    @TestRail(section = {"cmis-api"}, executionType= ExecutionType.REGRESSION,
            description = "In cmis logs doesn't contain log for deleted document with invalid changLogToken ")
    @Test(groups = { TestGroup.REGRESSION, TestGroup.CMIS })
    public void deletedDocumentDoesNotHaveCmisLogChangesWithInvalidToken() throws Exception
    {
        testFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        cmisApi.usingSite(testSite)
                .createFile(testFile).delete()
                .assertThat().contentModelDoesnotHaveChangesWithWrongToken(testFile, ChangeType.CREATED, ChangeType.DELETED);
    }
}
