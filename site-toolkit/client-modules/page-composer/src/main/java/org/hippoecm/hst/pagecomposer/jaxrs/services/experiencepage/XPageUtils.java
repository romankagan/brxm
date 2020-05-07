/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage;

import java.rmi.RemoteException;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_HOLDER;
import static org.hippoecm.repository.util.JcrUtils.getNodePathQuietly;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.DRAFT;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

public class XPageUtils {

    private static final Logger log = LoggerFactory.getLogger(XPageUtils.class);

    private XPageUtils() {

    }

    protected static DocumentWorkflow getDocumentWorkflow(final HippoSession userSession,
                                                          final PageComposerContextService contextService) throws RepositoryException, WorkflowException {

        // userSession is allowed to read the node since has XPAGE_REQUIRED_PRIVILEGE_NAME on the node
        final Node handle = userSession.getNodeByIdentifier(contextService.getExperiencePageHandleUUID());

        // TODO is 'default' the right document workflow??
        // I think it is ...
        final DocumentWorkflow documentWorkflow = (DocumentWorkflow) userSession.getWorkspace().getWorkflowManager().getWorkflow("default", handle);

        final Node draftNode = WorkflowUtils.getDocumentVariantNode(handle, DRAFT).orElse(null);
        if ((draftNode != null)) {
            final String draftHolder = getStringProperty(draftNode, HIPPOSTD_HOLDER, null);
            final String userId = userSession.getUserID();
            if (!userId.equals(draftHolder)) {
                throw new ClientException("Document being edited by another user", ClientError.ITEM_ALREADY_LOCKED);
            }
        }

        checkoutCorrectBranch(documentWorkflow, contextService);
        return documentWorkflow;
    }


    /**
     * we need to write with the workflowSession. Make sure to use this workflowSession and not impersonate to a
     * workflowSession : This way we can make sure that the workflow manager also persists the changes since the
     * workflow manager will handle the  workflow session save (when we invoke the document workflow
     */
    protected static Session getInternalWorkflowSession(final DocumentWorkflow documentWorkflow) {
        return documentWorkflow.getWorkflowContext().getInternalWorkflowSession();
    }

    protected static void checkoutCorrectBranch(final DocumentWorkflow documentWorkflow,
                                                final PageComposerContextService contextService) throws WorkflowException {
        // TODO checkout the right branch which currently being edited in CM, this might be another one than currently
        // TODO the unpublished is......find current branch via CmsSessionContext

        try {
            if (!Boolean.TRUE.equals(documentWorkflow.hints().get("checkoutBranch"))) {
                // there is only master branch, so no need to check out a branch
                return;
            }
        } catch (RemoteException | RepositoryException e) {
            throw new WorkflowException(e.getMessage());
        }

        final HttpSession httpSession = contextService.getRequestContext().getServletRequest().getSession();
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(httpSession);

        // TODO the CM should have been rendered with UUIDs from version history which should stay the same after
        // TODO restoring a version from history
        documentWorkflow.checkoutBranch(getBranchId(cmsSessionContext));
    }

    protected static String getBranchId(CmsSessionContext cmsSessionContext) {
        return Optional.ofNullable(cmsSessionContext.getContextPayload())
                .map(contextPayload -> contextPayload.get(ContainerConstants.RENDER_BRANCH_ID).toString())
                .orElse(MASTER_BRANCH_ID);
    }

    protected static Node getContainer(final long versionStamp, final Session session,
                                       final PageComposerContextService contextService) throws RepositoryException {

        final Node container = contextService.getRequestConfigNodeById(contextService.getRequestConfigIdentifier(),
                "hst:abstractcomponent", session);

        validateTimestamp(versionStamp, container);
        return container;
    }

    protected static void validateTimestamp(final long versionStamp, final Node container) throws RepositoryException {
        if (versionStamp != 0 && container.hasProperty(GENERAL_PROPERTY_LAST_MODIFIED)) {
            long existingStamp = container.getProperty(GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
            if (existingStamp != versionStamp) {
                String msg = String.format("Node '%s' has been modified wrt versionStamp. Someone else might have " +
                                "made concurrent changes, page must be reloaded. This can happen due to optimistic locking",
                        getNodePathQuietly(container));
                log.info(msg);
                throw new ClientException(msg, ClientError.ITEM_CHANGED);
            }
        }
    }


}
