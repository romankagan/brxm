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

Ext.namespace('Hippo.ChannelManager.TemplateComposer');

Hippo.ChannelManager.TemplateComposer.PageEditor = Ext.extend(Ext.Panel, {

    constructor : function(config) {
        if (config.debug) {
            Ext.Ajax.timeout = 90000; // this changes the 30 second default to 90 seconds
        }

        this.ids = {
            pageUrl : null,
            pageId : null,
            mountId : null
        };

        this.stores = {
            toolkit : null,
            pageModel : null
        };

        if (config.composerMountUrl.lastIndexOf('/') !== config.composerMountUrl.length - 1) {
            config.composerMountUrl = config.composerMountUrl + '/';
        }
        if (config.renderHostSubMountPath && config.renderHostSubMountPath.indexOf('/') === 0) {
            config.renderHostSubMountPath = config.renderHostSubMountPath.substr(1);
        }

        this.hstInComposerMode = false;
        this.isPreviewHstConfig = false;

        this.addEvents('beforePreCacheIFrameResources', 'afterPreCacheIFrameResources',
                       'beforePageIdChange', 'beforeMountIdChange',
                       'beforeIFrameDOMReady', 'afterIFrameDOMReady',
                       'beforeRequestHstMetaData', 'hstMetaDataResponse',
                       'beforeInitializeIFrameHead', 'afterInitializeIFrameHead', 'iFrameInitialized',
                       'isPreviewHstConfig',
                       'iFrameException');

        this.pageModelFacade = null;

        this.iframeResourceCache = [];
        this.preCacheIFrameResources(config);

        this.initUI(config);

        Hippo.ChannelManager.TemplateComposer.PageEditor.superclass.constructor.call(this, config);
    },

    //Keeps the session alive every minute
    keepAlive : function() {
        Ext.Ajax.request({
            url: this.composerRestMountUrl + 'cafebabe-cafe-babe-cafe-babecafebabe./keepalive',
            success: function () {
                // Do nothing
            }
        });
    },

    initUI : function(config) {
        Ext.apply(config, { items :
            [
                {
                    id: 'Iframe',
                    xtype: 'iframepanel',
                    // loadMask: true,
                    collapsible: false,
                    disableMessaging: false,
                    tbar: [
                        {
                            text : config.resources['back-to-channel-manager-button'],
                            id : "channelManager",
                            listeners : {
                                'click' : {
                                    fn : function() {
                                        Ext.getCmp('rootPanel').showChannelManager();
                                    },
                                    scope: this
                                }
                            }
                        },
                        {
                            text: config.resources['preview-button'],
                            iconCls: 'title-button',
                            id: 'pagePreviewButton',
                            toggleGroup : 'composerMode',
                            pressed: config.previewMode,
                            allowDepress: false,
                            width: 150,
                            disabled: true
                        },
                        {
                            text: config.resources['edit-button'],
                            iconCls: 'title-button',
                            id: 'pageComposerButton',
                            enableToggle: true,
                            toggleGroup : 'composerMode',
                            pressed: !config.previewMode,
                            allowDepress: false,
                            width: 150,
                            disabled: true,
                            listeners: {
                                'toggle': {
                                    fn : this.toggleMode,
                                    scope: this
                                }
                            }
                        },
                        {
                            text: config.resources['publish-button'],
                            iconCls: 'title-button',
                            id: 'publishHstConfig',
                            width: 150,
                            disabled: true,
                            listeners: {
                                'click': {
                                    fn : function() {
                                        this.publishHstConfiguration();
                                    },
                                    scope: this
                                }
                            }
                        },
                        {
                            id: 'channelName',
                            xtype: 'tbtext',
                            text: '',
                            style: {
                                marginLeft: '150px'
                            }
                        }
                    ],
                    listeners: {
                        'message': {
                            fn: this.handleFrameMessages,
                            scope:this
                        },
                        'documentloaded' : {
                            fn: function(frm) {
                                //Only called after the first load. Every refresh within a running iframe result in
                                //a domready event only
                                if (Ext.isSafari || Ext.isChrome) {
                                    this.onIframeDOMReady(frm);
                                }
                            },
                            scope: this
                        },
                        'domready' : {
                            fn: function(frm) {
                                //Safari && Chrome report a DOM ready event, but js is not yet fully loaded, resulting
                                //in 'undefined' errors.
                                if (Ext.isGecko || Ext.isIE) {
                                    this.onIframeDOMReady(frm);
                                }
                            },
                            scope: this
                        },
                        'exception' : {
                            fn: function(frm, e) {
                                Hippo.Msg.alert(this.resources['iframe-event-exception-message-title'], e.message, function() {
                                    this.refreshIframe();
                                });
                                console.error(e);
                            },
                            scope: this
                        },
                        'resize' : {
                            fn: function() {
                                this.sendFrameMessage({}, 'resize');
                            },
                            scope: this
                        }
                    }
                }
            ]
        });

        if (config.debug) {
            Ext.data.DataProxy.addListener('exception', function(proxy, type, action, options, res, e) {
                if (!res.success && res.message) {
                    console.error(res.message);
                }
                else {
                    if (e) {
                        if(typeof console.error == 'function') {
                           console.error(e);
                        } else {
                            throw e;
                        }
                    } else if(res.status) {
                        var json = Ext.util.JSON.decode(res.responseText);
                        var msg = '<br/><b>StatusText:</b> ' + res.statusText + '<br/><b>StatusCode:</b> ' + res.status +
                                '<br/><b>Detailed message:</b> ' + json.message;
                        console.error(json.message);
                    } else {
                        console.group("Exception");
                        console.dir(arguments);
                        console.groupEnd();
                    }
                }
            }, this);

            Ext.data.DataProxy.addListener('write', function(proxy, action, result, res, rs) {
                console.log('Data Proxy Action: ' + action + '<br/>Message: ' + res.message);
            }, this);
        }
    },

    initComponent : function() {
        Hippo.ChannelManager.TemplateComposer.PageEditor.superclass.initComponent.call(this);
        // recalculate the ExtJs layout when the YUI layout manager fires a resize event
        this.on('afterrender', function() {
            var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
            YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, function() {
                Ext.getCmp('Iframe').setSize(arguments[0].body.w, arguments[0].body.h);
            }, true);

            if (this.renderHostSubMountPath && this.renderHost) {
                this.initComposer(this.renderHostSubMountPath, this.renderHost);
            }
        }, this, {single: true});

        this.on('beforePreCacheIFrameResources', function() {
            console.log('beforePreCacheIFrameResources');
            this.iframeResourcesCached = false;
        }, this);

        this.on('afterPreCacheIFrameResources', function() {
            console.log('afterPreCacheIFrameResources');
            this.iframeResourcesCached = true;
        }, this);

        this.on('beforeIFrameDOMReady', function() {
            console.log('beforeIFrameDOMReady');
            this.iframeDOMReady = false;
            this.iframeInitialized = false;
        }, this);

        this.on('afterIFrameDOMReady', function() {
            console.log('afterIFrameDOMReady');
            this.iframeDOMReady = true;
        }, this);

        this.on('beforeInitializeIFrameHead', function() {
            this.initializingIFrameHead = true;
            console.log('beforeInitializeIFrameHead');
        }, this);

        this.on('iFrameInitialized', function() {
            console.log('onIFrameInitialized');
            this.initializingIFrameHead = false;
            this.iframeInitialized = true;
        }, this);

        this.on('hstMetaDataResponse', this.onHstMetaDataResponse, this);

        this.on('beforeMountIdChange', function(data) {
            if (!this.preview && !data.isPreviewHstConfig && this.isPreviewHstConfig != data.isPreviewHstConfig) {
                // switching mount when edit is active and no preview available on the new mount
                if (this.initializingIFrameHead) {
                    this.on('iFrameInitialized', function() {
                        Ext.getCmp('pagePreviewButton').toggle(true);
                        this.refreshIframe();
                    }, this, {single : true});
                } else {
                    Ext.getCmp('pagePreviewButton').toggle(true);
                    this.refreshIframe();
                }
            } else {
                if (!this.iframeInitialized) {
                    console.log('add beforeInitializeIFrameHead listener');
                    this.mountIdIFrameInitListener = function() {
                        this.initEditMount(data.mountId)
                    };
                    this.on('beforeRequestHstMetaData', function() {
                        if (this.mountIdIFrameInitListener) {
                            this.removeListener('beforeInitializeIFrameHead', this.mountIdIFrameInitListener, this);
                            this.mountIdIFrameInitListener = null;
                        }
                    }, this, {single : true});
                    this.on('beforeInitializeIFrameHead', this.mountIdIFrameInitListener, this, {single: true});
                } else {
                    this.initEditMount(data.mountId);
                }
            }
        }, this);

        this.on('beforePageIdChange', function(data) {
            console.log('beforePageIdChange, previewMode: '+this.previewMode);
            if (!this.iframeInitialized) {
                console.log('add beforeInitializeIFrameHead listener for '+JSON.stringify(data));
                this.pageIdIFrameInitListener = function() {
                    console.log('pageIdIFrameInitListener '+JSON.stringify(data));
                    this.initEditPage(data.mountId, data.pageId);
                };
                // remove listener when new data is requested, needed for refresh if page uuid changes due to edit
                this.on('beforeRequestHstMetaData', function() {
                    if (this.pageIdIFrameInitListener) {
                        console.log('remove previous pageIdIFrameInitListener '+JSON.stringify(data));
                        this.removeListener('beforeInitializeIFrameHead', this.pageIdIFrameInitListener, this);
                        this.pageIdIFrameInitListener = null;
                    }
                }, this, {single : true});
                this.on('beforeInitializeIFrameHead', this.pageIdIFrameInitListener, this, {single: true});
            } else {
                this.initEditPage(data.mountId, data.pageId);
            }
        }, this);

        this.on('beforeInitComposer', function() {
            Hippo.Msg.wait(this.resources['loading-message']);
            this.previewMode = true;
            this.ids.pageId = null;
            this.ids.mountId = null;
            this.ids.pageUrl = null;
            this.isPreviewHstConfig = false;
            this.channelName = null;
            this.resetIFrameState();
            Ext.getCmp('pagePreviewButton').toggle(true, true);
            Ext.getCmp('pageComposerButton').toggle(false, true);
            Ext.getCmp('pagePreviewButton').setDisabled(true);
            Ext.getCmp('pageComposerButton').setDisabled(true);
            Ext.getCmp('publishHstConfig').setDisabled(true);
            if (this.mainWindow) {
                this.mainWindow.hide();
            }
        }, this);

        this.on('afterInitComposer', function() {
            Ext.getCmp('pagePreviewButton').setDisabled(false);
            Ext.getCmp('pageComposerButton').setDisabled(false);
            Hippo.Msg.hide();
        }, this);

        this.on('beforeModeChange', function(data) {
            console.log('beforeModeChange');
            Ext.getCmp('pagePreviewButton').setDisabled(data.previewMode);
            Ext.getCmp('pageComposerButton').setDisabled(!data.previewMode);
            if (data.previewMode) {
                Hippo.Msg.wait('Loading...');
            }
        }, this);

        this.on('modeChanged', function(data) {
            console.log('mode changed');
            if (data.previewMode) {
                this.mainWindow.hide();
            } else {
                this.mainWindow.show();
                Hippo.Msg.hide();
            }
            Ext.getCmp('pagePreviewButton').setDisabled(false);
            Ext.getCmp('pageComposerButton').setDisabled(false);
        }, this);

        this.on('isPreviewHstConfigChanged', function(data) {
            console.log('isPreviewHstConfigChanged ' + data.isPreviewHstConfig);
            Ext.getCmp('publishHstConfig').setDisabled(!data.isPreviewHstConfig);
        }, this);

        this.on('beforePublishHstConfiguration', function() {
            Ext.getCmp('pagePreviewButton').setDisabled(true);
            Ext.getCmp('pageComposerButton').setDisabled(true);
        }, this);

        this.on('afterPublishHstConfiguration', function() {
            Ext.getCmp('pagePreviewButton').setDisabled(false);
            Ext.getCmp('pageComposerButton').setDisabled(false);
        }, this);

        this.on('iFrameException', function(data) {
            var iFrame = Ext.getCmp('Iframe');
            var frm = iFrame.getFrame();
            if (frm !== null && data.msg) {
                this.on('afterIFrameDOMReady', function () {
                    frm.execScript('setErrorMessage(\''+data.msg+'\');', true);
                }, this, {single : true});
                frm.isReset = false;
                frm.setSrc(this.iFrameErrorPage);
            }
        }, this);
    },

    initEditMount : function(mountId) {
        this.stores.toolkit = this.createToolkitStore(mountId);
        this.stores.toolkit.on('exception', function(dataProxy, type, action, options, response, arg) {
            if (type === 'response') {
                console.error('Server returned status '+response.status+" for the toolkit store.");
            } else if (type === 'remote') {
                console.error('Error handling the response of the server for the toolkit store. Response is:\n'+response.responseText);
            }
            Hippo.Msg.alert(this.resources['toolkit-store-error-message-title'], this.resources['toolkit-store-error-message'], function(id) {
                this.initComposer(this.renderHostSubMountPath, this.renderHost);
            }, this);
        }, this);
        this.stores.toolkit.load();
        if (this.mainWindow) {
            var grid = Ext.getCmp('ToolkitGrid');
            grid.reconfigure(this.stores.toolkit, grid.getColumnModel());
        } else {
            this.mainWindow = this.createMainWindow(mountId);
        }
        this.mainWindow.show();
    },

    initEditPage : function(mountId, pageId) {
        this.stores.pageModel = this.createPageModelStore(mountId, pageId);
        this.stores.pageModel.on('exception', function(dataProxy, type, action, options, response, arg) {
            if (type === 'response') {
                console.error('Server returned status '+response.status+" for the page store.");
            } else if (type === 'remote') {
                console.error('Error handling the response of the server for the page store. Response is:\n'+response.responseText);
            }
            Hippo.Msg.alert(this.resources['page-store-error-message-title'], this.resources['page-store-error-message'], function(id) {
                this.initComposer(this.renderHostSubMountPath, this.renderHost);
            }, this);
        }, this);
        this.stores.pageModel.load();

        if (this.mainWindow) {
            console.log('reconfigure page model grid');
            var grid = Ext.getCmp('PageModelGrid');
            grid.reconfigure(this.stores.pageModel, grid.getColumnModel());
            this.mainWindow.show();
        }
    },

    preCacheIFrameResources : function(config) {
        this.fireEvent('beforePreCacheIFrameResources');
        var self = this;
        // clone array with concat()
        var queue = config.iFrameCssHeadContributions.concat().concat(config.iFrameJsHeadContributions);
        var requestContents = function(queueEmptyCallback) {
            if (queue.length == 0) {
                queueEmptyCallback();
                return;
            }
            var src = queue.shift();
            Ext.Ajax.request({
                url : src,
                method : 'GET',
                success : function(result, request) {
                    self.iframeResourceCache[src] = result.responseText;
                    requestContents(queueEmptyCallback);
                },
                failure : function(result, request) {
                    self.fireEvent.apply(self, ['iFrameException', {msg : self.resources['pre-cache-iframe-resources-exception'].format(src)}]);
                }
            });
        };
        requestContents(
            function() {
                self.iframeResourcesCached = true;
                self.fireEvent.call(self, 'afterPreCacheIFrameResources');
            }
        );
    },

    initComposer : function(renderHostSubMountPath, renderHost) {
        this.fireEvent('beforeInitComposer');
        this.on('afterIFrameDOMReady', function() {
            this.fireEvent('afterInitComposer');
        }, this, {single : true});
        if (renderHostSubMountPath && renderHostSubMountPath.indexOf('/') === 0) {
            this.renderHostSubMountPath = renderHostSubMountPath.substr(1);
        } else {
            this.renderHostSubMountPath = renderHostSubMountPath;
        }
        this.renderHost = renderHost;

        var retry = this.initialHstConnectionTimeout;
        if (!this.hstInComposerMode) {
            var me = this;
            // do initial handshake with CmsSecurityValve of the composer mount and
            // go ahead with the actual host which we want to edit (for which we need to be authenticated)
            var composerMode = function(callback) {
                Ext.Ajax.request({
                    url: me.composerRestMountUrl + 'cafebabe-cafe-babe-cafe-babecafebabe./composermode',
                    success: callback,
                    failure: function(exceptionObject) {
                        if (exceptionObject.isTimeout) {
                            retry = retry - Ext.Ajax.timeout;
                        }
                        if (retry > 0) {
                            retry = retry - 5000;
                            window.setTimeout(function() {
                                composerMode(callback);
                            }, 5000);
                        } else {
                            Hippo.Msg.hide();
                            Hippo.Msg.confirm(me.resources['hst-timeout-message-title'], me.resources['hst-timeout-message'], function(id) {
                                if (id === 'yes') {
                                    retry = me.initialHstConnectionTimeout;
                                    Hippo.Msg.wait('Loading...');
                                    composerMode(callback);
                                } else {
                                    me.fireEvent.apply(me, ['iFrameException', {msg : me.resources['hst-timeout-iframe-exception']}]);
                                }
                            });
                        }
                    }
                });
            };
            composerMode(function() {
                me.hstInComposerMode = true;
                var iFrame = Ext.getCmp('Iframe');
                iFrame.frameEl.isReset = false; // enable domready get's fired workaround, we haven't set defaultSrc on the first place
                iFrame.setSrc(me.composerMountUrl + me.renderHostSubMountPath + "?" + me.renderHostParameterName + "=" + me.renderHost);

                // keep session active
                Ext.TaskMgr.start({
                    run: me.keepAlive,
                    interval: 60000,
                    scope: me
                });
            });
        } else {
            var iFrame = Ext.getCmp('Iframe');
            iFrame.frameEl.isReset = false; // enable domready get's fired workaround, we haven't set defaultSrc on the first place
            iFrame.setSrc(this.composerMountUrl + this.renderHostSubMountPath + "?" + this.renderHostParameterName + "=" + this.renderHost);
        }
    },

    setChannelName : function(name) {
        if (typeof this.showTitleSwitchTimeout !== 'undefined') {
            window.clearTimeout(this.showTitleSwitchTimeout);
        }
        var oldName = this.channelName;
        this.channelName = name;
        var channelNameText = Ext.getCmp('channelName');
        if (typeof oldName !== 'undefined' && oldName !== null) {
            channelNameText.setText(this.resources['channel-switch-text'].format(oldName, name));
            this.showTitleSwitchTimeout = window.setTimeout(function() {
                channelNameText.setText(name);
            }, 5000);
        } else {
            channelNameText.setText(name);
        }
    },

    publishHstConfiguration : function() {
        this.fireEvent('beforePublishHstConfiguration');
        var self = this;
        Ext.Ajax.request({
            method: 'POST',
            url: this.composerRestMountUrl + this.ids.mountId + './publish',
            success: function () {
                Ext.getCmp('pagePreviewButton').toggle(true);
                self.on.apply(self, ['afterIFrameDOMReady', function() {
                    this.fireEvent('afterPublishHstConfiguration');
                }, self, {single : true}]);
                self.refreshIframe.call(self, null);
            },
            failure: function(result) {
                var jsonData = Ext.util.JSON.decode(result.responseText);
                Hippo.Msg.alert(self.resources['published-hst-config-failed-message']+' '+jsonData.message, function() {
                    self.refreshIframe.call(self, null);
                });
            }
        });
    },

    toggleMode: function () {
        this.fireEvent('beforeModeChange', {previewMode : this.previewMode});

        var func = function() {
            // first time switch to template composer mode
            // initialize iframe head for drag and drop...
            if (this.previewMode && !this.iframeInitialized) {

                this.previewMode = false;
                var initialize = function(mountId, pageId) {
                    console.log('this.editingUnpublishedHstConfig:' + this.isPreviewHstConfig);
                    if (this.isPreviewHstConfig) {
                        this.on('iFrameInitialized', function() {
                            this.fireEvent('modeChanged', {previewMode : this.previewMode});
                        }, this, { single: true});
                        this.initializeIFrameHead(this.frm);
                    } else {
                        // create new preview hst configuration
                        var self = this;
                        Ext.Ajax.request({
                            method: 'POST',
                            url: this.composerRestMountUrl + mountId + './edit',
                            success: function () {
                                // refresh iframe to get new hst config uuids. previewMode=false will initialize
                                // the editor for editing with the refresh
                                self.on('afterShareDataWithIFrame', function() {
                                    self.fireEvent('modeChanged', {previewMode : self.previewMode});
                                }, self, { single: true});
                                self.refreshIframe.call(self, null);
                            },
                            failure: function(result) {
                                var jsonData = Ext.util.JSON.decode(result.responseText);
                                Hippo.Msg.alert(this.resouces['preview-hst-config-creation-failed']+' '+jsonData.message, function() {
                                    self.previewMode = true;
                                    self.refreshIframe.call(self, null);
                                });
                            }
                        });
                    }
                };
                if (this.isHstMetaDataLoaded()) {
                    initialize.apply(this, [this.ids.mountId, this.ids.pageId]);
                } else {
                    this.on('hstMetaDataResponse', function(data) {
                        initialize.apply(this, [data.mountId, data.pageId]);
                    }, this, {single : true});
                }
            } else {
                // once the composer javascript in the iframe is injected and initialised we send a toggle
                // message which shows/hides the drag and drop overlay in the iframe
                var iframe = Ext.getCmp('Iframe');
                iframe.getFrame().sendMessage({}, 'toggle');
                this.previewMode = !this.previewMode;
                this.fireEvent('modeChanged', { previewMode : this.previewMode});
            }
        }

        if (this.iframeDOMReady) {
            func.call(this);
        } else {
            this.on('afterIFrameDOMReady', func, this, {single : true});
        }

        return true;
    },

    refreshIframe : function() {
        this.resetIFrameState();
        // we don't need to reload the stores, so just share the data again with the iframe
        this.shareData();
        var iframe = Ext.getCmp('Iframe');
        var frame = iframe.getFrame();
        var window = frame.getWindow();
        var scrollSave = {x: window.pageXOffset, y: window.pageYOffset};
        this.on('beforeIFrameDOMReady', function() {
            window.scrollBy(scrollSave.x, scrollSave.y);
        }, this, {single : true});
        iframe.setSrc(iframe.getFrameDocument().location.href); //following links in the iframe doesn't set iframe.src..
    },

    resetIFrameState : function() {
        this.iframeDOMReady = false;
        this.iframeInitialized = false;
    },

    onIframeDOMReady : function(frm) {
        this.fireEvent('beforeIFrameDOMReady');
        this.frm = frm;
        this.requestHstMetaData( { url: frm.getDocumentURI() } );
        if (!this.previewMode) {
            this.initializeIFrameHead(frm);
        }
        this.fireEvent('afterIFrameDOMReady');
    },

    initializeIFrameHead : function(frm) {
        this.fireEvent('beforeInitializeIFrameHead');

        var func = function() {
            for (var i = 0, len = this.iFrameCssHeadContributions.length; i < len; i++) {
                var src = this.iFrameCssHeadContributions[i];
                var responseText = this.iframeResourceCache[src];
                var frmDocument = frm.getFrameDocument();

                if (Ext.isIE) {
                    var style = frmDocument.createStyleSheet().cssText = responseText;
                } else {
                    var headElements = frmDocument.getElementsByTagName("HEAD");
                    var head;
                    if (headElements.length == 0) {
                        head = frmDocument.createElement("HEAD");
                        frmDocument.appendChild(head);
                    } else {
                        head = headElements[0];
                    }

                    var styleElement = frmDocument.createElement("STYLE");
                    styleElement.setAttribute("type", "text/css");
                    var textNode = frmDocument.createTextNode(responseText);
                    styleElement.appendChild(textNode);
                    styleElement.setAttribute("title", src);
                    head.appendChild(styleElement);
                }
            }

            for (var i = 0, len = this.iFrameJsHeadContributions.length; i < len; i++) {
                var src = this.iFrameJsHeadContributions[i];
                var responseText = this.iframeResourceCache[src];
                (function(src, responseText) {
                    window.setTimeout(function() {
                        frm.writeScript.apply(frm, [responseText, {type: "text/javascript", "title" : src}]);
                    }, 0);
                })(src, responseText);
            }

            var self = this;
            window.setTimeout(function() {
                frm.sendMessage({debug: self.debug,
                                 previewMode: self.previewMode,
                                 resources: self.resources}, 'init');
                self.fireEvent.call(self, 'afterInitializeIFrameHead');
            }, 0);
        };

        if (this.iframeResourcesCached) {
            func.call(this);
        } else {
            this.on('afterPreCacheIFrameResources', func, this, {single : true});
        }
    },

    requestHstMetaData: function(options) {
        this.fireEvent('beforeRequestHstMetaData');
        console.log('requestHstMetaData' + JSON.stringify(options));
        var self = this;
        Ext.Ajax.request({
            method: "HEAD",
            url : options.url,
            success : function(responseObject) {
                if (options.url !== self.frm.getDocumentURI()) {
                    // response is out of date
                    return;
                }
                self.fireEvent.apply(self, ["hstMetaDataResponse", {
                    url : options.url,
                    pageId : responseObject.getResponseHeader('HST-Page-Id'),
                    mountId : responseObject.getResponseHeader('HST-Mount-Id'),
                    siteId : responseObject.getResponseHeader('HST-Site-Id'),
                    hasPreviewConfig : responseObject.getResponseHeader('HST-Site-HasPreviewConfig')
                }]);
            },
            failure : function(responseObject) {
                self.fireEvent.apply(self, ['iFrameException', { msg: self.resources['hst-meta-data-request-failed']}]);
            }
        });
    },

    onHstMetaDataResponse: function(data) {
        console.log('hstMetaDataResponse '+JSON.stringify(data));
        var pageId = data.pageId;
        var mountId = data.mountId;
        var hasPreviewConfig = this.getBoolean(data.hasPreviewConfig);

        if (mountId != this.ids.mountId) {
            if (this.fireEvent('beforeMountIdChange', {mountId: mountId, oldMountId: this.ids.mountId, pageId: pageId, isPreviewHstConfig: hasPreviewConfig})) {
                this.ids.mountId = mountId;
            }
        }

        if (pageId != this.ids.pageId) {
            if (this.fireEvent('beforePageIdChange', {mountId: mountId, pageId: pageId, oldPageId: this.ids.pageId, isPreviewHstConfig: hasPreviewConfig})) {
                this.ids.pageId = pageId;
                console.log('set pageUrl to '+data.url)
                this.ids.pageUrl = data.url;
            }
        }

        if (this.isPreviewHstConfig !== hasPreviewConfig) {
            this.isPreviewHstConfig = hasPreviewConfig;
            this.fireEvent('isPreviewHstConfigChanged', {mountId: mountId, pageId: pageId, isPreviewHstConfig: hasPreviewConfig });
        }
    },

    getBoolean: function(object) {
        if (typeof object === 'undefined' || object === null) {
            return null;
        }
        if (object === true || object === false) {
            return object;
        }
        var str = object.toString().toLowerCase();
        if (str === "true") {
            return true;
        } else if (str === "false") {
            return false
        }
        return null;
    },

    isHstMetaDataLoaded : function() {
        return this.ids.pageUrl === this.frm.getDocumentURI();
    },

    createToolkitStore : function(mountId) {
        return new Hippo.ChannelManager.TemplateComposer.ToolkitStore({
            mountId : mountId,
            composerRestMountUrl : this.composerRestMountUrl
        });
    },

    createPageModelStore : function(mountId, pageId) {
        return new Hippo.ChannelManager.TemplateComposer.PageModelStore({
            rootComponentIdentifier: this.rootComponentIdentifier,
            mountId: mountId,
            pageId: pageId,
            composerRestMountUrl: this.composerRestMountUrl,
            resources: this.resources,
            listeners: {
                write : {
                    fn: function(store, action, result, res, records) {
                        if (action == 'create') {
                            records = Ext.isArray(records) ? records : [records];
                            for (var i = 0; i < records.length; i++) {
                                var record = records[i];
                                if (record.get('type') == HST.CONTAINERITEM) {
                                    //add element to the iframe DOM
                                    this.sendFrameMessage({parentId: record.get('parentId'), element: record.get('element')}, 'add');

                                    //add id to parent children map
                                    var parentId = record.get('parentId');
                                    var parentIndex = store.findExact('id', parentId);
                                    var parentRecord = store.getAt(parentIndex);
                                    var children = parentRecord.get('children');
                                    children.push(record.get('id'));
                                    parentRecord.set('children', children);
                                }
                            }
                        } else if (action == 'update') {
                            if (!this.isReloading) {
                                store.reload();
                                this.isReloading = true;
                            }
                        }
                    },
                    scope: this
                },
                load :{
                    fn : function(store, records, options) {
                        this.isReloading = false;
                        this.shareData();
                    },
                    scope: this
                },
                remove : {
                    fn : function(store, record, index) {

                        if (record.get('type') == HST.CONTAINER) {
                            //remove all children as well
                            Ext.each(record.get('children'), function(id) {
                                var childIndex = store.findExact('id', id);
                                if (childIndex > -1) {
                                    store.removeAt(childIndex);
                                }
                            });
                        } else {
                            //containerItem: unregister from parent
                            var parentRecord = store.getAt(store.findExact('id', record.get('parentId')));
                            if (typeof parentRecord !== 'undefined') {
                                var children = parentRecord.get('children');
                                children.remove(record.get('id'));
                                parentRecord.set('children', children);
                            }
                        }
                        var grid = Ext.getCmp('PageModelGrid');
                        if (grid.getSelectionModel().getSelected() == record) {
                            this.deselect(null, null, record);
                        }
                        this.sendFrameMessage({element: record.data.element}, 'remove');
                    },
                    scope : this
                }
            }
        });
    },

    createMainWindow : function(mountId) {
        var window1 = new Hippo.ux.window.FloatingWindow({
            title: 'Configuration',
            x:10, y: 35,
            width: 310,
            height: 650,
            initRegion: 'right',
            layout: 'border',
            closable: true,
            constrainHeader: true,
            closeAction: 'hide',
            bodyStyle: 'background-color: #ffffff',
            renderTo: Ext.getCmp('Iframe').getEl(),
            constrain: true,
            items: [
                {
                    region: 'north',
                    split:true,
                    layout: 'accordion',
                    height: 300,
                    items:[
                        {
                            xtype: 'h_base_grid',
                            flex:2,
                            id: 'ToolkitGrid',
                            title: this.resources['toolkit-grid-title'],
                            store: this.stores.toolkit,
                            cm: new Ext.grid.ColumnModel({
                                columns: [
                                    { header: this.resources['toolkit-grid-column-header-name'], dataIndex: 'name', id:'name', viewConfig :{width: 40}}
//                                    { header: "Id", dataIndex: 'id', id:'id', viewConfig :{width: 40}},
//                                    { header: "Path", dataIndex: 'path', id:'path', viewConfig :{width: 120}}
                                ],
                                defaults: {
                                    sortable: true,
                                    menuDisabled: true
                                }
                            }),
                            plugins: [
                                Hippo.ChannelManager.TemplateComposer.DragDropOne
                            ]
                        },
                        {
                            xtype: 'h_base_grid',
                            flex: 3,
                            id: 'PageModelGrid',
                            title: this.resources['page-model-grid-title'],
                            store: this.stores.pageModel,
                            sm: new Ext.grid.RowSelectionModel({
                                singleSelect: true,
                                listeners: {
                                    rowselect: {
                                        fn: this.select,
                                        scope: this
                                    },
                                    rowdeselect: {
                                        fn: this.deselect,
                                        scope: this
                                    }
                                }
                            }),
                            cm : new Ext.grid.ColumnModel({
                                columns: [
                                    { header: this.resources['page-model-grid-column-header-name'], dataIndex: 'name', id:'name', viewConfig :{width: 120}},
                                    { header: this.resources['page-model-grid-column-header-type'], dataIndex: 'type', id:'type'},
                                    { header: this.resources['page-model-grid-column-header-template'], dataIndex: 'template', id:'template'}
                                ],
                                defaults: {
                                    sortable: false,
                                    menuDisabled: true
                                }
                            }),
                            menuProvider: this
                        }
                    ]
                },
                {
                    id: 'componentPropertiesPanel',
                    xtype:'h_properties_panel',
                    region: 'center',
                    split: true,
                    resources: this.resources,
                    locale: this.locale,
                    composerRestMountUrl: this.composerRestMountUrl,
                    mountId: mountId
                }
            ]
        });
        return window1;
    },

    shareData : function() {
        this.fireEvent('beforeShareDataWithIFrame');
        console.log('shareData');

        var func = function() {
            if (this.stores.pageModel == null) {
                return;
            }
            var self = this;
            console.log('shareData run');
            var facade = function() {
            };
            facade.prototype = {
                getName : function(id) {
                    var idx = self.stores.pageModel.findExact('id', id);
                    if (idx == -1) {
                        return null;
                    }
                    var record = self.stores.pageModel.getAt(idx);
                    return record.get('name');
                }
            };

            if (this.pageModelFacade == null) {
                this.pageModelFacade = new facade();
            }
            this.sendFrameMessage(this.pageModelFacade, 'sharedata');
            this.fireEvent('afterShareDataWithIFrame');
        }

        if (this.iframeInitialized) {
            func.call(this);
        } else {
            if (this.shareDataIFrameInitializedListener) {
                this.removeListener('iFrameInitialized', this.shareDataIFrameInitializedListener, this);
            }
            this.shareDataIFrameInitializedListener = func;
            this.on('iFrameInitialized', func, this, {single : true});
        }
    },

    handleOnClick : function(element) {
        if (element.getAttribute(HST.ATTR.INHERITED)) {
            return;
        }
        var id = element.getAttribute('id');
        var recordIndex = this.stores.pageModel.findExact('id', id);

        if (recordIndex < 0) {
            console.warn('Handling onClick for element[id=' + id + '] with no record in component store');
            return;
        }

        var sm = Ext.getCmp('PageModelGrid').getSelectionModel();
        if (sm.isSelected(recordIndex)) {
            sm.deselectRow(recordIndex);
        } else {
            sm.selectRow(recordIndex);
        }
    },

    findElement: function(id) {
        var frameDoc = Ext.getCmp('Iframe').getFrameDocument();
        var el = frameDoc.getElementById(id);
        return el;
    },

    select : function(model, index, record) {
        this.sendFrameMessage({element: record.data.element}, 'select');
        if (record.get('type') === HST.CONTAINERITEM) {
            this.showProperties(record);
        }
    },

    deselect : function(model, index, record) {
        this.sendFrameMessage({element: record.data.element}, 'deselect');
        this.hideProperties();
    },

    onRearrangeContainer: function(id, children) {
        var recordIndex = this.stores.pageModel.findExact('id', id);//should probably do this through the selectionModel
        var record = this.stores.pageModel.getAt(recordIndex);
        record.set('children', children);
        record.commit();
    },

    handleReceivedItem : function(containerId, element) {
        //we reload for now so no action here, children value update of containers will take care of it
    },

    sendFrameMessage : function(data, name) {
        Ext.getCmp('Iframe').getFrame().sendMessage(data, name);
    },

    showProperties : function(record) {
        Ext.getCmp('componentPropertiesPanel').reload(record.get('id'), record.get('name'), record.get('path'));
    },

    hideProperties : function() {
        Ext.getCmp('componentPropertiesPanel').removeAll();
    },

    /**
     * ContextMenu provider
     */
    getMenuActions : function(record, selected) {
        var actions = [];
        var store = this.stores.pageModel;
        var type = record.get('type');
        if (type == HST.CONTAINERITEM) {
            actions.push(new Ext.Action({
                text: this.resources['context-menu-action-delete'],
                handler: function() {
                    this.removeByRecord(record)
                },
                scope: this
            }));
        }
        var children = record.get('children');
        if (type == HST.CONTAINER && children.length > 0) {
            actions.push(new Ext.Action({
                text: this.resources['context-menu-delete-items'],
                handler: function() {
                    var msg = this.resources['context-menu-delete-items-message'].format(children.length);
                    Hippo.Msg.confirm(this.resources['context-menu-delete-items-message-title'], msg, function(btn, text) {
                        if (btn == 'yes') {
                            var r = [children.length];
                            Ext.each(children, function(c) {
                                r.push(store.getAt(store.findExact('id', c)));
                            });
                            //it seems that calling store.remove(r) will end up re-calling the destroy api call for
                            //all previous items in r.. maybe a bug, for now do a loop
                            Ext.each(r, store.remove, store);
                            //store.remove(r);
                        }
                    });
                },
                scope: this
            }));
        }
        return actions;
    },

    removeByRecord: function(record) {
        var store = this.stores.pageModel;
        Hippo.Msg.confirm(this.resources['delete-message-title'], this.resources['delete-message'].format(record.get('name')), function(btn, text) {
            if (btn == 'yes') {
                store.remove(record);
            }
        });
    },

    removeByElement : function(element) {
        var store = this.stores.pageModel;
        var index = store.findExact('id', Ext.fly(element).getAttribute(HST.ATTR.ID));
        this.removeByRecord(store.getAt(index))
    },

    /**
     * It's not possible to register message:afterselect style listeners..
     * This should work and I'm probably doing something stupid, but I could not
     * get it to work.. So do like this instead.....
     */
    handleFrameMessages : function(frm, msg) {
        try {
            if (msg.tag == 'rearrange') {
                this.onRearrangeContainer(msg.data.id, msg.data.children);
            } else if (msg.tag == 'onclick') {
                this.handleOnClick(msg.data.element);
            } else if (msg.tag == 'receiveditem') {
                this.handleReceivedItem(msg.data.id, msg.data.element);
            } else if (msg.tag == 'remove') {
                this.removeByElement(msg.data.element);
            } else if (msg.tag == 'afterinit') {
                this.fireEvent('iFrameInitialized', msg.data);
            } else if (msg.tag == 'refresh') {
                this.refreshIframe();
            } else if (msg.tag == 'iframeexception') {
                Hippo.Msg.alert(this.resources['iframe-event-exception-message-title'], msg.data.message, function() {
                    self.refreshIframe.call(self);
                });
            }
        } catch(e) {
            var self = this;
            Hippo.Msg.alert(this.resources['iframe-event-handle-error-title'], this.resources['iframe-event-handle-error'], function() {
                self.refreshIframe.call(self);
            });
            console.error(e);
        }
    }
});

