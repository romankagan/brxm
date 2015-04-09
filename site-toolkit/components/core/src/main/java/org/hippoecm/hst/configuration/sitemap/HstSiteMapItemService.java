/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.sitemap;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.Sets;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.internal.ConfigurationLockInfo;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.site.MountSiteMapConfiguration;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.core.internal.CollectionOptimizer;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.core.util.PropertyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.ANY;
import static org.hippoecm.hst.configuration.HstNodeTypes.EDITABLE_PROPERTY_STATE;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROEPRTY_SCHEME_AGNOSTIC;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_CACHEABLE;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCALE;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_ABSTRACTPAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMAPITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PAGE_TITLE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_AUTHENTICATED;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENT_CONFIG_MAPPING_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENT_CONFIG_MAPPING_VALUES;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_CONTAINER_RESOURCE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_ERRORCODE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_HIDDEN_IN_CHANNEL_MANAGER;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_NAMEDPIPELINE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_REF_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_RESOURCE_BUNDLE_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_ROLES;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_SCHEME;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_SITEMAPITEMHANDLERIDS;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_STATUSCODE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_USERS;
import static org.hippoecm.hst.configuration.HstNodeTypes.WILDCARD;


public class HstSiteMapItemService implements HstSiteMapItem, CanonicalInfo, ConfigurationLockInfo
{

    private static final Logger log = LoggerFactory.getLogger(HstSiteMapItemService.class);

    private static final String PARENT_PROPERTY_PLACEHOLDER = "${parent}";
    public static final String CONTAINER_RESOURCE_PIPELINE_NAME = "ContainerResourcePipeline";
    public static final String WEB_FILE_PIPELINE_NAME = "WebFilePipeline";

    public static final Set<String> CDN_SUPPORTED_PIPELINES = Sets.newHashSet(CONTAINER_RESOURCE_PIPELINE_NAME, WEB_FILE_PIPELINE_NAME);

    private Map<String, HstSiteMapItem> childSiteMapItems = new HashMap<String, HstSiteMapItem>();

    private Map<String, HstSiteMapItemHandlerConfiguration> siteMapItemHandlerConfigurations = new LinkedHashMap<>();

    private String id;

    private final String canonicalIdentifier;
    private final String canonicalPath;
    private String lockedBy;
    private Calendar lockedOn;

    private final boolean workspaceConfiguration;

    // note refId is frequently just null. Only when it is configured, it is not null. The id is however never null!
    private String refId;

    private String pageTitle;

    private String value;

    /**
     * The locale for this HstSiteMapItem. When the backing configuration does not contain a locale, it is taken from the parent {@link HstSiteMapItem} if there is
     * a parent. If there is no parent, we inherit the locale from the {@link Mount#getLocale()} for this item. The locale can be <code>null</code>
     */
    private String locale;

    private int statusCode;

    private int errorCode;

    private String parameterizedPath;

    private int occurences;

    private String relativeContentPath;

    /**
     * Default componentConfigurationId which is used when there is no more
     * specific componentId to be used through the componentConfigurationIdMappings
     */
    private String componentConfigurationId;

    /**
     * Mapping from primary nodeytypes to more specific component ids
     */
    private Map<String, String> componentConfigurationIdMappings;

    private boolean authenticated;

    private Set<String> roles;

    private Set<String> users;

    private boolean isExcludedForLinkRewriting;

    private boolean isWildCard;

    private boolean isAny;

    private String namedPipeline;

    /*
     * Internal only: used for linkrewriting: when true, it indicates, that this HstSiteMapItem can only be used in linkrewriting
     * when the current context helps to resolve some wildcards
     */
    private boolean useableInRightContextOnly;
    /*
     * Internal only: needed for context aware linkrewriting
     */
    private Map<String, String> keyToPropertyPlaceHolderMap = new HashMap<String,String>(0);

    private int depth;

    private HstSiteMap hstSiteMap;

    private HstSiteMapItemService parentItem;

    private Map<String,String> parameters = new HashMap<String,String>();
    private Map<String,String> localParameters = new HashMap<String,String>();

