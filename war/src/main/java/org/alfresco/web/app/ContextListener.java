/*
 * #%L
 * Alfresco Repository WAR Community
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.web.app;

import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.transaction.UserTransaction;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationContext;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;


/**
 * ServletContextListener implementation that initialises the application.
 * 
 * NOTE: This class must appear after the Spring context loader listener
 * 
 * @author gavinc
 */
public class ContextListener implements ServletContextListener, HttpSessionListener
{
   private static Log logger = LogFactory.getLog(ContextListener.class);

   private ServletContext servletContext;

   /**
    * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
    */
   public void contextInitialized(ServletContextEvent event)
   {
       // make sure that the spaces store in the repository exists
      this.servletContext = event.getServletContext();
      WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
      
      // If no context has been initialised, exit silently so config changes can be made
      if (ctx == null)
      {
          return;
      }
      
      ServiceRegistry registry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
      TransactionService transactionService = registry.getTransactionService();
      NodeService nodeService = registry.getNodeService();
      SearchService searchService = registry.getSearchService();
      NamespaceService namespaceService = registry.getNamespaceService();
      AuthenticationContext authenticationContext = (AuthenticationContext) ctx
            .getBean("authenticationContext");

      // repo bootstrap code for our client
      UserTransaction tx = null;
      NodeRef companySpaceNodeRef = null;
      try
      {
         tx = transactionService.getUserTransaction();
         tx.begin();
         authenticationContext.setSystemUserAsCurrentUser();

         // get and setup the initial store ref and root path from config
         StoreRef storeRef = getStoreRef(servletContext);
         
         // get root path
         String rootPath = Application.getRootPath(servletContext);

         // Extract company space id and store it in the Application object
         companySpaceNodeRef = getCompanyRoot(nodeService, searchService, namespaceService, storeRef, rootPath);
         Application.setCompanyRootId(companySpaceNodeRef.getId());
         
         // commit the transaction
         tx.commit();
      }
      catch (Throwable e)
      {
         // rollback the transaction
         try
         {
            if (tx != null)
            {
               tx.rollback();
            }
         }
         catch (Exception ex) {}
         
         logger.error("Failed to initialise ", e);
         throw new AlfrescoRuntimeException("Failed to initialise ", e);
      }
      finally
      {
          try
          {
             authenticationContext.clearCurrentSecurityContext();
          }
          catch (Exception ex) {}
      }
   }

   
   /**
    * {@inheritDoc}
    */
   public void contextDestroyed(ServletContextEvent event)
   {
       // NOOP
   }

   /**
    * Session created listener
    */
   public void sessionCreated(HttpSessionEvent event)
   {
      if (logger.isDebugEnabled())
         logger.debug("HTTP session created: " + event.getSession().getId());
   }

   /**
    * Session destroyed listener
    */
   public void sessionDestroyed(HttpSessionEvent event)
   {
      if (logger.isDebugEnabled())
         logger.debug("HTTP session destroyed: " + event.getSession().getId());
   }

    /**
     * Returns a store reference object.
     * This method is used to setup the cached value by the ContextListener initialisation methods
     *
     * @return The StoreRef object
     */
    private static StoreRef getStoreRef(ServletContext context)
    {
        return  Application.getRepositoryStoreRef(context);
    }

    /**
     * Returns a company root node reference object.
     *
     * @return The NodeRef object
     */
    private static NodeRef getCompanyRoot(NodeService nodeService, SearchService searchService, NamespaceService namespaceService, StoreRef storeRef, String rootPath)
    {
        // check the repository exists, create if it doesn't
        if (nodeService.exists(storeRef) == false)
        {
            throw new AlfrescoRuntimeException("Store not created prior to application startup: " + storeRef);
        }

        // get hold of the root node
        NodeRef rootNodeRef = nodeService.getRootNode(storeRef);

        // see if the company home space is present
        if (rootPath == null)
        {
            throw new AlfrescoRuntimeException("Root path has not been configured");
        }

        List<NodeRef> nodes = searchService.selectNodes(rootNodeRef, rootPath, null, namespaceService, false);
        if (nodes.size() == 0)
        {
            throw new AlfrescoRuntimeException("Root path not created prior to application startup: " + rootPath);
        }

        // return company root
        return nodes.get(0);
    }
}