Hippo.ChannelManager.TemplateComposer.RestStore = Ext.extend(Ext.data.Store, {

    constructor : function(config) {

        var reader = new Ext.data.JsonReader({
            successProperty: 'success',
            root: 'data',
            messageProperty: 'message',
            idProperty: 'id'
        }, config.prototypeRecord);

        var writer = new Ext.data.JsonWriter({
            encode: false   // <-- don't return encoded JSON -- causes Ext.Ajax#request to send data using jsonData config rather than HTTP params
        });

        var cfg = {
            restful: true,
            reader: reader,
            writer: writer
        };

        Ext.apply(this, cfg, config);
        Hippo.ChannelManager.TemplateComposer.RestStore.superclass.constructor.call(this, config);
    }
});

Hippo.ChannelManager.TemplateComposer.ToolkitStore = Ext.extend(Hippo.ChannelManager.TemplateComposer.RestStore, {

    constructor : function(config) {

        var proxy = new Ext.data.HttpProxy({
            api: {
                read     : config.composerRestMountUrl + config.mountId + './toolkit'
                ,create  : '#'
                ,update  : '#'
                ,destroy : '#'
            }
        });

        var cfg = {
            id: 'ToolkitStore',
            proxy: proxy,
            prototypeRecord : Hippo.ChannelManager.TemplateComposer.PageModel.ReadRecord
        };

        Ext.apply(config, cfg);

        Hippo.ChannelManager.TemplateComposer.ToolkitStore.superclass.constructor.call(this, config);
    }
});

