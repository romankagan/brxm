package org.hippoecm.frontend.plugins.search;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.search.yui.SearchBehavior;
import org.hippoecm.frontend.plugins.yui.autocomplete.AutoCompleteSettings;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchPlugin extends RenderPlugin implements IBrowseService<IModel> {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SearchPlugin.class);

    public SearchPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        TextField tx = new TextField("searchBox");
        AutoCompleteSettings settings = new AutoCompleteSettings(config.getPluginConfig("yui.autocomplete.config"));
        tx.add(new SearchBehavior(settings, this, config.getStringArray("search.paths")));
        add(tx);
    }

    public void browse(IModel model) {
        String browserId = getPluginConfig().getString("browser.id");
        IBrowseService<IModel> browseService = getPluginContext().getService(browserId, IBrowseService.class);
        if (browseService != null && model instanceof JcrNodeModel) {
            browseService.browse(model);
        }
    }
}
