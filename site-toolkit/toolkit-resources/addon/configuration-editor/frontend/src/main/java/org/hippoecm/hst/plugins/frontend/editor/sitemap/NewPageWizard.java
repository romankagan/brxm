package org.hippoecm.hst.plugins.frontend.editor.sitemap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardModel;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardStep;
import org.apache.wicket.extensions.wizard.dynamic.IDynamicWizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.components.DescriptionPanel;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;
import org.hippoecm.hst.plugins.frontend.editor.dao.ComponentDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.PageDAO;
import org.hippoecm.hst.plugins.frontend.editor.dao.TemplateDAO;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component;
import org.hippoecm.hst.plugins.frontend.editor.validators.NodeUniqueValidator;
import org.hippoecm.hst.plugins.frontend.util.JcrUtilities;

public abstract class NewPageWizard extends AjaxWizard {
    private static final long serialVersionUID = 1L;

    private final class PageNameStep extends DynamicWizardStep {
        private static final long serialVersionUID = 1L;

        public PageNameStep(IDynamicWizardStep previousStep) {
            super(previousStep);

            setTitleModel(new Model("Give your new page a name"));

            RequiredTextField textField = new RequiredTextField("name", new PropertyModel(newPage, "name"));
            textField.add(new NodeUniqueValidator<Component>(new BeanProvider<Component>() {
                private static final long serialVersionUID = 1L;

                public Component getBean() {
                    return newPage;
                }

            }));
            add(textField);
        }

        public boolean isLastStep() {
            return false;
        }

        public IDynamicWizardStep next() {
            return new PageTemplateStep(this);
        }
    }

    private final class PageTemplateStep extends DynamicWizardStep {
        private static final long serialVersionUID = 1L;

        public PageTemplateStep(IDynamicWizardStep previousStep) {
            super(previousStep);

            setTitleModel(new Model("Select a template"));

            List<String> templates = hstContext.template.getTemplatesAsList();

            final String originalTemplate = newPage.getTemplate();
            final DropDownChoice dc = new DropDownChoice("template", new PropertyModel(newPage, "template"), templates);
            dc.setNullValid(false);
            dc.setRequired(true);
            dc.setOutputMarkupId(true);
            dc.add(new AjaxFormComponentUpdatingBehavior("onChange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (!newPage.getTemplate().equals(originalTemplate)) {
                        info("Your template has changed, saving this session will create new child components as specified by the templates containers. Existing child components will be removed if they don't match the container names.");
                    }
                }
            });
            add(dc);
        }

        public boolean isLastStep() {
            return false;
        }

        public IDynamicWizardStep next() {
            JcrNodeModel template = new JcrNodeModel(hstContext.template.absolutePath(newPage.getTemplate()));
            List<String> containers = JcrUtilities.getMultiValueProperty(template, TemplateDAO.HST_CONTAINERS);
            if (containers != null && containers.size() > 0) {
                containersModel = new Containers(containers);
                return new ComponentDescriptionStep(this);
            }
            return new PageDescriptionStep(this);
        }
    }

    private final class ComponentDescriptionStep extends DynamicWizardStep {
        private static final long serialVersionUID = 1L;

        public ComponentDescriptionStep(DynamicWizardStep previousStep) {
            super(previousStep);

            add(new Label("containerName", new Model(containersModel.getName())));

            List<String> templates = hstContext.component.getComponentsAsList();

            final DropDownChoice dc = new DropDownChoice("components", new PropertyModel(containersModel, "component"),
                    templates);
            dc.setNullValid(false);
            dc.setRequired(true);
            dc.setOutputMarkupId(true);
            dc.add(new AjaxFormComponentUpdatingBehavior("onChange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {

                }
            });
            add(dc);
        }

        public boolean isLastStep() {
            return false;
        }

        public IDynamicWizardStep next() {
            if (containersModel.hasNext()) {
                containersModel.next();
                return new ComponentDescriptionStep(this);
            }
            return new PageDescriptionStep(this);
        }
    }

    private final class PageDescriptionStep extends DynamicWizardStep {
        private static final long serialVersionUID = 1L;

        public PageDescriptionStep(IDynamicWizardStep previousStep) {
            super(previousStep);

            add(new DescriptionPanel("desc", new Model(newPage), context, config));
        }

        public boolean isLastStep() {
            return true;
        }

        public IDynamicWizardStep next() {
            return null;
        }

    }

    private PageDAO pageDao;
    private Component newPage;
    private HstContext hstContext;

    private IPluginContext context;
    private IPluginConfig config;

    private Containers containersModel;

    public NewPageWizard(String id, IPluginContext context, IPluginConfig config) {
        super(id, false);

        this.context = context;
        this.config = config;

        hstContext = context.getService(HstContext.class.getName(), HstContext.class);

        pageDao = new PageDAO(context, hstContext.page.getNamespace());
        newPage = pageDao.create(hstContext.page.getModel());

        setOutputMarkupId(true);

        DynamicWizardModel model = new DynamicWizardModel(new PageNameStep(null));

        init(model);
    }

    @Override
    public void onCancel() {
        if (pageDao.delete(newPage)) {
            info("Wizard cancelled");
        }
    }

    @Override
    public final void onFinish() {
        if (pageDao.save(newPage)) {
            ComponentDAO cDao = new ComponentDAO(context, hstContext.component.getNamespace());
            for (Entry<String, String> e : containersModel.values.entrySet()) {
                JcrNodeModel cModel = new JcrNodeModel(newPage.getModel().getItemModel().getPath() + "/" + e.getKey());
                Component c = cDao.load(cModel);
                c.setReference(true);
                c.setReferenceName(e.getValue());
                cDao.save(c);
            }
        }
    }

    public final void onFinish2() {
        pageDao.persist(newPage, newPage.getModel());

        ComponentDAO cDao = new ComponentDAO(context, hstContext.component.getNamespace());
        for (Entry<String, String> e : containersModel.values.entrySet()) {
            JcrNodeModel cModel = new JcrNodeModel(newPage.getModel().getItemModel().getPath() + "/" + e.getKey());
            Component c = cDao.load(cModel);
            c.setReference(true);
            c.setReferenceName(e.getValue());
            cDao.persist(c, c.getModel());
        }
        Session session;
        try {
            session = newPage.getModel().getNode().getSession();
            if (session.hasPendingChanges()) {
                session.save();
                onFinish(newPage);
            }
        } catch (RepositoryException e1) {
            e1.printStackTrace();
            error("Error saving new page.");
        }

        //        if (pageDao.save(newPage)) {
        //        } else {
        //        }
    }

    protected abstract void onFinish(Component page);

    class Containers implements IClusterable {

        private static final long serialVersionUID = 1L;

        Map<String, String> values;
        List<String> containers;
        int current;

        public Containers(List<String> containers) {
            current = 0;
            values = new HashMap<String, String>();
            this.containers = containers;
        }

        public void next() {
            current++;
        }

        public boolean hasNext() {
            return containers.size() < (current + 1);
        }

        public String getName() {
            return containers.get(current);
        }

        public void setComponent(String name) {
            values.put(getName(), name);
        }

        public String getComponent() {
            return null;
        }
    }
}