Hippo.ChannelManager.TemplateComposer.PageModelStore = Ext.extend(Hippo.ChannelManager.TemplateComposer.RestStore, {

    constructor : function(config) {

        var composerRestMountUrl = config.composerRestMountUrl;

        var proxy = new Ext.data.HttpProxy({
            api: {
                read     : composerRestMountUrl + config.mountId + './pagemodel/'+config.pageId+"/"
                ,create  : '#' // see beforewrite
                ,update  : '#'
                ,destroy : '#'
            },

            listeners : {
                beforeload: {
                    fn: function (store, options) {
                        Hippo.Msg.wait(config.resources['page-model-store-before-load-message']);
                    }
                },
                beforewrite : {
                    fn : function(proxy, action, rs, params) {
                        Hippo.Msg.wait(config.resources['page-model-store-before-write-message']);
                        if (action == 'create') {
                            var prototypeId = rs.get('id');
                            var parentId = rs.get('parentId');
                            proxy.setApi(action, {url: composerRestMountUrl + parentId + './create/' + prototypeId, method: 'POST'});
                        } else if (action == 'update') {
                            //Ext appends the item ID automatically
                            var id = rs.get('id');
                            proxy.setApi(action, {url: composerRestMountUrl + id + './update', method: 'POST'});
                        } else if (action == 'destroy') {
                            //Ext appends the item ID automatically
                            var parentId = rs.get('parentId');
                            proxy.setApi(action, {url: composerRestMountUrl + parentId + './delete', method: 'GET'});
                        }
                    }
                },
                write :{
                    fn: function(store, action, result, res, rs) {
                        Hippo.Msg.hide();
                        Hippo.ChannelManager.TemplateComposer.Instance.refreshIframe();
                    }
                },
                load : {
                    fn: function (store, records, options) {
                        Hippo.Msg.hide();
                    }
                }
            }
        });
        var cfg = {
            id: 'PageModelStore',
            proxy: proxy,
            prototypeRecord : Hippo.ChannelManager.TemplateComposer.PageModel.ReadRecord
        };

        Ext.apply(config, cfg);

        Hippo.ChannelManager.TemplateComposer.PageModelStore.superclass.constructor.call(this, config);
    }
});