    private List<HstSiteMapItemService> containsWildCardChildSiteMapItems = new ArrayList<HstSiteMapItemService>();
    private List<HstSiteMapItemService> containsAnyChildSiteMapItems = new ArrayList<HstSiteMapItemService>();
    private boolean containsAny;
    private boolean containsWildCard;
    private boolean isExplicitItem;
    private String postfix;
    private String extension;
    private String prefix;
    private final boolean cacheable;
    private String scheme;
    private boolean schemeAgnostic;
    private int schemeNotMatchingResponseCode = -1;
    private final String [] resourceBundleIds;
    private boolean containerResource;
    private boolean hiddenInChannelManager;

    HstSiteMapItemService(final HstNode node,
                          final MountSiteMapConfiguration mountSiteMapConfiguration,
                          final HstSiteMapItemHandlersConfiguration siteMapItemHandlersConfiguration,
                          final HstSiteMapItem parentItem, HstSiteMap hstSiteMap,
                          final int depth) throws ModelLoadingException {
        this.parentItem = (HstSiteMapItemService)parentItem;
        this.hstSiteMap = hstSiteMap;
        this.depth = depth;
        canonicalIdentifier = node.getValueProvider().getIdentifier();
        canonicalPath = StringPool.get(node.getValueProvider().getPath());
        workspaceConfiguration = ConfigurationUtils.isWorkspaceConfig(node);
        lockedBy = node.getValueProvider().getString(GENERAL_PROPERTY_LOCKED_BY);
        lockedOn = node.getValueProvider().getDate(GENERAL_PROPERTY_LOCKED_ON);

        // the id is the relative path below the root sitemap node. You cannot do a substring on the value provider getPath because due to inheritance
        // there can be completely different paths for the root sitemap node than for the inherited sitemap items.
        HstNode crNode = node;
        StringBuilder idBuilder = new StringBuilder("/").append(crNode.getValueProvider().getName());
        while(crNode.getParent().getNodeTypeName().equals(NODETYPE_HST_SITEMAPITEM)) {
            crNode = crNode.getParent();
            idBuilder.insert(0, crNode.getValueProvider().getName()).insert(0, "/");
        }
        // we take substring(1) to remove the first slash
        id = StringPool.get(idBuilder.toString().substring(1));

        // currently, the value is always the nodename
        value = StringPool.get(node.getValueProvider().getName());

        if(node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_REF_ID)) {
            refId = StringPool.get(node.getValueProvider().getString(SITEMAPITEM_PROPERTY_REF_ID));
        }

        if(node.getValueProvider().hasProperty(SITEMAPITEM_PAGE_TITLE)) {
            pageTitle = StringPool.get(node.getValueProvider().getString(SITEMAPITEM_PAGE_TITLE));
        }

        statusCode = node.getValueProvider().getLong(SITEMAPITEM_PROPERTY_STATUSCODE).intValue();
        errorCode = node.getValueProvider().getLong(SITEMAPITEM_PROPERTY_ERRORCODE).intValue();

        if(parentItem != null) {
            parameterizedPath = this.parentItem.getParameterizedPath()+"/";
            occurences = this.parentItem.getWildCardAnyOccurences();
        } else {
            parameterizedPath = "";
        }
        if(WILDCARD.equals(value)) {
            occurences++;
            parameterizedPath = parameterizedPath + "${" + occurences + "}";
            isWildCard = true;
        } else if(ANY.equals(value)) {
            occurences++;
            parameterizedPath = parameterizedPath + "${" + occurences + "}";
            isAny = true;
        } else if(value.contains(WILDCARD)) {
            containsWildCard = true;
            postfix = value.substring(value.indexOf(WILDCARD) + WILDCARD.length());
            prefix = value.substring(0, value.indexOf(WILDCARD));
            if(postfix.contains(".")) {
                extension = postfix.substring(postfix.indexOf("."));
            }
            if(parentItem != null) {
                ((HstSiteMapItemService)parentItem).addWildCardPrefixedChildSiteMapItems(this);
            }
            occurences++;
            parameterizedPath = parameterizedPath + value.replace(WILDCARD, "${"+occurences+"}" );
        } else if(value.contains(ANY)) {
            containsAny = true;
            postfix = value.substring(value.indexOf(ANY) + ANY.length());
            if(postfix.contains(".")) {
                extension = postfix.substring(postfix.indexOf("."));
            }
            prefix = value.substring(0, value.indexOf(ANY));
            if(parentItem != null) {
                ((HstSiteMapItemService)parentItem).addAnyPrefixedChildSiteMapItems(this);
            }
            occurences++;
            parameterizedPath = parameterizedPath + value.replace(ANY, "${"+occurences+"}" );
        }
        else {
            parameterizedPath = parameterizedPath + value;
        }

