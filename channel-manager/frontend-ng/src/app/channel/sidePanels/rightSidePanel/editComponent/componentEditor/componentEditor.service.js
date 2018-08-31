/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const ERROR_MAP = {};

class ComponentEditorService {
  constructor($q, $translate, CmsService, DialogService, FeedbackService, HstComponentService) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.HstComponentService = HstComponentService;
  }

  open({ channel, component, container, page }) {
    this.close();

    return this.HstComponentService.getProperties(component.id, component.variant)
      .then(response => this._onLoadSuccess(channel, component, container, page, response.properties))
      .catch(response => this._onLoadFailure(response));
  }

  _onLoadSuccess(channel, component, container, page, properties) {
    this.channel = channel;
    this.component = component;
    this.container = container;
    this.page = page;
    this.properties = properties;

    console.log('Channel', this.channel);
    console.log('Component', this.component);
    console.log('Component properties', this.properties);
    console.log('Container', this.container);
    console.log('Page', this.page);
  }

  _onLoadFailure(response) {
    this._clearData();

    let errorKey;
    let params = null;

    if (this._isErrorInfo(response.data)) {
      const errorInfo = response.data;
      errorKey = errorInfo.reason;
      params = this._extractErrorParams(errorInfo);

      if (errorInfo.params) {
        this.publicationState = errorInfo.params.publicationState;
      }
    } else if (response.status === 404) {
      errorKey = 'NOT_FOUND';
    } else {
      errorKey = 'UNAVAILABLE';
    }

    this.error = ERROR_MAP[errorKey];
    if (params) {
      this.error.messageParams = params;
    }
  }

  getComponentName() {
    if (this.component) {
      return this.component.label;
    }
    if (this.error && this.error.messageParams) {
      return this.error.messageParams.displayName;
    }
    return undefined;
  }

  close() {
    this._clearData();
    delete this.error;
  }

  _clearData() {
    delete this.channel;
    delete this.component;
    delete this.container;
    delete this.page;
    delete this.properties;
  }
}

export default ComponentEditorService;
