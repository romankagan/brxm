/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

export class HippoIframeCtrl {
  constructor($rootScope, $element, linkProcessorService, hstCommentsProcessorService, ChannelService,
              PageStructureService, OverlaySyncService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.linkProcessorService = linkProcessorService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.ChannelService = ChannelService;
    this.PageStructureService = PageStructureService;
    this.OverlaySyncService = OverlaySyncService;

    this.iframeJQueryElement = $element.find('iframe');
    this.iframeJQueryElement.on('load', () => this.onLoad());

    OverlaySyncService.init(this.iframeJQueryElement, $element.find('.overlay'));
  }

  onLoad() {
    this.$rootScope.$apply(() => {

      this._parseHstComments();
      this._parseLinks();

      this.OverlaySyncService.startObserving();
    });
  }

  showComponentProperties(structureElement) {
    this.PageStructureService.showComponentProperties(structureElement);
  }

  _parseHstComments() {
    const iframeDom = this.iframeJQueryElement.contents()[0];

    this.PageStructureService.clearParsedElements();
    this.hstCommentsProcessorService.run(iframeDom,
      this.PageStructureService.registerParsedElement.bind(this.PageStructureService));
    this.PageStructureService.printParsedElements();
  }

  _parseLinks() {
    const iframeDom = this.iframeJQueryElement.contents()[0];
    const internalLinkPrefix = `${iframeDom.location.protocol}//${iframeDom.location.host}${this.ChannelService.getUrl()}`;

    this.linkProcessorService.run(iframeDom, internalLinkPrefix);
  }

  getContainers() {
    return this.selectMode ? this.PageStructureService.containers : [];
  }

}