Hippo.ChannelManager.TemplateComposer.DragDropOne = (function() {

    return {

        init: function(c) {
            c.onRender = c.onRender.createSequence(this.onRender);
        },

        onRender: function() {
            var miframePanel = Ext.getCmp('Iframe');
            var miframe = miframePanel.getFrame();

            this.iFramePosition = miframePanel.getPosition();

            this.boxs = [];
            this.nodeOverRecord = null;
            var self = this;

            this.dragZone = new Ext.grid.GridDragZone(this, {
                containerScroll: true,
                ddGroup: 'blabla',

                onInitDrag : function() {
                    var framePanel = Ext.getCmp('Iframe');
                    var frmDoc = framePanel.getFrameDocument();
                    framePanel.getFrame().sendMessage({groups: 'dropzone'}, 'highlight');
                    Hippo.ChannelManager.TemplateComposer.Instance.stores.pageModel.each(function(record) {
                        var type = record.get('type');
                        if (record.get('type') === HST.CONTAINER) {
                            var id = record.get('id');
                            var el = frmDoc.getElementById(id + '-overlay');
                            if (el != null && !frmDoc.getElementById(id).getAttribute(HST.ATTR.INHERITED)) {
                                var box = Ext.Element.fly(el).getBox();
                                self.boxs.push({record: record, box: box});
                            }
                        }
                    });
                    Ext.ux.ManagedIFrame.Manager.showShims();
                },

                onEndDrag : function() {
                    self.boxs = [];
                    Ext.ux.ManagedIFrame.Manager.hideShims();
                    Ext.getCmp('Iframe').getFrame().sendMessage({groups: 'dropzone'}, 'unhighlight');
                }
            });

            var containerItemsGrid = this;
            this.dropZone = new Ext.dd.DropZone(miframePanel.body.dom, {
                ddGroup: 'blabla',

                //If the mouse is over a grid row, return that node. This is
                //provided as the "target" parameter in all "onNodeXXXX" node event handling functions
                getTargetFromEvent : function(e) {
                    return e.getTarget();
                },

                //While over a target node, return the default drop allowed class which
                //places a "tick" icon into the drag proxy.
                onNodeOver : function(target, dd, e, data) {
                    var curX = dd.lastPageX + dd.deltaX - self.iFramePosition[0];
                    var curY = dd.lastPageY + dd.deltaY - self.iFramePosition[1];
                    //TODO: implement dynamic fetch of toolbar height to adjust pageY
                    curY -= 27;

                    for (var i = 0; i < self.boxs.length; i++) {
                        var item = self.boxs[i], box = item.box;
                        if (curX >= box.x && curX <= box.right && curY >= box.y && curY <= box.bottom) {
                            self.nodeOverRecord = item.record;
                            return Ext.dd.DropZone.prototype.dropAllowed;
                        }
                    }
                    self.nodeOverRecord = null;
                    return Ext.dd.DropZone.prototype.dropNotAllowed;
                },

                //On node drop we can interrogate the target to find the underlying
                //application object that is the real target of the dragged data.
                //In this case, it is a Record in the GridPanel's Store.
                //We can use the data set up by the DragZone's getDragData method to read
                //any data we decided to attach in the DragZone's getDragData method.
                onNodeDrop : function(target, dd, e, data) {
                    //                    var rowIndex = this.getView().findRowIndex(target);
                    //                    var r = this.getStore().getAt(rowIndex);
                    //                    Ext.Msg.alert('Drop gesture', 'Dropped Record id ' + data.draggedRecord.id +
                    //                            ' on Record id ' + r.id);
                    if (self.nodeOverRecord != null) {
                        var selections = containerItemsGrid.getSelectionModel().getSelections();

                        var pmGrid = Ext.getCmp('PageModelGrid');
                        var pmRecord = self.nodeOverRecord;
                        var pmStore = pmGrid.getStore();
                        var parentId = pmRecord.get('id');

                        var models = [];
                        var offset = pmRecord.data.children.length + 1;
                        var at = pmStore.indexOf(pmRecord) + offset;
                        for (var i = 0; i < selections.length; i++) {
                            var record = selections[i];
                            var cfg = {
                                parentId: parentId,
                                //we set the id of new types to the id of their prototype, this allows use
                                //to change the rest-api url for the create method, which should contain this
                                //id
                                id : record.get('id'),
                                name: null,
                                type: HST.CONTAINERITEM,
                                template: record.get('template'),
                                componentClassName : record.get('componentClassName'),
                                xtype: record.get('xtype')
                            };
                            var model = Hippo.ChannelManager.TemplateComposer.PageModel.Factory.createModel(null, cfg);
                            models.push(model);
                            pmStore.insert(at + i, Hippo.ChannelManager.TemplateComposer.PageModel.Factory.createRecord(model));
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    };

})();

/**
 * Hippo.Msg is decorating Ext.Msg and makes alert, confirm and prompt blocking.
 * If a blocking message is waiting for user interaction, new fired messages will be queued
 * (e. g. important for asynchronous requests which could trigger hide while we are waiting for user input).
 */
Hippo.Msg = (function() {
    var msgQueue = [];

    var blockingType = [];
    blockingType['alert'] = true;
    blockingType['confirm'] = true;
    blockingType['prompt'] = true;
    blockingType['show'] = false;
    blockingType['wait'] = false;
    blockingType['hide'] = false;
    var blocked = false;
    var waitTimeout;

    var func = function(type, args) {
        if (blocked) {
            if (blockingType[type]) {
                msgQueue.push(function() {
                    func.apply(this, args);
                });
            }
            return;
        }
        if (blockingType[type]) {
            blocked = true;
            if (args.length >= 3) {
                var oldFunction = args[2];
                var scope = this;
                if (args.length >= 4) {
                    scope = args[3];
                }
                args[2] = function() {
                    oldFunction.apply(scope, arguments);
                    blocked = false;
                    if (msgQueue.length > 0) {
                        var nextMessage = msgQueue.shift();
                        nextMessage();
                    }
                }
            }
        }
        Ext.Msg[type].apply(Ext.Msg, args);
    };

    return {
        alert : function() {
            func('alert', arguments);
        },
        confirm : function() {
            func('confirm', arguments);
        },
        prompt : function() {
            func('prompt', aguments);
        },
        show : function() {
            func('show', arguments);
        },
        wait : function() {
            if (waitTimeout) {
                return;
            }
            var args = arguments;
            waitTimeout = window.setTimeout(function() {
                waitTimeout = null;
                func('wait', args);
            }, 500);
        },
        hide : function() {
            if (waitTimeout) {
                console.log('clear wait timeout');
                window.clearTimeout(waitTimeout);
                waitTimeout = null;
            }
            func('hide', arguments);
        }
    }
})();
