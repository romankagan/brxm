/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.admin.menu.move;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.DefaultTreeModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.tree.JcrLazyTreeNode;
import org.hippoecm.frontend.tree.LazyTreeModel;

public class MoveDialog extends AbstractDialog  {
    private static final long serialVersionUID = 1L;

    private MoveTargetTreeView tree;
    private MoveDialogInfoPanel infoPanel;

    public MoveDialog(final DialogWindow dialogWindow, JcrNodeModel model) {
        super(dialogWindow, model);
        dialogWindow.setTitle("Move selected node");

        Node root;
        try {
            root = model.getNode().getSession().getRootNode();
            JcrNodeModel rootModel = new JcrNodeModel(root);
            JcrLazyTreeNode treeNode = new JcrLazyTreeNode(null, rootModel);
            LazyTreeModel treeModel = new LazyTreeModel(treeNode);
            tree = new MoveTargetTreeView("tree", treeModel, this);
            tree.getTreeState().expandNode(treeNode);
            add(tree);

            infoPanel = new MoveDialogInfoPanel("info", model);
            add(infoPanel);

        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (model.getNode() == null) {
            ok.setVisible(false);
        }
    }

    public void ok() throws RepositoryException {
        if (model.getNode() != null) {
            JcrLazyTreeNode treeNode = (JcrLazyTreeNode) tree.getSelectedNode();
            JcrNodeModel targetNodeModel = treeNode.getJcrNodeModel();
            String destination = targetNodeModel.getNode().getPath() + "/" + model.getNode().getName();
            model.getNode().getSession().move(model.getNode().getPath(), destination);
        }
    }

    public void cancel() {
    }

    public String getMessage() {
        try {
            return "Move " + model.getNode().getPath();
        } catch (RepositoryException e) {
            return "";
        }
    }

    public void setMessage(String message) {
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        if (model != null) {
            try {
                infoPanel.setDestinationPath(model.getNode().getPath() + "/" + this.model.getNode().getName());
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (target != null) {
            target.addComponent(infoPanel);
        }
    }

}