        isExplicitItem = !(isWildCard || isAny || containsWildCard || containsAny);

        parameterizedPath = StringPool.get(parameterizedPath);
        prefix = StringPool.get(prefix);
        postfix = StringPool.get(postfix);
        extension = StringPool.get(extension);

        Properties mountParameters = new Properties();
        for (Map.Entry<String, String> entry : mountSiteMapConfiguration.getParameters().entrySet()) {
            mountParameters.setProperty(entry.getKey(), entry.getValue());
        }
        PropertyParser mountParameterParser = new PropertyParser(mountParameters,
                PropertyParser.DEFAULT_PLACEHOLDER_PREFIX, PropertyParser.DEFAULT_PLACEHOLDER_SUFFIX,
                PropertyParser.DEFAULT_VALUE_SEPARATOR,
                true);

        String[] parameterNames = node.getValueProvider().getStrings(GENERAL_PROPERTY_PARAMETER_NAMES);
        String[] parameterValues = node.getValueProvider().getStrings(GENERAL_PROPERTY_PARAMETER_VALUES);

        //componentConfigurationIdMappings
        String[] componentConfigurationNames = node.getValueProvider().getStrings(SITEMAPITEM_PROPERTY_COMPONENT_CONFIG_MAPPING_NAMES);
        String[] componentConfigurationValues = node.getValueProvider().getStrings(SITEMAPITEM_PROPERTY_COMPONENT_CONFIG_MAPPING_VALUES);

        if(componentConfigurationNames != null && componentConfigurationValues != null){
            if(componentConfigurationNames.length != componentConfigurationValues.length) {
                log.warn("Skipping componentConfigurationMappings for sitemapitem '{}' because they only make sense " +
                        "if there are equal number of names and values", canonicalPath);
            }  else {
                componentConfigurationIdMappings = new HashMap<>();
                for(int i = 0; i < componentConfigurationNames.length ; i++) {
                    this.componentConfigurationIdMappings.put(StringPool.get(componentConfigurationNames[i]), StringPool.get(componentConfigurationValues[i]));
                }
            }
        }

        if(parameterNames != null && parameterValues != null){
           if(parameterNames.length != parameterValues.length) {
               log.warn("Skipping parameters for sitemapitem '{}' because they only make sense if there are equal number of names and values",
                       canonicalPath);
           }  else {
               for (int i = 0; i < parameterNames.length ; i++) {
                   if (parameterValues[i] != null && parameterValues[i].contains("${")) {
                       String resolved = (String) mountParameterParser.resolveProperty(parameterNames[i], parameterValues[i]);
                       if (containsInvalidOrNonIntegerPlaceholders(resolved)) {
                           log.warn("Invalid irreplaceable property placeholder found for parameter name '{}' with value '{}' for " +
                                   "sitemap item '{}'. Setting value for '{}' to null", new String[]{parameterNames[i], parameterValues[i],
                                    id, parameterNames[i]});
                           parameterValues[i] = null;
                       } else {
                           parameterValues[i] = resolved;
                       }
                   }
                   parameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
                   localParameters.put(StringPool.get(parameterNames[i]), StringPool.get(parameterValues[i]));
               }
           }
        }

