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
import java.util.Locale;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.web.config.ClientConfigElement;
import org.alfresco.web.config.LanguagesConfigElement;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.config.Config;
import org.springframework.extensions.config.ConfigService;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Utilities class
 * 
 * @author gavinc
 */
public class Application
{
   private static final String LOCALE = "locale";
   private static final String USE_SESSION_LOCALE = "USE_SESSION_LOCALE";
   
   public static final String BEAN_CONFIG_SERVICE = "webClientConfigService";
   public static final String BEAN_IMPORTER_BOOTSTRAP = "spacesBootstrap";

   public static final String MESSAGE_BUNDLE = "alfresco.messages.webclient";
   
   private static StoreRef repoStoreRef;
   private static String rootPath;
   private static String companyRootId;
   
   /**
    * Private constructor to prevent instantiation of this class 
    */
   private Application()
   {
   }

   /**
    * @return Returns the repository store URL (retrieved from config service)
    */
   public static StoreRef getRepositoryStoreRef(ServletContext context)
   {
      return getRepositoryStoreRef(WebApplicationContextUtils.getRequiredWebApplicationContext(context));
   }

   /**
    * @return Returns id of the company root
    * 
    * @deprecated Replace with user-context-specific getCompanyRootId (e.g. could be tenant-specific)
    */
   public static String getCompanyRootId()
   {
      return companyRootId;
   }
   
   /**
    * Sets the company root id. This is setup by the ContextListener.
    * 
    * @param id The company root id
    * 
    * @deprecated Replace with user-context-specific getCompanyRootId (e.g. could be tenant-specific)
    */
   public static void setCompanyRootId(String id)
   {
      companyRootId = id;
   }

   /**
    * @return Returns the root path for the application
    */
   public static String getRootPath(ServletContext context)
   {
      return getRootPath(WebApplicationContextUtils.getRequiredWebApplicationContext(context));
   }

   /**
    * Set the language locale for the current user session.
    * 
    * @param session        HttpSession for current user
    * @param code           The ISO locale code to set
    */
   @Deprecated
   public static void setLanguage(HttpSession session, String code)
   {
      Locale locale = I18NUtil.parseLocale(code);
      
      session.setAttribute(LOCALE, locale);
      session.removeAttribute(MESSAGE_BUNDLE);
      session.setAttribute(USE_SESSION_LOCALE, Boolean.TRUE);
      
      // Set the current locale in the server thread
      I18NUtil.setLocale(locale);
   }

   /**
    * Return the language Locale for the current user Session.
    * 
    * @param session
    *           HttpSession for the current user
    * @param useInterfaceLanguage
    *           If the session language hasn't been established yet, should we base it on user preferences?
    * @return Current language Locale set or the VM default if none set - never null
    */
   public static Locale getLanguage(HttpSession session, boolean useInterfaceLanguage)
   {
      Boolean useSessionLocale = (Boolean)session.getAttribute(USE_SESSION_LOCALE);
      if (useSessionLocale == null)
      {
         useSessionLocale = useInterfaceLanguage;
         session.setAttribute(USE_SESSION_LOCALE, useSessionLocale);
      }
      Locale locale = (Locale)session.getAttribute(LOCALE);
      if (locale == null || (!locale.equals(I18NUtil.getLocale()) && !useInterfaceLanguage))
      {
         if (useSessionLocale && useInterfaceLanguage)
         {
            // get from web-client config - the first item in the configured list of languages
            locale = getLanguage(WebApplicationContextUtils.getRequiredWebApplicationContext(session
               .getServletContext()));

            // This is an interface session - the same locale will be used for the rest of the session
            session.setAttribute(LOCALE, locale);
         }
         else
         {
            // Get the request default, already decoded from the request headers
            locale = I18NUtil.getLocale();
         }
      }
      return locale;
   }
   
   /**
    * Return the configured language Locale for the application context
    * 
    * @param ctx
    *           the application context
    * @return Current language Locale set or the VM default if none set - never null
    */
   public static Locale getLanguage(ApplicationContext ctx)
   {
      // get from web-client config - the first item in the configured list of languages
      Config config = ((ConfigService) ctx.getBean(Application.BEAN_CONFIG_SERVICE)).getConfig("Languages");
      LanguagesConfigElement langConfig = (LanguagesConfigElement) config
            .getConfigElement(LanguagesConfigElement.CONFIG_ELEMENT_ID);
      List<String> languages = langConfig.getLanguages();
      if (languages != null && languages.size() != 0)
      {
         return I18NUtil.parseLocale(languages.get(0));
      }
      else
      {
         // failing that, use the server default locale
         return Locale.getDefault();
      }
   }
   
   /**
    * Helper to get the ConfigService instance
    * 
    * @param context        ServletContext
    * 
    * @return ConfigService
    */
   public static ConfigService getConfigService(ServletContext context)
   {
      return (ConfigService)WebApplicationContextUtils.getRequiredWebApplicationContext(context).getBean(
            Application.BEAN_CONFIG_SERVICE);
   }
   
   /**
    * Helper to get the client config element from the config service
    * 
    * @param context FacesContext
    * @return The ClientConfigElement
    */
   public static ClientConfigElement getClientConfig(ServletContext context)
   {
      return (ClientConfigElement)getConfigService(context).getGlobalConfig().
         getConfigElement(ClientConfigElement.CONFIG_ELEMENT_ID);
   }
   
   /**
    * Returns the root path for the application
    * 
    * @param context The spring context
    * @return The application root path
    */
   private static String getRootPath(WebApplicationContext context)
   {
      if (rootPath == null)
      {
         ImporterBootstrap bootstrap = (ImporterBootstrap)context.getBean(BEAN_IMPORTER_BOOTSTRAP);
         Properties configuration = bootstrap.getConfiguration();
         rootPath = configuration.getProperty("spaces.company_home.childname");
      }
      
      return rootPath;
   }

   /**
    * Returns the repository store URL
    *
    * @param context The spring context
    * @return The repository store URL to use
    */
   private static StoreRef getRepositoryStoreRef(WebApplicationContext context)
   {
      if (repoStoreRef == null)
      {
         ImporterBootstrap bootstrap = (ImporterBootstrap)context.getBean(BEAN_IMPORTER_BOOTSTRAP);
         repoStoreRef = bootstrap.getStoreRef();
      }
      return repoStoreRef;
   }
}
