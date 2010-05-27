package org.hippoecm.hst.test.sitemapitemhandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.SiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;

public abstract class AbstractTestHstSiteMapHandler implements HstSiteMapItemHandler{

    private ServletContext servletContext;
    private SiteMapItemHandlerConfiguration handlerConfig;
    
    public void init(ServletContext servletContext, SiteMapItemHandlerConfiguration handlerConfig) throws HstSiteMapItemHandlerException {
        this.handlerConfig = handlerConfig;
        this.servletContext = servletContext;
    }
    
    /**
     * Override this method when you are implementing your own real HstSiteMapHandler. By default, the AbstractHstSiteMapHandler returns 
     * the <code>resolvedSiteMapItem</code> directly.
     */
    public ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request,
            HttpServletResponse response) throws HstSiteMapItemHandlerException {
        return resolvedSiteMapItem;
    }

    
    /**
     * Override this method when the destroy of this HstSiteMapItemHandler should invoke some processing, for example clear a cache
     */
    public void destroy() throws HstSiteMapItemHandlerException {
    }
    
    public SiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration() {
        return handlerConfig;
    }
    public ServletContext getServletContext() {
        return servletContext;
    }
    
    
    public ResolvedSiteMapItem resolveToNewSiteMapItem(HttpServletRequest request,
            HttpServletResponse response, ResolvedSiteMapItem currentResolvedSiteMapItem, String pathInfo){
        
        HstContainerURL newContainerUrl = createContainerURL(request, response, currentResolvedSiteMapItem, pathInfo);
        return currentResolvedSiteMapItem.getResolvedSiteMount().matchSiteMapItem(newContainerUrl);
    }
    
    public HstContainerURL createContainerURL(HttpServletRequest request, HttpServletResponse response, ResolvedSiteMapItem resolvedSiteMapItem, String pathInfo) {
        HstURLFactory factory = getURLFactory(resolvedSiteMapItem);
        return factory.getContainerURLProvider().parseURL(request, response, null, pathInfo);
    }
    
    public HstURLFactory getURLFactory(ResolvedSiteMapItem resolvedSiteMapItem) {
        HstURLFactory factory = resolvedSiteMapItem.getResolvedSiteMount().getResolvedVirtualHost().getVirtualHost().getVirtualHosts().getVirtualHostsManager().getUrlFactory();
        return factory;
    }

}