        if(parentItem != null){
            // add the parent parameters that are not already present
            for(Entry<String, String> parentParam : this.parentItem.getParameters().entrySet()) {
                if(!parameters.containsKey(parentParam.getKey())) {
                    parameters.put(StringPool.get(parentParam.getKey()), StringPool.get(parentParam.getValue()));
                }
            }
        }

        relativeContentPath = node.getValueProvider().getString(SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH);
        if(relativeContentPath != null && relativeContentPath.contains(PARENT_PROPERTY_PLACEHOLDER)) {
             if(parentItem == null || parentItem.getRelativeContentPath() == null) {
                 log.error("Cannot use '{}' for a sitemap item that does not have a parent or a parent without relative content path. Used at: '{}'", PARENT_PROPERTY_PLACEHOLDER, id);
             } else {
                 relativeContentPath = relativeContentPath.replace(PARENT_PROPERTY_PLACEHOLDER, parentItem.getRelativeContentPath());
             }
        }

        if(relativeContentPath != null && relativeContentPath.contains("${")) {
            String resolved = (String) mountParameterParser.resolveProperty("relativeContentPath", relativeContentPath);
            if (containsInvalidOrNonIntegerPlaceholders(resolved)) {
                log.warn("Invalid irreplaceable property placeholder found for hst:relativecontentpath '{}' for sitemap item '{}'." +
                        "Setting relativeContentPath to null", new String[]{relativeContentPath, canonicalPath});
                relativeContentPath = null;
            } else {
                relativeContentPath = resolved;
            }
        }
        if (relativeContentPath != null) {
            relativeContentPath = relativeContentPath.trim();
        }
        relativeContentPath = StringPool.get(relativeContentPath);

        this.componentConfigurationId = node.getValueProvider().getString(SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID);
        if (componentConfigurationId != null && componentConfigurationId.contains("${")) {
            String resolved = (String) mountParameterParser.resolveProperty("componentConfigurationId", componentConfigurationId);
            if (containsInvalidOrNonIntegerPlaceholders(resolved)) {
                log.warn("Invalid irreplaceable property placeholder found for {} '{}' for sitemap item '{}'. " +
                        "Setting relativeContentPath to null", new String[]{SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID,
                        relativeContentPath, canonicalPath});
                componentConfigurationId = null;
            } else {
                componentConfigurationId = resolved;
            }
        }
        componentConfigurationId = StringPool.get(componentConfigurationId);

        if (componentConfigurationId != null && componentConfigurationId.startsWith(NODENAME_HST_ABSTRACTPAGES) ) {
            log.warn("Invalid {} '{}' for '{}' because a sitemap item should not reference an " +
                    "abstract page.",
                    new String[] {SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, componentConfigurationId, canonicalPath});
        }

        String[] siteMapItemHandlerIds = node.getValueProvider().getStrings(SITEMAPITEM_PROPERTY_SITEMAPITEMHANDLERIDS);
        if (ArrayUtils.isEmpty(siteMapItemHandlerIds)) {
            siteMapItemHandlerIds = mountSiteMapConfiguration.getDefaultSiteMapItemHandlerIds();
        }
        if(siteMapItemHandlerIds != null && siteMapItemHandlersConfiguration != null) {
            for(String handlerId : siteMapItemHandlerIds) {
                HstSiteMapItemHandlerConfiguration handlerConfiguration = siteMapItemHandlersConfiguration.getSiteMapItemHandlerConfiguration(handlerId);
                if(handlerConfiguration == null) {
                    log.error("Incorrect configuration: SiteMapItem '{}' contains a handlerId '{}' which cannot be found in the siteMapItemHandlers configuration. The handler will be ignored", getQualifiedId(), handlerId);
                } else {
                    this.siteMapItemHandlerConfigurations.put(StringPool.get(handlerId), handlerConfiguration);
                }
            }
        }

        if (node.getValueProvider().hasProperty(GENERAL_PROPERTY_LOCALE)) {
            locale = node.getValueProvider().getString(GENERAL_PROPERTY_LOCALE);
        } else if(parentItem != null){
            locale = parentItem.getLocale();
        } else {
            locale = mountSiteMapConfiguration.getLocale();
        }
        locale = StringPool.get(locale);

