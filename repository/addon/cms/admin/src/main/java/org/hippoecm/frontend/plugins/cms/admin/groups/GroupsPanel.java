/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.users.CreateUserPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxBreadCrumbPanelLink;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;

public class GroupsPanel extends AdminBreadCrumbPanel {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private AdminDataTable table;

    public GroupsPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        add(new AjaxBreadCrumbPanelLink("create-group", context, this, CreateUserPanel.class));

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new AbstractColumn(new Model("Name"), "groupname") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item item, final String componentId, final IModel model) {
                
                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new PropertyModel(model, "groupname")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        //panel.showView(target, model);
                        activate(new IBreadCrumbPanelFactory()
                        {
                            public BreadCrumbPanel create(String componentId,
                                    IBreadCrumbModel breadCrumbModel)
                            {
                                return new ViewGroupPanel(componentId, context, breadCrumbModel, model);
                            }
                        });
                    }
                };
                item.add(action);
            }
        });

        columns.add(new PropertyColumn(new Model("Description"), "description", "description"));
        columns.add(new AbstractColumn(new Model("Members")) {
            private static final long serialVersionUID = 1L;

            public void populateItem(Item cellItem, String componentId, IModel model) {
                Group group = (Group) model.getObject();
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (String user : group.getMembers()) {
                    if (first) {
                        sb.append(user);
                        first = false;
                    } else {
                        sb.append(',').append(user);
                    }
                }
                cellItem.add(new Label(componentId, sb.toString()));
            }
        });

        table = new AdminDataTable("table", columns, new GroupDataProvider(), 40);
        add(table);
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("admin-groups-title", component, null);
    }

}