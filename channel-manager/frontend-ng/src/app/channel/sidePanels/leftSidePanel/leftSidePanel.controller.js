/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

class LeftSidePanelCtrl {
  constructor(
    $element,
    $scope,
    localStorageService,
    CatalogService,
    ChannelService,
    SidePanelService,
    SiteMapService,
  ) {
    'ngInject';

    this.$element = $element;
    this.$scope = $scope;
    this.localStorageService = localStorageService;
    this.CatalogService = CatalogService;
    this.ChannelService = ChannelService;
    this.SidePanelService = SidePanelService;
    this.SiteMapService = SiteMapService;

    this.lastSavedWidth = null;
  }

  $onInit() {
    this.lastSavedWidth = this.localStorageService.get('leftSidePanelWidth') || '320px';
    this.sideNavElement = this.$element.find('.left-side-panel');
    this.sideNavElement.css('width', this.lastSavedWidth);
  }

  $postLink() {
    this.SidePanelService.initialize('left', this.$element.find('.left-side-panel'));
  }

  onResize(newWidth) {
    this.lastSavedWidth = `${newWidth}px`;
    this.localStorageService.set('leftSidePanelWidth', this.lastSavedWidth);
  }

  get isOpen() {
    return this.localStorageService.get('leftSidePanelOpen') === true;
  }

  set isOpen(isOpen) {
    if (typeof isOpen === 'boolean') {
      this.localStorageService.set('leftSidePanelOpen', isOpen);
    }
  }

  isLockedOpen() {
    return this.SidePanelService.isOpen('left');
  }

  showComponentsTab() {
    const catalog = this.getCatalog();
    return this.componentsVisible && catalog.length > 0;
  }

  getCatalog() {
    return this.CatalogService.getComponents();
  }

  getSiteMapItems() {
    return this.SiteMapService.get();
  }

  isSidePanelLifted() {
    return this.SidePanelService.isSidePanelLifted;
  }

  isEditable() {
    return this.ChannelService.isEditable();
  }
}

export default LeftSidePanelCtrl;
