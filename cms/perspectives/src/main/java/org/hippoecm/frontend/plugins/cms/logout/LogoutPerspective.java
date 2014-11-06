/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.logout;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;

import static org.hippoecm.frontend.util.WebApplicationHelper.HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME;

public class LogoutPerspective extends Perspective {

    public LogoutPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void focus(final IRenderService child) {
        clearStates();
        logoutSession();
        redirectPage();
    }

    /**
     * Clear any user states other than user session.
     */
    protected void clearStates() {
        // Remove the Hippo Auto Login cookie
        WebApplicationHelper.clearCookie(WebApplicationHelper.getFullyQualifiedCookieName(HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME));
    }

    /**
     * Log out user session.
     */
    protected void logoutSession() {
        UserSession userSession = UserSession.get();

        try {
            Session session = userSession.getJcrSession();

            if (session != null) {
                session.save();
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        userSession.logout();
    }

    /**
     * Redirect it to (home)page
     */
    protected void redirectPage() {
        if (WebApplication.exists()) {
            throw new RestartResponseException(WebApplication.get().getHomePage());
        }
    }

}