        if (node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_AUTHENTICATED)) {
            authenticated = node.getValueProvider().getBoolean(SITEMAPITEM_PROPERTY_AUTHENTICATED);
        } else if(this.parentItem != null){
            authenticated = parentItem.isAuthenticated();
        }

        if (node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_ROLES)) {
            String [] rolesProp = node.getValueProvider().getStrings(SITEMAPITEM_PROPERTY_ROLES);
            roles = new HashSet<>();
            CollectionUtils.addAll(this.roles, rolesProp);
        } else if (this.parentItem != null){
            roles = new HashSet<>(parentItem.getRoles());
        } else {
            roles = new HashSet<>(0);
        }

        if (node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_USERS)) {
            String [] usersProp = node.getValueProvider().getStrings(SITEMAPITEM_PROPERTY_USERS);
            users = new HashSet<>();
            CollectionUtils.addAll(this.users, usersProp);
        } else if (parentItem != null){
            users = new HashSet<>(parentItem.getUsers());
        } else {
            users = new HashSet<>();
        }

        if (node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_CONTAINER_RESOURCE)) {
            containerResource = node.getValueProvider().getBoolean(SITEMAPITEM_PROPERTY_CONTAINER_RESOURCE);
        } else if(parentItem != null) {
            containerResource = parentItem.isContainerResource();
        }

        if (containerResource) {
            if (!this.canonicalPath.contains("/"+HstNodeTypes.NODENAME_HST_HSTDEFAULT + "/" + HstNodeTypes.NODENAME_HST_SITEMAP + "/")) {
                final String msg = String.format("Invalid sitemap item configuration for '%s'. A sitemap item is only " +
                        "allowed to be marked with '%s = true' if the sitemap item is located in '%s'.",
                        canonicalPath, HstNodeTypes.SITEMAPITEM_PROPERTY_CONTAINER_RESOURCE,
                        "/"+HstNodeTypes.NODENAME_HST_HSTDEFAULT + "/" + HstNodeTypes.NODENAME_HST_SITEMAP + "/" );
                throw new ModelLoadingException(msg);
            }
            log.info("Sitemap item '{}' is a container resource item. Default the properties '{}' will be" +
                            " set to '{}', '{}' = true and '{}' = true, unless the properties are explicitly configured on this item" +
                            " or a parent item to have a different value",
                    canonicalPath, SITEMAPITEM_PROPERTY_NAMEDPIPELINE, CONTAINER_RESOURCE_PIPELINE_NAME,
                    SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING, GENERAL_PROEPRTY_SCHEME_AGNOSTIC
            );

            isExcludedForLinkRewriting = true;
            schemeAgnostic = true;
            namedPipeline = CONTAINER_RESOURCE_PIPELINE_NAME;
        }

        if (node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_HIDDEN_IN_CHANNEL_MANAGER)) {
            hiddenInChannelManager = node.getValueProvider().getBoolean(SITEMAPITEM_PROPERTY_HIDDEN_IN_CHANNEL_MANAGER);
        } else if(parentItem != null) {
            hiddenInChannelManager = parentItem.isHiddenInChannelManager();
        }

        if(node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING)) {
            isExcludedForLinkRewriting = node.getValueProvider().getBoolean(SITEMAPITEM_PROPERTY_EXCLUDEDFORLINKREWRITING);
        } else if (parentItem != null) {
            isExcludedForLinkRewriting = parentItem.isExcludedForLinkRewriting();
        }

        if(node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_NAMEDPIPELINE)) {
            namedPipeline = node.getValueProvider().getString(SITEMAPITEM_PROPERTY_NAMEDPIPELINE);
        } else if (parentItem != null) {
            namedPipeline = parentItem.getNamedPipeline();
        }

        if (namedPipeline == null) {
            // inherit the namedPipeline from the mount (can be null)
            namedPipeline = mountSiteMapConfiguration.getNamedPipeline();
        }

        namedPipeline = StringPool.get(namedPipeline);

        if(node.getValueProvider().hasProperty(GENERAL_PROPERTY_CACHEABLE)) {
            cacheable = node.getValueProvider().getBoolean(GENERAL_PROPERTY_CACHEABLE);
        } else if(this.parentItem != null) {
            cacheable = parentItem.isCacheable();
        } else {
            cacheable = mountSiteMapConfiguration.isCacheable();
        }

        scheme = null;
        if (node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_SCHEME)) {
            scheme = StringPool.get(node.getValueProvider().getString(SITEMAPITEM_PROPERTY_SCHEME));
        }
        if (StringUtils.isBlank(scheme)) {
            scheme = parentItem != null ? parentItem.getScheme() : mountSiteMapConfiguration.getScheme();
        }

        if(node.getValueProvider().hasProperty(GENERAL_PROEPRTY_SCHEME_AGNOSTIC)) {
            schemeAgnostic = node.getValueProvider().getBoolean(GENERAL_PROEPRTY_SCHEME_AGNOSTIC);
        } else if (parentItem != null) {
            schemeAgnostic =  parentItem.isSchemeAgnostic();
        } else if (!containerResource) {
            schemeAgnostic = mountSiteMapConfiguration.isSchemeAgnostic();
        }

        if(node.getValueProvider().hasProperty(GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE)) {
            schemeNotMatchingResponseCode = (int)node.getValueProvider().getLong(GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE).longValue();
            if (!ConfigurationUtils.isSupportedSchemeNotMatchingResponseCode(schemeNotMatchingResponseCode)) {
                log.warn("Invalid '{}' configured on '{}'. Use inherited value. Supported values are '{}'", new String[]{GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE,
                        node.getValueProvider().getPath(), ConfigurationUtils.suppertedSchemeNotMatchingResponseCodesAsString()});
                schemeNotMatchingResponseCode = -1;
            }
        }
        if (schemeNotMatchingResponseCode == -1) {
            schemeNotMatchingResponseCode = parentItem != null ?
                    parentItem.getSchemeNotMatchingResponseCode() : mountSiteMapConfiguration.getSchemeNotMatchingResponseCode();
        }

        if (node.getValueProvider().hasProperty(SITEMAPITEM_PROPERTY_RESOURCE_BUNDLE_ID)) {
            resourceBundleIds = StringUtils.split(StringPool.get(node.getValueProvider().getString(SITEMAPITEM_PROPERTY_RESOURCE_BUNDLE_ID)), " ,\t\f\r\n");
        } else {
            resourceBundleIds = parentItem != null ? parentItem.getResourceBundleIds() : mountSiteMapConfiguration.getDefaultResourceBundleIds();
        }

        for(HstNode child : node.getNodes()) {
            if ("deleted".equals(child.getValueProvider().getString(EDITABLE_PROPERTY_STATE))) {
                log.debug("SKipping marked deleted node {}", child.getValueProvider().getPath());
                continue;
            }
            if(NODETYPE_HST_SITEMAPITEM.equals(child.getNodeTypeName())) {
                try {
                    HstSiteMapItemService siteMapItemService = new HstSiteMapItemService(child, mountSiteMapConfiguration,  siteMapItemHandlersConfiguration , this, this.hstSiteMap, depth + 1);
                    childSiteMapItems.put(siteMapItemService.getValue(), siteMapItemService);
                } catch (ModelLoadingException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Skipping root sitemap '{}'", child.getValueProvider().getPath(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Skipping root sitemap '{}'", child.getValueProvider().getPath());
                    }
                }
            }
        }
    }

    public HstSiteMapItem getChild(String value) {
        return childSiteMapItems.get(value);
    }



    public List<HstSiteMapItem> getChildren() {
        return Collections.unmodifiableList(new ArrayList<>(childSiteMapItems.values()));
    }

    public String getComponentConfigurationId() {
        return componentConfigurationId;
    }

    @Override
    public Map<String, String> getComponentConfigurationIdMappings() {
        return componentConfigurationIdMappings;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getCanonicalIdentifier() {
        return canonicalIdentifier;
    }

    @Override
    public String getCanonicalPath() {
        return canonicalPath;
    }

    @Override
    public boolean isWorkspaceConfiguration() {
        return workspaceConfiguration;
    }

    @Override
    public String getLockedBy() {
        return lockedBy;
    }

    @Override
    public Calendar getLockedOn() {
        return lockedOn;
    }

    public String getRefId() {
        return refId;
    }

    public String getRelativeContentPath() {
        return relativeContentPath;
    }


    public String getParameter(String name) {
        return parameters.get(name);
    }


    public Map<String, String> getParameters() {
        return parameters;
    }


	public String getLocalParameter(String name) {
		return localParameters.get(name);
	}

	public Map<String, String> getLocalParameters() {
		return localParameters;
	}


	public HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(String handlerId) {
	    return siteMapItemHandlerConfigurations.get(handlerId);
	}

    public List<HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations() {
        return Collections.unmodifiableList(new ArrayList<>(siteMapItemHandlerConfigurations.values()));
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getUsers() {
        return users;
    }

    public String getValue() {
        return value;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getLocale() {
        return locale;
    }

    public boolean isWildCard() {
        return isWildCard;
    }

    public boolean isAny() {
        return isAny;
    }

    public HstSiteMap getHstSiteMap() {
        return hstSiteMap;
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public boolean isSchemeAgnostic() {
        return schemeAgnostic;
    }

    @Override
    public int getSchemeNotMatchingResponseCode() {
        return schemeNotMatchingResponseCode;
    }

    @Override
    public String getResourceBundleId() {
        if (resourceBundleIds == null || resourceBundleIds.length == 0) {
            return null;
        }

        return resourceBundleIds[0];
    }

    @Override
    public String [] getResourceBundleIds() {
        if (resourceBundleIds == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        return (String[]) ArrayUtils.clone(resourceBundleIds);
    }

    @Override
    public boolean isContainerResource() {
        return containerResource;
    }

    @Override
    public boolean isHiddenInChannelManager() {
        return hiddenInChannelManager;
    }

    public HstSiteMapItem getParentItem() {
        return parentItem;
    }

    public String getParameterizedPath(){
        return parameterizedPath;
    }

    public int getWildCardAnyOccurences(){
        return occurences;
    }


    // ---- BELOW FOR INTERNAL CORE SITEMAP MAP RESOLVING && LINKREWRITING ONLY

    public void addWildCardPrefixedChildSiteMapItems(HstSiteMapItemService hstSiteMapItem){
        containsWildCardChildSiteMapItems.add(hstSiteMapItem);
    }

    public void addAnyPrefixedChildSiteMapItems(HstSiteMapItemService hstSiteMapItem){
        containsAnyChildSiteMapItems.add(hstSiteMapItem);
    }

    public HstSiteMapItem getWildCardPatternChild(String value, List<HstSiteMapItem> excludeList){
        if(value == null || containsWildCardChildSiteMapItems.isEmpty()) {
            return null;
        }
        return match(value, containsWildCardChildSiteMapItems, excludeList);
    }

    public HstSiteMapItem getAnyPatternChild(String[] elements, int position, List<HstSiteMapItem> excludeList){
        if(value == null || containsAnyChildSiteMapItems.isEmpty()) {
            return null;
        }
        StringBuilder remainder = new StringBuilder(elements[position]);
        while(++position < elements.length) {
            remainder.append("/").append(elements[position]);
        }
        return match(remainder.toString(), containsAnyChildSiteMapItems, excludeList);
    }


    public boolean patternMatch(String value, String prefix, String postfix ) {
     // postFix must match
        if(prefix != null && !"".equals(prefix)){
            if(prefix.length() >= value.length()) {
                // can never match
                return false;
            }
            if(!value.substring(0, prefix.length()).equals(prefix)){
                // wildcard prefixed sitemap does not match the prefix. we can stop
                return false;
            }
        }
        if(postfix != null && !"".equals(postfix)){
            if(postfix.length() >= value.length()) {
                // can never match
                return false;
            }
            if(!value.substring(value.length() - postfix.length()).equals(postfix)){
                // wildcard prefixed sitemap does not match the postfix . we can stop
                return false;
            }
        }
        // if we get here, the pattern matched
        return true;
    }

    private HstSiteMapItem match(String value, List<HstSiteMapItemService> patternSiteMapItems, List<HstSiteMapItem> excludeList) {

        for(HstSiteMapItemService item : patternSiteMapItems){
            // if in exclude list, go to next
            if(excludeList.contains(item)) {
                continue;
            }

            if(patternMatch(value, item.getPrefix(),  item.getPostfix())) {
                return item;
            }

        }
        return null;
    }


    public String getNamedPipeline() {
        return namedPipeline;
    }

    public String getPostfix(){
        return postfix;
    }

    public String getExtension(){
        return extension;
    }

    public String getPrefix(){
        return prefix;
    }

    public boolean containsWildCard() {
        return containsWildCard;
    }

    public boolean containsAny() {
        return containsAny;
    }

    @Override
    public boolean isExplicitElement() {
        return isExplicitItem;
    }

    @Override
    public boolean isExplicitPath() {
        return  parentItem == null ? isExplicitElement() : isExplicitElement() && parentItem.isExplicitPath();
    }

    public void setUseableInRightContextOnly(boolean useableInRightContextOnly) {
        this.useableInRightContextOnly = useableInRightContextOnly;
    }

    public boolean isUseableInRightContextOnly() {
        return useableInRightContextOnly;
    }

    public void setKeyToPropertyPlaceHolderMap(Map<String, String> keyToPropertyPlaceHolderMap) {
       this.keyToPropertyPlaceHolderMap = keyToPropertyPlaceHolderMap;
    }

    public Map<String, String> getKeyToPropertyPlaceHolderMap() {
        return keyToPropertyPlaceHolderMap;
    }

    public int getDepth() {
        return depth;
    }

    public String getQualifiedId() {
        return canonicalPath;
    }

    public boolean isExcludedForLinkRewriting() {
        return isExcludedForLinkRewriting;
    }

    /**
     * returns <code>true</code> if <code>input</code> contains a non integer property placeholder, for example ${foo}.
     * ${1}, ${2} etc are allowed. Note this method does not check broken property place holder values, eg 'foo${' or
     * 'foo${$}}'
     */
    static boolean containsInvalidOrNonIntegerPlaceholders(final String input) {
        if (input == null) {
            return false;
        }
        int nextPlaceholder = input.indexOf("${");
        while (nextPlaceholder >= 0) {
            int start = nextPlaceholder + 2;
            int end = input.indexOf('}', start + 1);
            if (end < 0) {
                return true; // unclosed ${
            }
            for (int i = start; i < end; i++) {
                if (!Character.isDigit(input.charAt(i))) {
                    return true;
                }
            }
            nextPlaceholder = input.indexOf("${", end);
        }
        return false;
    }

    void optimize() {

        childSiteMapItems = CollectionOptimizer.optimizeHashMap(childSiteMapItems);
        siteMapItemHandlerConfigurations = CollectionOptimizer.optimizeLinkedHashMap(siteMapItemHandlerConfigurations);
        componentConfigurationIdMappings = CollectionOptimizer.optimizeHashMap(componentConfigurationIdMappings);
        users = CollectionOptimizer.optimizeHashSet(users);
        roles = CollectionOptimizer.optimizeHashSet(roles);
        parameters = CollectionOptimizer.optimizeHashMap(parameters);
        localParameters = CollectionOptimizer.optimizeHashMap(localParameters);
        containsAnyChildSiteMapItems = CollectionOptimizer.optimizeArrayList(containsAnyChildSiteMapItems);
        containsWildCardChildSiteMapItems = CollectionOptimizer.optimizeArrayList(containsWildCardChildSiteMapItems);

        // optimize all
        for (HstSiteMapItem child : childSiteMapItems.values()) {
            ((HstSiteMapItemService)child).optimize();
        }
    }

    @Override
    public String toString() {
        return "HstSiteMapItemService{" +
                "canonicalPath='" + canonicalPath + '\'' +
                ", site=" + hstSiteMap.getSite() +
                '}';
    }
}
