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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.repository.api.HippoNode;

public class MoveDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private MoveTargetTreeView tree;
    private MoveDialogInfoPanel infoPanel;

    public MoveDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
        dialogWindow.setTitle("Move selected node");
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();

        UserSession session = (UserSession)getSession();
        HippoNode rootNode = session.getRootNode();
        JcrNodeModel rootModel = new JcrNodeModel(null, rootNode);

        JcrTreeNode rootNodeModel = new JcrTreeNode(rootModel);
        JcrTreeModel treeModel = new JcrTreeModel(rootNodeModel);
        
        tree = new MoveTargetTreeView("tree", treeModel, this);
        tree.getTreeState().expandNode(rootNodeModel);
        add(tree);

        infoPanel = new MoveDialogInfoPanel("info", nodeModel);
        add(infoPanel);

        if (nodeModel.getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    public JcrEvent ok() throws RepositoryException {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();

        JcrEvent result;
        if (nodeModel.getParentModel() == null) {
            result = new JcrEvent(nodeModel, false);
        } else {
            String nodeName = nodeModel.getNode().getName();
            String sourcePath = nodeModel.getNode().getPath();

            JcrTreeNode targetNodeModel = (JcrTreeNode) tree.getSelectedNode();            
            String targetPath = targetNodeModel.getNodeModel().getNode().getPath();
            if (!targetPath.endsWith("/")) {
                targetPath += "/";
            }
            targetPath += nodeName; 
            
            // The actual move
            Session jcrSession = ((UserSession) getSession()).getJcrSession();
            jcrSession.move(sourcePath, targetPath);
            
            //TODO: use common ancestor iso root
            JcrNodeModel rootNodeModel = targetNodeModel.getNodeModel();
            while (nodeModel.getParentModel() != null) {
                rootNodeModel = nodeModel.getParentModel();
            }
            result = new JcrEvent(rootNodeModel, true);
        }

        return result;
    }

    @Override
    public void cancel() {
    }

    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        if (model != null) {
            try {
                infoPanel.setDestinationPath(model.getNode().getPath());
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
