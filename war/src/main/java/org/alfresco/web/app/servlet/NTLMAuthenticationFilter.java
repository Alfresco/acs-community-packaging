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

import java.io.IOException;

import java.io.PrintWriter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.web.auth.WebCredentials;
import org.alfresco.repo.webdav.auth.AuthenticationDriver;
import org.alfresco.repo.webdav.auth.BaseNTLMAuthenticationFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Web-client NTLM Authentication Filter Class
 * 
 * @author GKSpencer
 */
public class NTLMAuthenticationFilter extends BaseNTLMAuthenticationFilter
{
    // Debug logging
    private static Log logger = LogFactory.getLog(NTLMAuthenticationFilter.class);

    
	/* (non-Javadoc)
	 * @see org.alfresco.repo.webdav.auth.BaseNTLMAuthenticationFilter#init()
	 */
	@Override
    protected void init() throws ServletException
    {
        // Call the base NTLM filter initialization
        super.init();
        
        // Use the web client user attribute name
        setUserAttributeName(AuthenticationDriver.AUTHENTICATION_USER);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.webdav.auth.BaseSSOAuthenticationFilter#onValidateFailed(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.servlet.http.HttpSession)
     */
    @Override
    protected void onValidateFailed(ServletContext sc, HttpServletRequest req, HttpServletResponse res, HttpSession session, WebCredentials credentials)
        throws IOException
    {
        super.onValidateFailed(sc, req, res, session, credentials);
        
        // Redirect to the login page if user validation fails
    	redirectToLoginPage(req, res);
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.webdav.auth.BaseNTLMAuthenticationFilter#onLoginComplete(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected boolean onLoginComplete(ServletContext sc, HttpServletRequest req, HttpServletResponse res, boolean userInit)
        throws IOException
    {
        String requestURI = req.getRequestURI();
        return true;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.webdav.auth.BaseSSOAuthenticationFilter#writeLoginPageLink(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void writeLoginPageLink(ServletContext context, HttpServletRequest req, HttpServletResponse resp)
            throws IOException
    {
        String redirectURL = req.getRequestURI();
        resp.setContentType("text/html; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final PrintWriter out = resp.getWriter();
        out.println("<html><head>");
        // Remove the auto refresh to avoid refresh loop, MNT-16931
//         out.println("<meta http-equiv=\"Refresh\" content=\"0; url=" + redirectURL + "\">");
        out.println("</head><body><p>Please <a href=\"" + redirectURL + "\">log in</a>.</p>");
        out.println("</body></html>");
        out.close();
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.webdav.auth.BaseNTLMAuthenticationFilter#getLogger()
     */
    @Override
    final protected Log getLogger()
    {
        return logger;
    }
}
