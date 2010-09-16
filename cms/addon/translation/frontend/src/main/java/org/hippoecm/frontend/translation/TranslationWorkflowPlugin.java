/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.translation;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.translation.TranslationWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TranslationWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static String COULD_NOT_CREATE_FOLDERS = "could-not-create-folders";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(TranslationWorkflowPlugin.class);

    private AbstractView<HippoLocale> rv;

    public boolean hasLocale(String locale) {
        WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel();
        if (wdm != null) {
            Node node;
            try {
                node = wdm.getNode();
                if (node != null) {
                    Node translations = node.getNode(HippoTranslationNodeType.TRANSLATIONS);
                    return translations.hasNode(locale);
                }
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return false;
    }

    public TranslationWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final IModel<String> languageModel = new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this
                        .getDefaultModel();
                if (wdm != null) {
                    WorkflowDescriptor descriptor = (WorkflowDescriptor) wdm.getObject();
                    WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
                    try {
                        TranslationWorkflow translationWorkflow = (TranslationWorkflow) manager.getWorkflow(descriptor);
                        return (String) translationWorkflow.hints().get("locale");
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage(), ex);
                    } catch (RemoteException ex) {
                        log.error(ex.getMessage(), ex);
                    } catch (WorkflowException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
                return "unknown";
            }
        };
        ILocaleProvider localeProvider = getLocaleProvider();
        rv = new AbstractView<HippoLocale>("languages", new ListDataProvider<HippoLocale>(
                (List<HippoLocale>) localeProvider.getLocales())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<HippoLocale> item) {
                final HippoLocale locale = item.getModelObject();
                String currentLocaleName = languageModel.getObject();
                final String language = locale.getName();
                if (!hasLocale(language)) {
                    item.add(new WorkflowAction("language", locale.getDisplayName(getLocale()), locale.getIcon(
                            IconSize.TINY, LocaleState.AVAILABLE)) {
                        private static final long serialVersionUID = 1L;

                        public String name;
                        public String url;

                        public List<FolderTranslation> folders;

                        @Override
                        protected Dialog createRequestDialog() {
                            try {
                                Node node = ((WorkflowDescriptorModel) TranslationWorkflowPlugin.this.getDefaultModel())
                                        .getNode();
                                url = node.getName();
                                name = url;
                                if (node instanceof HippoNode) {
                                    name = ((HippoNode) node).getLocalizedName();
                                }
                                folders = new LinkedList<FolderTranslation>();
                                Node folder = node.getParent();
                                boolean mutable = true;
                                // FIXME: OUCH!
                                while (!"/content/documents".equals(folder.getPath())) {
                                    if (folder.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                                        Node links = folder.getNode(HippoTranslationNodeType.TRANSLATIONS);
                                        FolderTranslation ft;
                                        if (links.hasNode(language)) {
                                            mutable = false;
                                            HippoNode translation = (HippoNode) links.getNode(language);
                                            ft = new FolderTranslation(translation.getCanonicalNode(), language);
                                        } else {
                                            ft = new FolderTranslation(folder, language);
                                        }
                                        ft.setMutable(mutable);
                                        folders.add(ft);
                                    }
                                    folder = folder.getParent();
                                }
                                Collections.reverse(folders);

                                return new TranslateDocumentDialog(
                                        TranslationWorkflowPlugin.this,
                                        this,
                                        new StringResourceModel("translate-title", TranslationWorkflowPlugin.this, null),
                                        folders);
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                                error(e.getMessage());
                            }
                            return null;
                        }

                        @Override
                        protected String execute(Workflow wf) throws Exception {
                            for (FolderTranslation folder : folders) {
                                if (!folder.isMutable()) {
                                    continue;
                                }
                                if (!folder.persist()) {
                                    return COULD_NOT_CREATE_FOLDERS;
                                }
                            }

                            TranslationWorkflow workflow = (TranslationWorkflow) wf;
                            Document translation = workflow.addTranslation(language, url);
                            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
                            WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager
                                    .getWorkflow("core", translation);
                            if (name != null && !url.equals(name)) {
                                defaultWorkflow.localizeName(name);
                            }
                            return null;
                        }
                    });
                } else {
                    final boolean currentLocale = currentLocaleName != null ? currentLocaleName
                            .equals(locale.getName()) : false;
                    item.add(new WorkflowAction("language", locale.getDisplayName(getLocale()), locale.getIcon(
                            IconSize.TINY, LocaleState.EXISTS)) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected String execute(Workflow wf) throws Exception {
                            IBrowseService<JcrNodeModel> browser = getBrowserService();
                            if (browser != null) {
                                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) TranslationWorkflowPlugin.this
                                        .getDefaultModel();
                                if (wdm != null) {
                                    Node node;
                                    try {
                                        node = wdm.getNode();
                                        if (node != null) {
                                            Node translations = node.getNode(HippoTranslationNodeType.TRANSLATIONS);
                                            HippoNode translation = (HippoNode) translations.getNode(language);
                                            browser
                                                    .browse(new JcrNodeModel(translation.getCanonicalNode().getParent()));
                                        } else {
                                            log.error("No node found for document");
                                        }
                                    } catch (RepositoryException e) {
                                        log.error("Error retrieving translation node", e);
                                    }
                                } else {
                                    log.error("No workflow descriptor model for document");
                                }
                            } else {
                                log.warn("No browser service found to navigate to translations.");
                            }
                            return null;
                        }

                    });
                }
            }

            @Override
            protected void onDetach() {
                languageModel.detach();
                super.onDetach();
            }
        };
        rv.setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());
        add(rv);
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        rv.populate();
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(getPluginConfig().getString("locale.id", ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

    protected IBrowseService getBrowserService() {
        return getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID),
                IBrowseService.class);
    }

    protected StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

}
