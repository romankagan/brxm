/**
 * @description
 * <p>
 * Provides an interface for components to register on handling WicketAjax 
 * component update
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hashmap
 * @module hippoajax
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.HippoAjax) { // Ensure only one hippo ajax exists
    (function() {
        
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang, tmpFunc;

        YAHOO.hippo.HippoAjaxImpl = function() {};
        
        YAHOO.hippo.HippoAjaxImpl.prototype = {
            prefix : 'hippo-destroyable-',
            callbacks : new YAHOO.hippo.HashMap(),
            _scrollbarWidth : null,
            
            loadJavascript : function(url, callback, scope) {
                var evt = !YAHOO.env.ua.ie ? "onload" : 'onreadystatechange',
                    element = document.createElement("script");
                element.type = "text/javascript";
                element.src = url;
                if (callback) {
                    element[evt] = function() {
                        if (YAHOO.env.ua.ie && ( !( /loaded|complete/.test(window.event.srcElement.readyState) ) )) {
                            return;
                        }
                        callback.call(scope);
                        element[evt] = null;
                    };
                }
                document.getElementsByTagName("head")[0].appendChild(element);
            },

            getScrollbarWidth : function() {
                var inner, outer, w1, w2;

                if(this._scrollbarWidth === null) {
                    inner = document.createElement('p');
                    inner.style.width = "100%";
                    inner.style.height = "200px";

                    outer = document.createElement('div');
                    outer.style.position = "absolute";
                    outer.style.top = "0px";
                    outer.style.left = "0px";
                    outer.style.visibility = "hidden";
                    outer.style.width = "200px";
                    outer.style.height = "150px";
                    outer.style.overflow = "hidden";
                    outer.appendChild (inner);

                    document.body.appendChild (outer);
                    w1 = inner.offsetWidth;
                    outer.style.overflow = 'scroll';
                    w2 = inner.offsetWidth;
                    if (w1 === w2) {
                        w2 = outer.clientWidth;
                    }

                    document.body.removeChild (outer);

                    this._scrollbarWidth = w1 - w2;
                }
                return this._scrollbarWidth;
            },
            
            getScrollbarHeight : function() {
                //I'm lazy so return scrollbarWidth for now
                return this.getScrollbarWidth();
            },
            
            registerDestroyFunction : function(el, func, context, args) {
                var id = this.prefix + Dom.generateId();
                el.HippoDestroyID = id;
                if(!Lang.isArray(args)) {
                    args = [args];
                }
                this.callbacks.put(id, {func: func, context: context, args: args});
            },
            
            callDestroyFunction : function(id) {
                if(this.callbacks.containsKey(id)) {
                    var callback = this.callbacks.remove(id);
                    callback.func.apply(callback.context, callback.args);
                }
            },

            cleanupModal: function(modal) {
                var els, i, len;
                els = YAHOO.util.Dom.getElementsBy(function(node) {
                    return !YAHOO.lang.isUndefined(node.HippoDestroyID);
                }, null, modal.window);

                for (i = 0, len = els.length; i < len; i++) {
                    YAHOO.hippo.HippoAjax.callDestroyFunction(els[i].HippoDestroyID);
                }
            }
        };
        
        YAHOO.hippo.HippoAjax = new YAHOO.hippo.HippoAjaxImpl();
        
        tmpFunc = Wicket.Ajax.Call.prototype.processComponent;
        Wicket.Ajax.Call.prototype.processComponent = function(steps, node) {
            var compId, el, els, i, len;

            compId = node.getAttribute("id");
            el = YAHOO.util.Dom.get(compId);

            if (el !== null && el !== undefined) {
                //console.time("HippoAjax.processComponent.cleanup");
                els = YAHOO.util.Dom.getElementsBy(function(node) {
                    return !YAHOO.lang.isUndefined(node.HippoDestroyID);
                }, null, el);

                for (i = 0, len = els.length; i < len; i++) {
                    YAHOO.hippo.HippoAjax.callDestroyFunction(els[i].HippoDestroyID);
                }
                //console.time('HippoAjax.processComponent.purgeElement');
                YAHOO.util.Event.purgeElement(el, true);
                //console.timeEnd('HippoAjax.processComponent.purgeElement');
                //console.timeEnd("HippoAjax.processComponent.cleanup");
            }
            tmpFunc.call(this, steps, node);
        };

    }());

    YAHOO.register("hippoajax", YAHOO.hippo.HippoAjax, {
        version: "2.8.1", build: "19"
    });
}

var HippoAjax = YAHOO.hippo.HippoAjax;
