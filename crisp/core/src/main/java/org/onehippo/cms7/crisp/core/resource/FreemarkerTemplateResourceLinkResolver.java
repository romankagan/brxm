package org.onehippo.cms7.crisp.core.resource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.onehippo.cms7.crisp.api.resource.AbstractResourceLinkResolver;
import org.onehippo.cms7.crisp.api.resource.ResourceContainer;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerTemplateResourceLinkResolver extends AbstractResourceLinkResolver {

    private static Logger log = LoggerFactory.getLogger(FreemarkerTemplateResourceLinkResolver.class);

    private Configuration configuration;
    private String templateSource;
    private Template template;

    private Properties properties;

    public FreemarkerTemplateResourceLinkResolver() {
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getTemplateSource() {
        return templateSource;
    }

    public void setTemplateSource(String templateSource) {
        this.templateSource = templateSource;
    }

    @Override
    public ResourceLink resolve(ResourceContainer resource, Map<String, Object> variables) throws ResourceException {
        Map<String, Object> context = new HashMap<String, Object>();

        if (variables != null) {
            context.putAll(variables);
        }

        context.put("resource", resource);
        StringWriter out = new StringWriter();

        try {
            getTemplate().process(context, out);
            out.flush();
            return new SimpleResourceLink(out.toString());
        } catch (Exception e) {
            throw new ResourceException("Resource link resolving failure.", e);
        }
    }

    protected Template getTemplate() {
        if (template == null) {
            template = createTemplate();
        }

        return template;
    }

    protected Template createTemplate() {
        try {
            return new Template(FreemarkerTemplateResourceLinkResolver.class.getSimpleName() + "-main",
                    new StringReader(templateSource), getConfiguration());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create template.");
        }
    }

    protected Configuration getConfiguration() {
        if (configuration == null) {
            configuration = createConfiguration();
        }

        return configuration;
    }

    protected Configuration createConfiguration() {
        Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS));

        if (properties != null) {
            try {
                cfg.setSettings(properties);
            } catch (TemplateException e) {
                log.warn("Failed to load Freemarker configuration properties.", e);
            }
        }

        return cfg;
    }

}
