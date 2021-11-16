package org.alfresco.email.imap;

import org.alfresco.email.EmailTest;
import org.alfresco.utility.model.*;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ImapReadMessagesAcsTests extends EmailTest
{
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createIMAPSite();
        adminUser = dataUser.getAdminUser();
        adminSite = dataSite.usingAdmin().createIMAPSite();
    }

    @TestRail(section = { TestGroup.PROTOCOLS, TestGroup.IMAP }, executionType = ExecutionType.REGRESSION,
            description = "Verify that site manager can see wiki pages via IMAP")
    @Test(groups = { TestGroup.PROTOCOLS, TestGroup.IMAP, TestGroup.CORE })
    public void siteManagerCanViewWikiPages() throws Exception
    {
        dataWiki.usingUser(testUser).usingSite(testSite).createRandomWiki();
        /* @Category(IntermittentlyFailingTests.class) ACS-959 Intermittent failure on next line. @Category not supported by TAS tests. */
// ACS-2268 comment out:        imapProtocol.authenticateUser(testUser).usingSiteWikiContainer(testSite).assertThat().countMessagesIs(1);
    }
}
