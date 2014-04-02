/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cmsrest.services;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.cmsrest.AbstractCmsRestTest;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static junit.framework.Assert.assertEquals;

public class DocumentsResourceTest extends AbstractCmsRestTest {
    private HstManager hstManager;
    private Session session;
    private HstURLFactory hstURLFactory;
    private HstLinkCreator linkCreator;
    private HstSiteMapMatcher siteMapMatcher;
    private DocumentsResource documentsResource;
    private String homePageNodeId;
    protected MountDecorator mountDecorator;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponentManager().getComponent(HstManager.class.getName());
        this.linkCreator = getComponentManager().getComponent(HstLinkCreator.class.getName());
        this.siteMapMatcher = getComponentManager().getComponent(HstSiteMapMatcher.class.getName());
        this.hstURLFactory = getComponentManager().getComponent(HstURLFactory.class.getName());
        this.mountDecorator = HstServices.getComponentManager().getComponent(MountDecorator.class.getName());
        this.session = createSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (session != null) {
        session.logout();
        }
    }

    protected void initRequest() throws Exception {
        HstRequestContext requestContext = getRequestFromCms("127.0.0.1:8080", "/_cmsrest");
        documentsResource = new DocumentsResource();
        documentsResource.setHstLinkCreator(linkCreator);
        String homePageDocument = "/unittestcontent/documents/unittestproject/common/homepage";
        Node homePageNode = session.getNode(homePageDocument);
        homePageNodeId = homePageNode.getIdentifier();
        ModifiableRequestContextProvider.set(requestContext);
    }
    @Test
    public void testDocumentResourceLiveUrls() throws Exception {
        initRequest();
        // this homepage document can be exposed by two unittest mounts name (see hst-unittestvirtualhosts.xml):
        /*
         * There we have on hst:hosts the property:
         *
         * hst:channelmanagerhostgroup = dev-localhost
         *
         * and we have
         *
         * dev-localhost
         *     `localhost
         *          ` hst:root (mount with content '/unittestcontent/documents/unittestproject')
         *                ` examplecontextpathonly (mount with content '/unittestcontent/documents/unittestproject' and
         *                                             hst:onlyforcontextpath = /mycontextpath)
         *
         *
         * From above, you can see that the homepage document can have a link for two mounts:
         * 1) hst:root
         * 2) examplecontextpathonly
         *
         * Since the second link has the same contentpath, same number of types, but has a more specific mount (contains
         * more ancestors), we expect examplecontextpathonly as mount to be found for the link
         */

        String url = documentsResource.getUrl(homePageNodeId , "live");
        // NOTE url is site host and NOT cms 127.0.0.1 host!
        assertEquals("http://localhost:8080/mycontextpath/examplecontextpathonly", url);
    }

    @Test
    public void testDocumentResourcePreviewUrls() throws Exception {
        initRequest();
        String url = documentsResource.getUrl(homePageNodeId, "preview");
        assertEquals("/site/preview/custompipeline", url);
        System.out.println(url);
    }

    @Test
    public void testDocumentResourceGetURLIsFullyQualifiedSiteURLsForTestGroupHostGroup() throws Exception {
        String originalValue = "";
        try {
            // first change 'hst:channelmanagerhostgroup' from 'dev-localhost' to 'testgroup'
            // for the rest this test is same as testDocumentResourceGetURLIsFullyQualifiedSiteURLs but now for hostgroup
            // 'testgroup' : Note that the mount with  'custompipeline' now is the best match
            originalValue = session.getNode("/hst:hst/hst:hosts").getProperty("hst:channelmanagerhostgroup").getString();
            session.getNode("/hst:hst/hst:hosts").setProperty("hst:channelmanagerhostgroup", "testgroup");
            session.save();

            initRequest();

            String url = documentsResource.getUrl(homePageNodeId , "live");

            assertEquals("http://www.unit.test:8080/site/custompipeline", url);

        } finally {
            session.getNode("/hst:hst/hst:hosts").setProperty("hst:channelmanagerhostgroup", originalValue);
            session.save();
        }
    }
    

    public HstRequestContext getRequestFromCms(final String hostAndPort,
                                               final String requestURI) throws Exception {
        HstRequestContextComponent rcc = getComponentManager().getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        HstContainerURL containerUrl = createContainerUrlForCmsRequest(requestContext, hostAndPort, requestURI);
        requestContext.setBaseURL(containerUrl);
        requestContext.setResolvedMount(getResolvedMount(containerUrl, requestContext));
        HstURLFactory hstURLFactory = getComponentManager().getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);

        return requestContext;
    }

    public HstContainerURL createContainerUrlForCmsRequest(final HstMutableRequestContext requestContext,
                                                           final String hostAndPort,
                                                           final String requestURI) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();
        requestContext.setServletRequest(request);
        String host = hostAndPort.split(":")[0];
        if (hostAndPort.split(":").length > 1) {
            int port = Integer.parseInt(hostAndPort.split(":")[1]);
            request.setLocalPort(port);
            request.setServerPort(port);
        }
        request.setScheme("http");
        request.setServerName(host);
        request.addHeader("Host", hostAndPort);
        request.setContextPath("/site");
        request.setRequestURI("/site" + requestURI);

        requestContext.setCmsRequest(true);

        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath(), HstRequestUtils.getRequestPath(request));
        return hstURLFactory.getContainerURLProvider().parseURL(request, response, mount);
    }

    public ResolvedMount getResolvedMount(HstContainerURL url, final HstMutableRequestContext requestContext) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = vhosts.matchMount(url.getHostName(), url.getContextPath(), url.getRequestPath());
        requestContext.setAttribute(ContainerConstants.UNDECORATED_MOUNT, resolvedMount.getMount());
        final Mount decorated = mountDecorator.decorateMountAsPreview(resolvedMount.getMount());
        ((MutableResolvedMount) resolvedMount).setMount(decorated);
    return resolvedMount;
    }

}
