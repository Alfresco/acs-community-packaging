package org.alfresco.webdav;

import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class WebDavDemoTest extends WebDavTest
{
    private SiteModel testSite;
    private FolderModel testFolder;

    @BeforeClass
    public void dataPreparation() throws Exception
    {
        testSite = dataSite.createPublicRandomSite();
    }

    @BeforeMethod
    public void useRandomTestFolder()
    {
        testFolder = FolderModel.getRandomFolderModel();
    }
   
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.WEBDAV,  "demo" })
    @TestRail(section={ TestGroup.PROTOCOLS, TestGroup.WEBDAV}, executionType = ExecutionType.SANITY,
            description ="Verify admin creates folder with WebDAV in document library")
    public void adminShouldCreateFolder() throws Exception
    {
        webDavProtocol.authenticateUser(dataUser.getAdminUser())
            .usingSite(testSite).createFolder(testFolder)
            .assertThat().existsInRepo();
    }

    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.WEBDAV,  "demo" })
    @TestRail(section={ TestGroup.PROTOCOLS, TestGroup.WEBDAV}, executionType = ExecutionType.SANITY,
            description ="Verify admin creates and deletes folder with WebDAV in document library")
    public void adminShouldDeleteFolder() throws Exception
    {
        webDavProtocol.authenticateUser(dataUser.getAdminUser())
            .usingSite(testSite).createFolder(testFolder)
            .assertThat().existsInRepo()
            .delete()
            .assertThat().doesNotExistInRepo();
    }

    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.WEBDAV, "demo" })
    @TestRail(section={ TestGroup.PROTOCOLS, TestGroup.WEBDAV}, executionType = ExecutionType.SANITY,
            description ="Verify admin creates file with WebDAV in document library")
    public void adminShouldCreateFileInFolder() throws Exception
    {
        FileModel newFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "the content");
        webDavProtocol.authenticateUser(dataUser.getAdminUser())
            .usingSite(testSite).createFolder(testFolder)
            .assertThat().existsInRepo()
            .then().usingResource(testFolder)
                .createFile(newFile).assertThat().existsInRepo();
    }

    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.WEBDAV,"demo" })
    @TestRail(section={ TestGroup.PROTOCOLS, TestGroup.WEBDAV}, executionType= ExecutionType.SANITY, description ="Verify admin moves folder with WebDAV in document library")
    public void adminShouldMoveFolder() throws Exception
    {
        FolderModel destinationFolder = FolderModel.getRandomFolderModel();
        webDavProtocol.authenticateUser(dataUser.getAdminUser())
            .usingSite(testSite).createFolder(testFolder).createFolder(destinationFolder)
            .usingResource(testFolder).moveTo(destinationFolder)
            .assertThat().existsInRepo();
        webDavProtocol.usingResource(testFolder).assertThat().doesNotExistInRepo();
    }
}
