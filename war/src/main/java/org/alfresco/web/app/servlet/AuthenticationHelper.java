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
package org.alfresco.web.app.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.alfresco.repo.webdav.auth.AuthenticationDriver;
import org.alfresco.web.app.Application;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Helper to authenticate the current user using available Ticket information.
 * <p>
 * User information is looked up in the Session. If found the ticket is retrieved and validated.
 * If the ticket is invalid then a redirect is performed to the login page.
 * <p>
 * If no User info is found then a search will be made for a previous username stored in a Cookie
 * value. If the username if found then a redirect to the Login page will occur. If no username
 * is found then Guest access login will be attempted by the system. Guest access can be forced
 * with the appropriate method call.  
 * 
 * @author Kevin Roast
 */
public final class AuthenticationHelper
{
   /** session variables */
   public static final String AUTHENTICATION_USER = AuthenticationDriver.AUTHENTICATION_USER;
   private static Log logger = LogFactory.getLog(AuthenticationHelper.class);
   
   
   /**
    * Does all the stuff you need to do after successfully authenticating/validating a user ticket to set up the request
    * thread. A useful utility method for an authentication filter.
    * 
    * @param sc
    *           the servlet context
    * @param req
    *           the request
    * @param res
    *           the response
    */
   public static void setupThread(ServletContext sc, HttpServletRequest req, HttpServletResponse res, boolean useInterfaceLanguage)
   {
      if (logger.isDebugEnabled())
          logger.debug("Setting up the request thread.");
      // Set the current locale and language (overriding the one already decoded from the Accept-Language header
      if (WebApplicationContextUtils.getRequiredWebApplicationContext(sc).containsBean(Application.BEAN_CONFIG_SERVICE))
      {
          I18NUtil.setLocale(Application.getLanguage(req.getSession(), Application.getClientConfig(sc).isLanguageSelect() && useInterfaceLanguage));
      }
      else
      {
          Application.getLanguage(req.getSession(), false);
      }
      if (logger.isDebugEnabled())
          logger.debug("The general locale is : " + I18NUtil.getLocale());
   }
}
