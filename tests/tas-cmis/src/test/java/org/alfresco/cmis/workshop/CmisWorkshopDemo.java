package org.alfresco.cmis.workshop;

import org.alfresco.cmis.CmisTest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.testng.annotations.Test;

/**
 * Demo workshop
 */
@SuppressWarnings("unused")
public class CmisWorkshopDemo extends CmisTest
{
    /**
     * As an engineer I want to create a new random test user
     * I want to use that user to defined a new random site
     * And using CMIS protocol, to create a folder
     * 
     */    
    @Test(groups="demo")
    public void myFirstScenario() throws Exception
    {
        UserModel myRandomUser = dataUser.createRandomTestUser();
        SiteModel myRandomSite = dataSite.usingUser(myRandomUser).createPublicRandomSite();
        FolderModel myRandomFolder = FolderModel.getRandomFolderModel();
        
        //add code solution here      
         
    }
    
    /**
     * As an engineer I want to create the following structure in my site
     * using CMIS protocol.
     * 
     * in Sites/<randomSite>/documentLibrary create
     *    --parent (folder)
     *    -------child (folder)
     *    ------------ file.txt (file)
     */
    @Test(groups="demo")
    public void mySecondScenario() throws Exception
    {
        SiteModel mySite = new SiteModel("workshop"); //this site should exist (using this approach for demo purposes)
        FolderModel parent = FolderModel.getRandomFolderModel();
        FolderModel child = FolderModel.getRandomFolderModel();
        FileModel file = FileModel.getRandomFileModel(FileType.HTML, "tas");
        
        //add code solution here
    }
}
