package org.alfresco.cmis.workshop;

import org.alfresco.cmis.CmisTest;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.UserModel;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Demo workshop
 */
public class CmisWorkshop extends CmisTest
{
    UserModel myUser = new UserModel("<your-username>", "superSecretPassword");
        
    @BeforeClass
    public void userPreparation() throws DataPreparationException
    {
      // add code for creating a user in repository with your name if not exist
      // we will reuse this UserModel further in our scenarios         
    }
    
    @Test
    public void createFolderInMyRandomSite() throws Exception
    {
        // create a new random site using your UserModel
       
        // in this random site, with "cmisApi" create one folder named "MyFirstFolder"
        
        // with "cmisApi" assert folder has been created     
    }
   
    
    @Test
    public void createFileWithContentInMyPrivateSite() throws Exception
    {        
        // if site is not created, create a new PRIVATE site named as your username (ex: MySitePaul, MySiteCorina)
        
        // in this site, with "cmisApi" create one random HTML file (hint: use FileModel) with content "tas" and assert content is "tas"                
    }    
}
