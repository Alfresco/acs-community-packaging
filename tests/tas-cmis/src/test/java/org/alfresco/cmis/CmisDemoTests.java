package org.alfresco.cmis;

import java.util.List;

import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { "demo" })
public class CmisDemoTests extends CmisTest
{
    SiteModel testSite;
    FolderModel testFolder;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        testSite = dataSite.createPublicRandomSite();
        cmisApi.authenticateUser(dataUser.getAdminUser());
    }

    @BeforeMethod(alwaysRun=true)
    public void generateNewRandomTestFolder()
    {
        testFolder = FolderModel.getRandomFolderModel();
    }

    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user creates and deletes folder in DocumentLibrary with CMIS")
    
    public void adminShouldCreateFolder() throws Exception
    {
        cmisApi.usingSite(testSite)
            .createFolder(testFolder).and().assertThat().existsInRepo()
            .when().delete().assertThat().doesNotExistInRepo();
    }

    
    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user is not able to create folders with same name twice using CMIS")
    @Test(expectedExceptions = CmisContentAlreadyExistsException.class)
    public void exceptionThrownOnCreatingFolderTwice() throws Exception
    {
        cmisApi.usingSite(testSite)
            .createFolder(testFolder).and().assertThat().existsInRepo()
                .createFolder(testFolder);
    }

    
    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY,
            description = "Verify admin user is not able to delete folder with one file in it using CMIS")
    @Test(expectedExceptions = CmisConstraintException.class)
    public void cannotDeleteFolderWithAtLeastOneChildFile() throws Exception
    {
        FileModel fileModel = FileModel.getRandomFileModel(FileType.PDF);
        cmisApi.usingSite(testSite)
            .createFolder(testFolder).and().assertThat().existsInRepo()
            .then().usingResource(testFolder)
                .createFile(fileModel).assertThat().existsInRepo()
            .when().usingResource(testFolder).delete().assertThat().doesNotExistInRepo();
    }

    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user deletes folder tree with CMIS")
    public void adminShouldDeleteFolderTree() throws Exception
    {
        FolderModel parentFolder = FolderModel.getRandomFolderModel();
        FileModel fileModel = FileModel.getRandomFileModel(FileType.PDF);
        cmisApi.usingSite(testSite)
            .createFolder(parentFolder).and().assertThat().existsInRepo()
            .then().usingResource(parentFolder)
                .createFile(fileModel).and().assertThat().existsInRepo();
        cmisApi.when().usingResource(parentFolder).deleteFolderTree().assertThat().doesNotExistInRepo()
            .and().usingResource(fileModel).assertThat().doesNotExistInRepo();
    }

    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY,
            description = "Verify admin user creates and deletes files in DocumentLibrary with CMIS")
    public void adminShouldCreateAndDeleteDocument() throws Exception
    {
        FileModel fileModel = FileModel.getRandomFileModel(FileType.MSWORD);
        cmisApi.usingSite(testSite).createFile(fileModel)
            .and().assertThat().existsInRepo()
            .then().delete().and().assertThat().doesNotExistInRepo();
    }

    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user creates and renames files in DocumentLibrary with CMIS")
    public void adminShouldRenameDocument() throws Exception
    {
        FileModel fileModel = FileModel.getRandomFileModel(FileType.MSWORD);
        cmisApi.usingSite(testSite).createFile(fileModel).and().assertThat().existsInRepo()
            .then().rename(fileModel.getName() + "-edit").and().assertThat().existsInRepo()
            .then().usingResource(fileModel).assertThat().doesNotExistInRepo();
    }

    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user creates and renames folder in DocumentLibrary with CMIS")
    public void adminShouldRenameFolder() throws Exception
    {
        FolderModel renameFolder = FolderModel.getRandomFolderModel();
        cmisApi.usingSite(testSite).createFolder(renameFolder)
            .then().rename(renameFolder.getName() + "-v2").and().assertThat().existsInRepo()
            .then().usingResource(renameFolder).assertThat().doesNotExistInRepo();
    }
    
    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user sets and verifies content files in DocumentLibrary with CMIS")
    public void adminShouldGetDocumentContent() throws Exception
    {
        FileModel fileModel = FileModel.getRandomFileModel(FileType.MSWORD);
        fileModel.setContent(fileModel.getName());
        cmisApi.usingSite(testSite)
            .createFile(fileModel).and().assertThat().contentIs(fileModel.getName())
            .update("+update").and().assertThat().contentIs(fileModel.getName() + "+update");
    }
    
    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user can get the children from a parent folder with CMIS")
    public void adminShouldGetChildrenFromParentFolder() throws Exception
    {
        FolderModel parentFolder = FolderModel.getRandomFolderModel();
        FolderModel subFolder1 = FolderModel.getRandomFolderModel();
        FolderModel subFolder2 = FolderModel.getRandomFolderModel();
        FileModel subFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        cmisApi.usingSite(testSite)
            .createFolder(parentFolder).and().assertThat().existsInRepo()
            .then().usingResource(parentFolder)
                .createFolder(subFolder1)
                .createFolder(subFolder2)
                .createFile(subFile);
        List<FolderModel> subFolders = cmisApi.usingResource(parentFolder).getFolders();
        Assert.assertTrue(subFolders.size() == 2);
        List<FileModel> subFiles = cmisApi.usingResource(parentFolder).getFiles();
        Assert.assertTrue(subFiles.size() == 1);
    }
    
    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user can move document with CMIS")
    public void adminShouldMoveDocument() throws Exception
    {
        FolderModel destination = FolderModel.getRandomFolderModel();
        FileModel source = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        
        cmisApi.usingSite(testSite)
            .createFolder(destination).createFile(source)
            .then().moveTo(destination).and().assertThat().existsInRepo();
        cmisApi.usingResource(source).assertThat().doesNotExistInRepo();
    }
    
    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user can copy document with CMIS")
    public void adminShouldCopyDocument() throws Exception
    {
        FolderModel destination = FolderModel.getRandomFolderModel();
        FileModel source = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        
        cmisApi.usingSite(testSite)
            .createFolder(destination).createFile(source)
            .then().copyTo(destination).and().assertThat().existsInRepo();
        cmisApi.usingResource(source).assertThat().existsInRepo();
    }
    
    @Test(enabled=false)
    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user can move folder with children in it using CMIS")
    public void adminShouldMoveFolderWithChild() throws Exception
    {
        FolderModel destination = FolderModel.getRandomFolderModel();
        FolderModel parentFolder = FolderModel.getRandomFolderModel();
        FileModel subFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        
        cmisApi.usingSite(testSite)
            .createFolder(destination).createFolder(parentFolder)
                .then().usingResource(parentFolder).createFile(subFile)
                .then().usingResource(parentFolder).moveTo(destination).and().assertThat().existsInRepo();
        
        cmisApi.usingResource(parentFolder).assertThat().doesNotExistInRepo()
            .and().usingResource(subFile).assertThat().doesNotExistInRepo();
        Assert.assertTrue(cmisApi.usingResource(destination).getFiles().size() == 1);
    }
    
    @Test(enabled=false)
    @TestRail(section = {"cmis-api"}, executionType=ExecutionType.SANITY, 
            description = "Verify admin user can copy folder with children in it using CMIS")
    public void adminShouldCopyFolderWithChild() throws Exception
    {
        FolderModel destination = FolderModel.getRandomFolderModel();
        FolderModel parentFolder = FolderModel.getRandomFolderModel();
        FileModel subFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        
        cmisApi.usingSite(testSite)
            .createFolder(destination).createFolder(parentFolder)
                .then().usingResource(parentFolder).createFile(subFile)
                .then().usingResource(parentFolder).copyTo(destination).and().assertThat().existsInRepo();
        
        cmisApi.usingResource(parentFolder).assertThat().existsInRepo()
            .and().usingResource(subFile).assertThat().existsInRepo();
        Assert.assertTrue(cmisApi.usingResource(destination).getFiles().size() == 1);
    }
}