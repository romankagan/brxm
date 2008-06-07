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
package org.hippoecm.frontend.plugins.admin.menu.reset;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.hippoecm.frontend.legacy.dialog.AbstractDialog;
import org.hippoecm.frontend.legacy.dialog.DialogWindow;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Request;
import org.hippoecm.frontend.model.JcrNodeModel;

/**
 * @deprecated use org.hippoecm.frontend.plugins.console.menu.* instead
 */
@Deprecated
public class ResetDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private boolean hasPendingChanges;

    public ResetDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
        dialogWindow.setTitle("Refresh Session (undo changes)");

        Component message;
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        try {
            NodeIterator it = nodeModel.getNode().pendingChanges();
            hasPendingChanges = it.hasNext();
            if (hasPendingChanges) {
                StringBuffer buf = new StringBuffer("Pending changes:\n");
                while (it.hasNext()) {
                    Node node = it.nextNode();
                    buf.append(node.getPath()).append("\n");
                }
                message = new MultiLineLabel("message", buf.toString());
            } else {
                message = new Label("message", "There are no pending changes");
                ok.setVisible(false);
            }
        } catch (RepositoryException e) {
            message = new Label("message", "exception: " + e.getMessage());
            ok.setVisible(false);
        }
        add(message);
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel nodeModel = getDialogWindow().getNodeModel();

        // The actual JCR refresh
        nodeModel.getNode().refresh(false);

        Channel channel = getChannel();
        if (channel != null) {
            Request request = channel.createRequest("select", nodeModel);
            channel.send(request);

            if (hasPendingChanges) {
                request = channel.createRequest("flush", nodeModel.findRootModel());
                channel.send(request);
            }
        }
    }

    @Override
    public void cancel() {
    }

}
