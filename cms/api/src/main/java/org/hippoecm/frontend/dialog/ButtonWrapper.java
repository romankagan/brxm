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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class ButtonWrapper implements IClusterable {
        private static final long serialVersionUID = 1L;

        private Button button;

        private boolean ajax;
        private IModel<String> label;
        private boolean visible;
        private boolean enabled;
        private KeyType keyType;
        private boolean hasChanges = false;

        public ButtonWrapper(Button button) {
            this.button = button;
            visible = button.isVisible();
            enabled = button.isEnabled();
            label = button.getModel();

            if (button instanceof AjaxButton) {
                ajax = true;
            }
        }

        public ButtonWrapper(IModel<String> label) {
            this(label, true);
        }

        public ButtonWrapper(IModel<String> label, boolean ajax) {
            this.ajax = ajax;
            this.label = label;
            this.visible = true;
            this.enabled = true;
        }

        private Button createButton() {
            if (ajax) {
                AjaxButton button = new AjaxButton(DialogConstants.BUTTON) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        ButtonWrapper.this.onSubmit();
                    }

                    @Override
                    public boolean isVisible() {
                        return visible;
                    }

                    @Override
                    public boolean isEnabled() {
                        return enabled;
                    }
                };
                button.setModel(label);
                return button;
            } else {
                Button button = new Button(DialogConstants.BUTTON) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        ButtonWrapper.this.onSubmit();
                    }

                    @Override
                    public boolean isVisible() {
                        return visible;
                    }

                    @Override
                    public boolean isEnabled() {
                        return enabled;
                    }
                };
                button.setModel(label);
                return button;
            }
        }

        public Button getButton() {
            if (button == null) {
                button = decorate(createButton());
            }
            return button;
        }

        protected Button decorate(Button button) {
            button.setEnabled(enabled);
            button.setVisible(visible);
            if (getKeyType() != null) {
                button.add(new InputBehavior(new KeyType[]{getKeyType()}, EventType.click));
            }
            return button;
        }

        public void setEnabled(boolean isset) {
            enabled = isset;
            if (button != null) {
                button.setEnabled(isset);
                if (ajax) {
                    AjaxRequestTarget target = AjaxRequestTarget.get();
                    if (target != null) {
                        target.addComponent(button);
                    }
                }
            }
        }

        public void setVisible(boolean isset) {
            visible = isset;
            if (button != null) {
                button.setVisible(isset);
                if (ajax) {
                    AjaxRequestTarget target = AjaxRequestTarget.get();
                    if (target != null) {
                        target.addComponent(button);
                    }
                }
            }
        }

        public void setAjax(boolean c) {
            ajax = c;
        }

        public void setLabel(IModel<String> label) {
            this.label = label;
            if (button != null) {
                button.setModel(label);
            }
            hasChanges = true;
        }

        public void setKeyType(KeyType keyType) {
            this.keyType = keyType;
        }

        protected void onSubmit() {
        }

        public boolean hasChanges() {
            if (!ajax) {
                return false;
            }

            if (button == null) {
                return true;
            }

            if (visible != button.isVisible()) {
                return true;
            }

            if (enabled != button.isEnabled()) {
                return true;
            }

            return hasChanges;
        }

        protected KeyType getKeyType() {
            return keyType;
        }

    }
