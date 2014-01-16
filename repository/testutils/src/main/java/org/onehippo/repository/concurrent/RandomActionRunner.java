/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.repository.concurrent.action.Action;
import org.slf4j.Logger;

public class RandomActionRunner extends ActionRunner {

    private final Random random = new Random();

    public RandomActionRunner(final Session session, Logger log,
                              final List<Class<? extends Action>> actions,
                              final long duration,
                              final long throttle) {
        super(session, log, actions, duration, throttle);
    }

    @Override
    protected Node selectNode() throws RepositoryException {
        Node root;
        if (random.nextGaussian() < .75) {
            root = context.getDocumentBase();
        } else {
            root = context.getAssetBase();
        }
        Node node = null;
        while (node == null) {
            try {
                node = traverse(root);
            } catch (RepositoryException e) {
                log.debug("Failed to traverse nodes: " + e);
            }
        }
        return node;
    }

    private Node traverse(final Node node) throws RepositoryException {
        if (node.hasNodes() && random.nextGaussian() < .5) {
            final NodeIterator nodes = node.getNodes();
            final int size = (int)nodes.getSize();
            if (size > 0) {
                int index = random.nextInt(size);
                if (index > 0) {
                    nodes.skip(index-1);
                }
                Node child = nodes.nextNode();
                if (child.isNodeType("hippo:handle")) {
                    child = child.getNode(child.getName());
                    return child;
                }
                return traverse(child);
            }
        }
        return node;
    }

    @Override
    protected Action getAction(final Node node) {
        final List<Action> actions = new ArrayList<Action>(this.actions.size());
        for (Class<? extends Action> actionClass : this.actions) {
            try {
                final Action action = context.getAction(actionClass);
                if (action.canOperateOnNode(node)) {
                    actions.add(action);
                }
            } catch (Exception ex) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to determine if action is able to operate on node", ex);
                }
            }
        }
        if (actions.size() > 0) {
            Action action = null;
            double weight = 0.0;
            for (Action a : actions) {
                weight += a.getWeight();
            }
            weight = (1.0 - random.nextDouble()) * weight;
            for (Action a : actions) {
                weight -= a.getWeight();
                if (weight <= 0.0) {
                    action = a;
                    break;
                }
            }
            return action;
        }
        return null;

    }

}
