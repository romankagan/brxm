/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.util.TraversingItemVisitor;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.util.JcrCompactNodeTypeDefWriter;
import org.hippoecm.repository.util.VersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Release72UpgraderPhase2 implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(Release72UpgraderPhase2.class);
    
    private static final String[][] rules = {
        {"type", "hippo:remodel", "hipposysedit_1_0:remodel"},
        {"field", "hippo:uri", "hipposysedit_1_0:uri"},
        {"type", "hippo:field", "hipposysedit_1_0:field"},
        {"field", "hippo:name", "hipposysedit_1_0:name"},
        {"field", "hippo:path", "hipposysedit_1_0:path"},
        {"field", "hippo:type", "hipposysedit_1_0:type"},
        {"field", "hippo:multiple", "hipposysedit_1_0:multiple"},
        {"field", "hippo:mandatory", "hipposysedit_1_0:mandatory"},
        {"field", "hippo:ordered", "hipposysedit_1_0:ordered"},
        {"field", "hippo:primary", "hipposysedit_1_0:primary"},
        {"type", "hippo:nodetype", "hipposysedit_1_0:nodetype"},
        {"field", "hippo:type", "hipposysedit_1_0:type"},
        {"field", "hippo:supertype", "hipposysedit_1_0:supertype"},
        {"field", "hippo:node", "hipposysedit_1_0:node"},
        {"field", "hippo:mixin", "hipposysedit_1_0:mixin"},
        {"child", "hippo:field", "hipposysedit_1_0:field"},
        {"type", "hippo:templatetype", "hipposysedit_1_0:templatetype"},
        {"type", "hippo:initializeitem", "hippo_2_0:initializeitem"},
        {"type", "hippo:softdocument", "hipposys_1_0:softdocument"},
        {"field", "hippo:uuid", "hipposys_1_0:uuid"},
        {"type", "hippo:request", "hipposys_1_0:request"},
        {"type", "hippo:implementation", "hipposys_1_0:implementation"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:serialver", "hipposys_1_0:serialver"},
        {"type", "hippo:type", "hipposys_1_0:type"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:display", "hipposys_1_0:display"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"type", "hippo:types", "hipposys_1_0:types"},
        {"type", "hippo:workflow", "hipposys_1_0:workflow"},
        {"field", "hippo:privileges", "hipposys_1_0:privileges"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:display", "hipposys_1_0:display"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:workflow", "hipposys_1_0:classname"},
        {"child", "hippo:types", "hipposys_1_0:types"},
        {"child", "hippo:config", "hipposys_1_0:config"},
        {"type", "hippo:workflowcategory", "hipposys_1_0:workflowcategory"},
        {"type", "hippo:workflowfolder", "hipposys_1_0:workflowfolder"},
        {"type", "hippo:ocmquery", "hipposys_1_0:ocmquery"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"child", "hippo:types", "hipposys_1_0:types"},
        {"type", "hippo:ocmqueryfolder", "hipposys_1_0:ocmqueryfolder"},
        {"type", "hippo:queryfolder", "hipposys_1_0:queryfolder"},
        {"type", "hippo:basequeryfolder", "hipposys_1_0:basequeryfolder"},
        {"type", "hippo:propertyreference", "hipposys_1_0:propertyreference"},
        {"type", "hippo:relativepropertyreference", "hipposys_1_0:relativepropertyreference"},
        {"field", "hippo:relPath", "hipposys_1_0:relPath"},
        {"type", "hippo:resolvepropertyreference", "hipposys_1_0:resolvepropertyreference"},
        {"field", "hippo:relPath", "hipposys_1_0:relPath"},
        {"type", "hippo:builtinpropertyreference", "hipposys_1_0:builtinpropertyreference"},
        {"field", "hippo:method", "hipposys_1_0:method"},
        {"type", "hippo:propertyreferences", "hipposys_1_0:propertyreferences"},
        {"type", "hippo:deriveddefinition", "hipposys_1_0:deriveddefinition"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:serialver", "hipposys_1_0:serialver"},
        {"child", "hippo:accessed", "hipposys_1_0:accessed"},
        {"child", "hippo:derived", "hipposys_1_0:derived"},
        {"type", "hippo:derivativesfolder", "hipposys_1_0:derivativesfolder"},
        {"type", "hippo:initializefolder", "hippo_2_0:initializefolder"},
        {"type", "hippo:temporaryfolder", "hipposys_1_0:temporaryfolder"},
        {"type", "hippo:applicationfolder", "hipposys_1_0:applicationfolder"},
        {"type", "hippo:configuration", "hipposys_1_0:configuration"},
        {"type", "hippo:accessmanager", "hipposys_1_0:accessmanager"},
        {"field", "hippo:permissioncachesize", "hipposys_1_0:permissioncachesize"},
        {"type", "hippo:user", "hipposys_1_0:user"},
        {"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"field", "hippo:active", "hipposys_1_0:active"},
        {"field", "hippo:password", "hipposys_1_0:password"},
        {"field", "hippo:passkey", "hipposys_1_0:passkey"},
        {"field", "hippo:lastlogin", "hipposys_1_0:lastlogin"},
        {"type", "hippo:externaluser", "hipposys_1_0:externaluser"},
        {"field", "hippo:lastsync", "hipposys_1_0:lastsync"},
        {"type", "hippo:group", "hipposys_1_0:group"},
        {"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"field", "hippo:members", "hipposys_1_0:members"},
        {"field", "hippo:groups", "hipposys_1_0:groups"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:externalgroup", "hipposys_1_0:externalgroup"},
        {"field", "hippo:syncdate", "hipposys_1_0:syncdate"},
        {"type", "hippo:role", "hipposys_1_0:role"},
        {"field", "hippo:privileges", "hipposys_1_0:privileges"},
        {"field", "hippo:roles", "hipposys_1_0:roles"},
        {"field", "hippo:jcrread", "hipposys_1_0:jcrread"},
        {"field", "hippo:jcrwrite", "hipposys_1_0:jcrwrite"},
        {"field", "hippo:jcrremove", "hipposys_1_0:jcrremove"},
        {"type", "hippo:externalrole", "hipposys_1_0:externalrole"},
        {"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"type", "hippo:authrole", "hipposys_1_0:authrole"},
        {"field", "hippo:users", "hipposys_1_0:users"},
        {"field", "hippo:groups", "hipposys_1_0:groups"},
        {"field", "hippo:role", "hipposys_1_0:role"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:facetrule", "hipposys_1_0:facetrule"},
        {"field", "hippo:facet", "hipposys_1_0:facet"},
        {"field", "hippo:value", "hipposys_1_0:value"},
        {"field", "hippo:type", "hipposys_1_0:type"},
        {"field", "hippo:equals", "hipposys_1_0:equals"},
        {"field", "hippo:filter", "hipposys_1_0:filter"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:domainrule", "hipposys_1_0:domainrule"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:domain", "hipposys_1_0:domain"},
        {"field", "hippo:description", "hipposys_1_0:description"},
        {"type", "hippo:userprovider", "hipposys_1_0:userprovider"},
        {"field", "hippo:dirlevels", "hipposys_1_0:dirlevels"},
        {"type", "hippo:groupprovider", "hipposys_1_0:groupprovider"},
        {"field", "hippo:dirlevels", "hipposys_1_0:dirlevels"},
        {"type", "hippo:roleprovider", "hipposys_1_0:roleprovider"},
        {"type", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"child", "hippo:userprovider", "hipposys_1_0:userprovider"},
        {"child", "hippo:groupprovider", "hipposys_1_0:groupprovider"},
        {"child", "hippo:roleprovider", "hipposys_1_0:roleprovider"},
        {"type", "hippo:userfolder", "hipposys_1_0:userfolder"},
        {"type", "hippo:groupfolder", "hipposys_1_0:groupfolder"},
        {"type", "hippo:rolefolder", "hipposys_1_0:rolefolder"},
        {"type", "hippo:domainfolder", "hipposys_1_0:domainfolder"},
        {"type", "hippo:securityfolder", "hipposys_1_0:securityfolder"},
        {"field", "hippo:userspath", "hipposys_1_0:userspath"},
        {"field", "hippo:groupspath", "hipposys_1_0:groupspath"},
        {"field", "hippo:rolespath", "hipposys_1_0:rolespath"},
        {"field", "hippo:domainspath", "hipposys_1_0:domainspath"},
        {"child", "hippo:accessmanager", "hipposys_1_0:accessmanager"},
        {"type", "hippo:namespace", "hipposysedit_1_0:namespace"},
        {"type", "hippo:namespacefolder", "hipposysedit_1_0:namespacefolder"},
        {"type", "hippo:resource", "hippo_2_0:resource"},
        {"type", "hippo:query", "hippo_2_0:query"},
        {"type", "hippo:derived", "hippo_2_0:derived"},
        {"type", "hippo:document", "hippo_2_0:document"},
        {"type", "hippo:handle", "hippo_2_0:handle"},
        {"type", "hippo:hardhandle", "hippo_2_0:hardhandle"},
        {"type", "hippo:harddocument", "hippo_2_0:harddocument"},
        {"type", "hippo:facetresult", "hippo_2_0:facetresult"},
        {"type", "hippo:facetbasesearch", "hippo_2_0:facetbasesearch"},
        {"type", "hippo:facetsearch", "hippo_2_0:facetsearch"},
        {"type", "hippo:facetselect", "hippo_2_0:facetselect"},
        {"type", "hippo:mirror", "hippo_2_0:mirror"},
        {"type", "hippo:facetlink", null},
        //{"type","hippo:translation","hipponew:translation"},
        //{"type","hippo:translated","hipponew:translated"},

        {"type", "frontend:workflow", "frontend_2_0:workflow"},
        {"field", "hippo:nodetype", "hipposys_1_0:nodetype"},
        {"field", "hippo:display", "hipposys_1_0:display"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
        {"field", "hippo:workflow", "hipposys_1_0:classname"},
        {"field", "hippo:privileges", "hipposys_1_0:privileges"},
        {"child", "hippo:types", "hipposys_1_0:types"},
        {"child", "hippo:config", "hipposys_1_0:config"},
        //{"type", "frontend:user", "frontend:user2"},
        //{"field", "hippo:securityprovider", "hipposys_1_0:securityprovider"},
        //{"field", "hippo:active", "hipposys_1_0:active"},
        //{"field", "hippo:password", "hipposys_1_0:password"},
        //{"field", "hippo:passkey", "hipposys_1_0:passkey"},
        //{"field", "hippo:lastlogin", "hipposys_1_0:lastlogin"},

        {"type", "hippo:implementation", "hipposys_1_0:implementation"},
        {"field", "hippo:classname", "hipposys_1_0:classname"},
    };

    static String[][] renames = {
        { "org.hippoecm.repository.standardworkflow.EditmodelWorkflowImpl",
          "org.hippoecm.editor.repository.impl.EditmodelWorkflowImpl" },
        { "org.hippoecm.frontend.plugins.standardworkflow.EditmodelWorkflowPlugin",
          "org.hippoecm.frontend.editor.workflow.EditmodelWorkflowPlugin" },
    };

    private static String getNewClass(String oldName) {
        for (String[] rename : renames) {
            if (rename[0].equals(oldName)) {
                return rename[1];
            }
        }
        return oldName;
    }
    
    public void register(final UpdaterContext context) {
        context.registerName("upgrade");
        context.registerStartTag("v20902-phase1");
        context.registerEndTag("v20902-phase2");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("rep:root") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                /*
                 * The removal of the entire /hippo:log tree seems to be appropriate.  This is relatively volatile data as
                 * this is a sliding log file with the oldest entries being removed automatically.  Combine this with the
                 * fact that old entries might not contain the same information and the effort of converting data which is
                 * going to be removed quickly is unnecessary.
                 */
                if (node.hasNode("hippo:log")) {
                    for(NodeIterator iter=node.getNode("hippo:log").getNodes(); iter.hasNext(); ) {
                        iter.nextNode().remove();
                    }
                }
            }
        }.setAtomic());
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:templatetype") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                // Should a new nodetype descriptor be created?
                boolean convert = false;
                String prefix = node.getParent().getName();
                String uri = null;
                String newUri = null;
                if (node.getDepth() > 0 && node.getParent().isNodeType("hippo:namespace")
                        && !"system".equals(prefix)) {
                    try {
                        uri = node.getSession().getNamespaceURI(prefix);
                        VersionNumber version = new VersionNumber(uri.substring(uri.lastIndexOf("/") + 1));
                        newUri = uri.substring(0, uri.lastIndexOf('/') + 1) + version.next().toString();
                        convert = true;
                        try {
                            node.getSession().getNamespacePrefix(newUri);
                        } catch (NamespaceException ex) {
                            convert = false;
                        }
                    } catch (NamespaceException ex) {
                        log.warn("Unknown namespace prefix " + prefix);
                    }
                }

                context.setPrimaryNodeType(node, "hipposysedit_1_0:templatetype");
                Node child = node.getNode("hippo:nodetype");
                Node current = null;
                for (NodeIterator nodetypeVersionIter = child.getNodes(child.getName()); nodetypeVersionIter.hasNext();) {
                    Node version = nodetypeVersionIter.nextNode();
                    context.setName(version, "hipposysedit_1_0:nodetype");
                    if (convert && version.isNodeType("hippo:remodel")) {
                        if (uri != null && uri.equals(version.getProperty("hippo:uri").getString())) {
                            current = version;
                        }
                    }
                    version.accept(new TraversingItemVisitor.Default(true) {
                        @Override
                        protected void entering(final Node node, int level) throws RepositoryException {
                            convert(node, context);
                        }
                    });
                }
                context.setName(child, "hipposysedit_1_0:nodetype");
                context.setPrimaryNodeType(child, "hippo_2_0:handle");
                if (current != null) {
                    Node clone = ((HippoSession) child.getSession()).copy(current, current.getPath());
                    clone.setProperty("hipposysedit_1_0:uri", newUri);
                }

                if (node.hasNode("hippo:prototype")) {
                    child = node.getNode("hippo:prototype");
                    for (NodeIterator prototypeIter = child.getNodes("hippo:prototype"); prototypeIter.hasNext();) {
                        Node prototype = prototypeIter.nextNode();
                        context.setName(prototype, "hipposysedit_1_0:prototype");
                        prototype.accept(new TraversingItemVisitor.Default(true) {
                            @Override
                            protected void entering(final Node node, int level) throws RepositoryException {
                                convert(node, context);
                            }
                        });
                    }
                    context.setName(child, "hipposysedit_1_0:prototypes");
                    context.setPrimaryNodeType(child, "hipposysedit_1_0:prototypeset");
                }

                if (node.hasNode("hippo:template")) {
                    node.addMixin("editor_1_0:editable");
                    child = node.getNode("hippo:template");
                    for (NodeIterator templateIter = child.getNodes(child.getName()); templateIter.hasNext();) {
                        Node template = templateIter.nextNode();
                        context.setName(template, "hipposysedit_1_0:template");
                        template.accept(new TraversingItemVisitor.Default(true) {
                            @Override
                            protected void entering(final Node node, int level) throws RepositoryException {
                                convert(node, context);
                            }
                        });
                    }
                    context.setName(child, "editor_1_0:templates");
                    context.setPrimaryNodeType(child, "editor_1_0:templateset");
                }
            }
        }.setAtomic());
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:namespacefolder") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                context.setPrimaryNodeType(node, "hipposysedit_1_0:namespacefolder");
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.isNodeType("hippo:namespace")) {
                        context.setPrimaryNodeType(child, "hipposysedit_1_0:namespace");
                    }
                }
            }
        }.setAtomic());

        /**
         * reviewed-actions workflow update
         */
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:workflowcategory") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.getName().equals("versioning")) {
                    if (node.hasNode("version")) {
                        Node version = node.getNode("version");
                        if (version.getProperty("hippo:classname").getString().equals("org.hippoecm.repository.api.Document")) {
                            version.setProperty("hippo:classname",
                                    "org.hippoecm.repository.standardworkflow.VersionWorkflowImpl");
                        }
                        version.setProperty("hippo:privileges", new String[] { "hippo:author" });
                        context.setPrimaryNodeType(version, "frontend:workflow");
                        Node renderer = version.addNode("frontend:renderer", "frontend:plugin");
                        renderer.setProperty("plugin.class",
                                "org.hippoecm.frontend.plugins.standardworkflow.NullWorkflowPlugin");
                    }
                    if (node.hasNode("revert")) {
                        Node restore = node.getNode("revert");
                        context.setName(restore, "restore");
                        if (restore.getProperty("hippo:classname").getString().equals("org.hippoecm.repository.api.Document")) {
                            restore.setProperty("hippo:classname",
                                    "org.hippoecm.repository.standardworkflow.VersionWorkflowImpl");
                        }
                        context.setPrimaryNodeType(restore, "frontend:workflow");
                        restore.setProperty("hippo:privileges", new String[] { "hippo:editor" });
                        Node renderer = restore.addNode("frontend:renderer", "frontend:plugin");
                        renderer.setProperty("plugin.class",
                                "org.hippoecm.frontend.plugins.reviewedactions.VersionWorkflowPlugin");
                        renderer.setProperty("editor.id", "${editor.id}");
                    }
                }
            }
        }.setAtomic());
        /**
         * add browser.id, editor.id to all frontend workflow plugins.
         * This doesn't harm plugins that don't use these services, but it might be better to
         * be a bit more fine-grained about this.
         */
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("frontend:workflow") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                // convert property to child node
                if (node.hasProperty("frontend:renderer")) {
                    if (!node.hasNode("frontend:renderer")) {
                        Node child = node.addNode("frontend_2_0:renderer", "frontend_2_0:plugin");
                        child.setProperty("plugin.class", node.getProperty("frontend:renderer").getString());
                        child.setProperty("model.id", "${model.id}");
                        child.setProperty("browser.id", "${browser.id}");
                        child.setProperty("editor.id", "${editor.id}");
                        node.setProperty("frontend_2_0:renderer", (Value) null);
                    } else {
                        log.error("Unable to convert deprecated property frontend:renderer to child node");
                    }
                }
                Node renderer = node.getNode("frontend:renderer");
                if (!renderer.hasProperty("validator.id")) {
                    renderer.setProperty("validator.id", "${validator.id}");
                }
                if (!renderer.hasProperty("feedback.id")) {
                    renderer.setProperty("feedback.id", "${feedback.id}");
                }
                String oldClassName = renderer.getProperty("plugin.class").getString();
                String className = getNewClass(oldClassName);
                if (!className.equals(oldClassName)) {
                    renderer.setProperty("plugin.class", className);
                }
            }
        }.setAtomic());
        /**
         * upgrade domain rules
         */
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:domain") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                String name = node.getName();
                if ("defaultwrite".equals(name)) {
                    if (node.hasNode("hippo-handle")) {
                        Node hippoHandle = node.getNode("hippo-handle");
                        hippoHandle.getNode("type-hippo-handle").setProperty("hippo:facet", "nodetype");
                    }
                } else if ("versioning".equals(name)) {
                    Node facetRule;

                    if (!node.hasNode("nt-system")) {
                        Node ntSystem = node.addNode("nt-system", "hippo:domainrule");
                        facetRule = ntSystem.addNode("type-rep-system", "hippo:facetrule");
                        facetRule.setProperty("hippo:equals", true);
                        facetRule.setProperty("hippo:facet", "jcr:primaryType");
                        facetRule.setProperty("hippo:filter", false);
                        facetRule.setProperty("hippo:type", "Name");
                        facetRule.setProperty("hippo:value", "rep:system");
                    }

                    if (!node.hasNode("nt-versionStorage")) {
                        Node ntVersionStorage = node.addNode("nt-versionStorage", "hippo:domainrule");
                        facetRule = ntVersionStorage.addNode("type-rep-versionStorage", "hippo:facetrule");
                        facetRule.setProperty("hippo:equals", true);
                        facetRule.setProperty("hippo:facet", "jcr:primaryType");
                        facetRule.setProperty("hippo:filter", false);
                        facetRule.setProperty("hippo:type", "Name");
                        facetRule.setProperty("hippo:value", "rep:versionStorage");
                    }

                    Node authRole = node.addNode("hippo:authrole", "hippo:authrole");
                    authRole.setProperty("hippo:role", "editor" );
                    authRole.setProperty("hippo:groups", new String[] { "editor" });
                } else if ("templates".equals(name)) {
                    Node facetRule;
                    if (!node.hasNode("type-hippo-prototypes")) {
                        Node ntPrototypes = node.addNode("type-hippo-prototypes", "hippo:domainrule");
                        facetRule = ntPrototypes.addNode("nodetype-hippo-prototypeset", "hippo:facetrule");
                        facetRule.setProperty("hippo:equals", true);
                        facetRule.setProperty("hippo:facet", "jcr:primaryType");
                        facetRule.setProperty("hippo:filter", false);
                        facetRule.setProperty("hippo:type", "Name");
                        facetRule.setProperty("hippo:value", "hipposysedit:prototypeset");
                    }

                    Node ntPrototype = node.getNode("hippo-prototype");
                    context.setName(ntPrototype, "type-hippo-prototype");
                    facetRule = ntPrototype.getNode("nodetype-hippo-prototype");
                    facetRule.setProperty("hippo:value", "hipposysedit:prototype");
                }
            }
        }.setAtomic());
        /**
         * add hipposys:system to workflowuser
         */
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:userfolder") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                if (node.hasNode("workflowuser")) {
                    node.getNode("workflowuser").setProperty("hipposys_1_0:system", true);
                }
            }
        }.setAtomic());
        /**
         * frontend:user => hipposys:user
         */
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("frontend:user") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                context.setPrimaryNodeType(node, "hippo:user");
                node.setProperty("frontend:firstname", (String) null);
                node.setProperty("frontend:lastname", (String) null);
                node.setProperty("frontend:email", (String) null);
            }
        }.setAtomic());
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:configuration") {
            @Override
            protected void leaving(final Node node, int level) throws RepositoryException {
                for (String[] delete : new String[][] {
                            {"hippo:temporary"}, // this removal is appropriate, any changes to this folder should be considered transient
                            {"hippo:frontend"} // tracking changes in cms configuration deemed too expensive
                        }) {
                    for (NodeIterator it = node.getNode(delete[0]).getNodes(); it.hasNext();) {
                        Node child = it.nextNode();
                        boolean keep = false;
                        for (int i = 1; i < delete.length; i++) {
                            if (child.getName().equals(delete[i]))
                                keep = true;
                        }
                        if (!keep)
                            child.remove();
                    }
                }
                // see comment on removal of hippostd-queries in hippo:initialize
                node.getNode("hippo:queries").getNode("hippo:templates").remove();
                for (String delete : new String[] {
                            /* Preserving changes from this item is a bridge too far, but it does include some
                             * important customization by users so must be clearly documented. */
                            "hippostd-queries",

                            "namespaces", // FIXME: comment on the appropriateness of removal or decide on not remove but convert
                            "templateeditor-hipposysedit", // FIXME: comment on the appropriateness of removal or decide on not remove but convert
                            "templateeditor-namespace.xml", // FIXME: comment on the appropriateness of removal or decide on not remove but convert
                            "templateeditor-type-query.xml", // FIXME: comment on the appropriateness of removal or decide on not remove but convert
                            "hipposched", // these items are never customized
                            "hippoldap", // it is rare to customize these items, a re-import is safe

                            /**
                             * these namespace declarations, type definitions are explicitly handled by the next visitor 
                             */
                            /*
                            "hippostd-date",
                            "hippostd",
                            "hippohtmlcleaner",
                            "hippolog",
                            "frontend",
                            "editor",
                            "reporting",
                            */

                            /**
                             * don't reload content
                             */
                            /* 
                             "content",
                             */

                            /**
                             * gallery: re-import templates, gallery workflow
                             */
                            "hippogallery",
                            "hippogallery-files",
                            "hippogallery-images",
                            "hippogallery-image",

                            /**
                             * Templates from the "system", hippo and hippostd namespaces have to be re-initialized
                             * as they are removed.
                             */
                            "hippostd-html-template",
                            "system-html-template",
                            "hippogallery-editor",
                            "hippostd-types",
                            "templateeditor-faceteddate",
                            "templateeditor-system",
                            "templateeditor-hippo",
                            "frontend-types",

                            /**
                             * users: type conversion is sufficient
                             */
                            /*
                            "user-editor",
                            "user-author",
                            */

                            /**
                             * groups: type conversion is sufficient
                             */
                            /*
                            "group-editor",
                            "group-author",
                            "group-everybody",
                            */

                            /**
                             * roles: type conversion is sufficient
                             */
                            /*
                            "role-jcrread",
                            "role-jcrwrite",
                            "role-editor",
                            "role-author",
                            */

                            /**
                             * authorization rules: conversion suffices
                             */
                            /*
                            "domain-defaultread",
                            "domain-defaultwrite",
                            "domain-versioning",
                            "domain-workflow",
                            "domain-hippodocuments",
                            "domain-hippofolders",
                            "domain-frontendconfig",
                            "domain-hippogallery",
                            "domain-htmlcleaner",
                            "domain-templates",
                            "domain-hippolog",
                            "domain-hipporequests",
                            "domain-templates-templateset",
                            */

                            /**
                             * The reviewed-actions addon should ideally take care of upgrading its content.
                             * At the moment, the imports below combine workflows from different projects.
                             * The upgrade is carried out by the frontend:workflow and workflowcategory iterators. 
                             */
                            /*repository/modules/src/main/java/org/hippoecm/repository/upgrade/Release72Updater.java
                            "reviewedactions1",
                            "core-workflows",
                            "reviewedactions2",
                            "versioning",
                            */
                            
                            /**
                             * The workflows that ship with the frontend/repository engines + editor.
                             */
                            "editor-workflows",
                            "hippostd-workflows",
                            "embedded-workflows",
                            "hippostd-workflows2",
                            "shortcuts-workflows",

                            /**
                             * These initialisation nodes correspond to the /hippo:configuration/hippo:frontend tree.
                             * The configuration changes are too big to merge by writing JCR calls.
                             */
                            "frontend-console",
                            "html-cleaner-service",
                            "cms",
                            "cms-login",
                            "cms-static",
                            "cms-editor",
                            "cms-preview",
                            "cms-headshortcuts",
                            "cms-dashshortcuts",
                            "cms-dashshortcuts-changepassword",
                            "cms-dashboard",
                            "cms-reports",
                            "cms-browser",
                            "cms-folder-views",
                            "cms-tree-views",
                            "cms-pickers",
                            "cms-services",
                            "cms-dashshortcuts-gotolink",
                            "reviewedactions3",
                            "reviewedactions4",
                            "layout-provider",
                            }) {
                    if (node.getNode("hippo:initialize").hasNode(delete)) {
                        node.getNode("hippo:initialize").getNode(delete).remove();
                    }
                }
                node.accept(new TraversingItemVisitor.Default(true) {
                    @Override
                    protected void entering(final Node node, int level) throws RepositoryException {
                        convert(node, context);
                    }
                });
            }
        }.setAtomic());
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("rep:root") {
            @Override
            public void leaving(final Node node, int level) throws RepositoryException {
                if (node.hasNode("hippo:namespaces")) {
                    Node nsFolderNode = node.getNode("hippo:namespaces");
                    String[][] remove = {
                            {"hippo", "templatetype", "namespace", "field", "nodetype", "remodel", "query", "resource", "facetlink", "facetselect", "facetsearch", "facetsubsearch", "document" },
                            {"system", "String", "Text", "Boolean", "Date", "Long", "Double", "Binary", "Docbase", "Link", "CssSize", "Password", "Html"},
                            {"hippostd", "languagable", "publishable", "publishableSummary", "taggable", "folder", "directory", "html"},
                            {"hippogallery", "asset", "image", "exampleImageSet", "exampleAssetSet"},
                    };
                    for (String[] namespace : remove) {
                        if (nsFolderNode.hasNode(namespace[0])) {
                            Node nsNode = nsFolderNode.getNode(namespace[0]);
                            for (int i = 1; i < namespace.length; i++) {
                                if (nsNode.hasNode(namespace[i])) {
                                    nsNode.getNode(namespace[i]).remove();
                                }
                            }
                        }
                    }
                }

                // recreate workflow nodes
                Node workflowCategories = node.getNode("hippo:configuration/hippo:workflows");
                for (String category : new String[] { "internal", "embedded", "threepane", "shortcuts", "editor" }) {
                    if (workflowCategories.hasNode(category)) {
                        workflowCategories.getNode(category).remove();
                    }
                }
            }
        }.setAtomic());
        for (String[] nodeTypeDefinitions : new String[][] {
                    {"hipposys"},
                    {"hipposysedit"},
                    {"hippo", "repository.cnd", "org.hippoecm.repository.LocalHippoRepository" },
                    {"hippostd"},
                    {"hippostd", "hippostd-addendum.cnd"},
                    {"hippogallery"},
                    {"frontend"},
                    {"hippolog"},
                    {"reporting"},
                    {"hippohtmlcleaner"},
                    {"editor"}}) {
            try {
                String prefix = nodeTypeDefinitions[0];
                String cndName = (nodeTypeDefinitions.length > 1 && nodeTypeDefinitions[1] != null ? nodeTypeDefinitions[1] : prefix + ".cnd");
                String classContext = (nodeTypeDefinitions.length > 2 && nodeTypeDefinitions[2] != null ? nodeTypeDefinitions[2] : null);
                InputStream cndStream;
                if (classContext != null) {
                    cndStream = Class.forName(classContext).getResourceAsStream(cndName);
                } else {
                    cndStream = getClass().getClassLoader().getResourceAsStream(cndName);
                }
                if(cndName.equals("hippogallery"))
                    cndName = "-";
                if (cndStream != null) {
                    context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, prefix, cndName, new InputStreamReader(cndStream)));
                }
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace(System.err);
            }
        }

        try {
            Workspace workspace = context.getWorkspace();
            for(String subtypedNamespace : subTypedNamespaces(workspace)) {
                String uri = workspace.getNamespaceRegistry().getURI(subtypedNamespace);
                workspace.getNamespaceRegistry().registerNamespace(subtypedNamespace, VersionNumber.versionFromURI(uri).next().versionToURI(uri));
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, subtypedNamespace, "-", null));
            }
        } catch (NamespaceException ex) {
            ex.printStackTrace(System.err);
        } catch (RepositoryException ex) {
            ex.printStackTrace(System.err);
        }
    }

    private Collection<String> subTypedNamespaces(Workspace workspace) throws RepositoryException {
        Set<String> knownNamespaces = new HashSet<String>();
        LinkedList<String> subtypedNamespaces = new LinkedList<String>();
        Set<String> skippedNamespaces = new HashSet<String>();
        knownNamespaces.add("hippo");
        skippedNamespaces.add("hipposys");
        skippedNamespaces.add("hipposysedit");
        skippedNamespaces.add("hippostd");
        skippedNamespaces.add("hippogallery");
        skippedNamespaces.add("frontend");
        skippedNamespaces.add("hippolog");
        skippedNamespaces.add("reporting");
        skippedNamespaces.add("hippohtmlcleaner");
        skippedNamespaces.add("editor");
        skippedNamespaces.add("hipposched");
        skippedNamespaces.add("hippoldap");
        skippedNamespaces.addAll(knownNamespaces);
        NodeTypeManager ntMgr = workspace.getNodeTypeManager();
        boolean rerun;
        do {
            rerun = false;
            for (NodeTypeIterator ntiter = ntMgr.getAllNodeTypes(); ntiter.hasNext();) {
                NodeType nt = ntiter.nextNodeType();
                String ntName = nt.getName();
                if (ntName.contains(":")) {
                    String ntNamespace = ntName.substring(0, ntName.indexOf(":"));
                    // if the namespace (x) is not known, but one of the supertypes of the type is in a
                    // known namespace (y) then add the namespace (x) to the list of known namespaces and restart.
                    if (!knownNamespaces.contains(ntNamespace)) {
                        Set<NodeType> dependencies = new HashSet<NodeType>();
                        for (NodeType superType : nt.getSupertypes())
                            dependencies.add(superType);
                        for (NodeDefinition childDef : nt.getDeclaredChildNodeDefinitions()) {
                            if(childDef.getDefaultPrimaryType() != null)
                                dependencies.add(childDef.getDefaultPrimaryType());
                            for(NodeType childNodeType : childDef.getRequiredPrimaryTypes())
                                dependencies.add(childNodeType);
                        }
                        for (NodeType superType : dependencies) {
                            String superName = superType.getName();
                            if (superName.contains(":")) {
                                String superNamespace = superName.substring(0, superName.indexOf(":"));
                                if (knownNamespaces.contains(superNamespace)) {
                                    knownNamespaces.add(ntNamespace);
                                    if (!skippedNamespaces.contains(ntNamespace)) {
                                        subtypedNamespaces.addFirst(ntNamespace);
                                    }
                                    rerun = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (rerun)
                    break;
            }
        } while (rerun);
        return subtypedNamespaces;
    }

    private void convert(Node node, UpdaterContext context) throws RepositoryException {
        for (int i = 0; i < rules.length; i++) {
            String[] rule = rules[i];
            if ("type".equals(rule[0])) {
                boolean typeMatch = false;
                int typeMatchIndex = 0;
                for (NodeType nodeType : context.getNodeTypes(node)) {
                    if (nodeType.getName().equals(rule[1])) {
                        typeMatch = true;
                        if (typeMatchIndex == 0) {
                            if (rule[2] != null) {
                                context.setPrimaryNodeType(node, rule[2]);
                            } else {
                                node.remove();
                                return;
                            }
                        } else {
                            node.removeMixin(rule[1]);
                            node.addMixin(rule[2]);
                        }
                        break;
                    }
                    ++typeMatchIndex;
                }
                if (typeMatch) {
                    int j;
                    for (j = i + 1; j < rules.length; j++) {
                        rule = rules[j];
                        if ("field".equals(rule[0])) {
                            if (node.hasProperty(rule[1])) {
                                Property property = node.getProperty(rule[1]);
                                if (rule[2] != null) {
                                    context.setName(property, rule[2]);
                                    if (!context.isMultiple(property)) {
                                        // TODO: whenever property is a Name (or Path), see if there is a prefix
                                        // in there that we can remap.
                                        if (rule[2].equals("hipposys_1_0:value") || rule[2].equals("hipposys_1_0:nodetype")) {
                                            String value = property.getString();
                                            for (int k = 0; k < rules.length; k++) {
                                                if (rules[k][0].equals("type") && rules[k][1].equals(value)) {
                                                    value = rules[k][2];
                                                    // hipposys:value is a String property, so the prefix will not be remapped
                                                    String prefix = value.substring(0, value.indexOf('_'));
                                                    value = prefix + value.substring(value.indexOf(':'));
                                                    break;
                                                }
                                            }
                                            property.setValue(value);
                                        } else if (rule[2].equals("hipposys_1_0:classname")) {
                                            String value = property.getString();
                                            property.setValue(getNewClass(value));
                                        }
                                    }
                                } else {
                                    property.remove();
                                }
                            }
                        } else if ("child".equals(rule[0])) {
                            for (NodeIterator iter = node.getNodes(rule[1]); iter.hasNext();) {
                                Node child = iter.nextNode();
                                if (rule[2] != null) {
                                    context.setName(child, rule[2]);
                                    if (rule.length > 3) {
                                        context.setPrimaryNodeType(child, rule[3]);
                                    }
                                } else {
                                    child.remove();
                                }
                            }
                        } else {
                            break;
                        }
                    }
                    i = j - 1;
                } else {
                    int j;
                    for (j = i + 1; j < rules.length; j++) {
                        rule = rules[j];
                        if (!"field".equals(rule[0]) && !"child".equals(rule[0])) {
                            break;
                        }
                    }
                    i = j - 1;
                }
            } else if ("field".equals(rule[0])) {
                throw new RepositoryException("bad rule on " + i + " " + rule[0] + " " + rule[1] + " " + rule[2]);
            } else if ("child".equals(rule[0])) {
                throw new RepositoryException("bad rule on " + i + " " + rule[0] + " " + rule[1] + " " + rule[2]);
            } else {
                throw new RepositoryException("bad rule on " + i + " " + rule[0] + " " + rule[1] + " " + rule[2]);
            }
        }
    }

}
