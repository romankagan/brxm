/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import angular from 'angular';
import 'angular-mocks';

describe('OpenUiService', () => {
  let $log;
  let $q;
  let $rootScope;
  let iframe;
  let ConfigService;
  let ExtensionService;
  let OpenUiService;
  let Penpal;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    iframe = angular.element('<iframe src="about:blank"></iframe>');

    inject((_$log_, _$q_, _$rootScope_, _ConfigService_, _ExtensionService_, _OpenUiService_, _Penpal_) => {
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ConfigService = _ConfigService_;
      ExtensionService = _ExtensionService_;
      OpenUiService = _OpenUiService_;
      Penpal = _Penpal_;

      spyOn(ConfigService, 'getCmsContextPath');
      spyOn(ConfigService, 'getCmsOrigin');
    });
  });

  it('connects to the child', () => {
    const params = {};

    spyOn(Penpal, 'connectToChild').and.returnValue({
      iframe,
      promise: $q.resolve('child'),
    });

    OpenUiService.connect(params);
    $rootScope.$digest();

    expect(iframe).toHaveAttr(
      'sandbox',
      'allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts',
    );
    expect(Penpal.connectToChild).toHaveBeenCalledWith(params);
  });

  describe('initialize', () => {
    let element;
    let extension;
    beforeEach(() => {
      element = {};
      extension = { displayName: 'test-extension' };
      spyOn(ExtensionService, 'getExtension').and.returnValue(extension);
      spyOn(ExtensionService, 'getExtensionUrl').and.returnValue('test-url');
    });

    it('connects to the child', () => {
      spyOn(OpenUiService, 'connect').and.returnValue($q.resolve());
      OpenUiService.initialize('test-id', { appendTo: element });

      expect(ExtensionService.getExtension).toHaveBeenCalledWith('test-id');
      expect(ExtensionService.getExtensionUrl).toHaveBeenCalledWith(extension);
      expect(OpenUiService.connect).toHaveBeenCalledWith(jasmine.objectContaining({
        url: 'test-url',
        appendTo: element,
      }));
    });

    it('logs a warning when connecting to the child failed', () => {
      const error = new Error('Connection destroyed');
      spyOn(OpenUiService, 'connect').and.returnValue($q.reject(error));
      spyOn($log, 'warn');

      OpenUiService.initialize('test-id', { appendTo: element });
      $rootScope.$digest();

      expect(OpenUiService.connect).toHaveBeenCalled();
      expect($log.warn).toHaveBeenCalledWith(
        "Extension 'test-extension' failed to connect with the client library.",
        error,
      );
    });
  });

  describe('getProperties', () => {
    describe('baseUrl', () => {
      it('is set to the base URL of a CMS on localhost', () => {
        ConfigService.getCmsContextPath.and.returnValue('/cms/');
        ConfigService.getCmsOrigin.and.returnValue('http://localhost:8080');
        expect(OpenUiService.getProperties({}).baseUrl).toBe('http://localhost:8080/cms/');
      });

      it('is set to the base URL of a CMS in production', () => {
        ConfigService.getCmsContextPath.and.returnValue('/');
        ConfigService.getCmsOrigin.and.returnValue('https://cms.example.com');
        expect(OpenUiService.getProperties({}).baseUrl).toBe('https://cms.example.com/');
      });
    });

    describe('extension config', () => {
      it('is set to the config string of the extension', () => {
        expect(OpenUiService.getProperties({ config: 'testConfig' }).extension.config).toBe('testConfig');
      });
    });

    describe('locale', () => {
      it('is set to the current CMS locale', () => {
        ConfigService.locale = 'fr';
        expect(OpenUiService.getProperties({}).locale).toBe('fr');
      });
    });

    describe('timeZone', () => {
      it('is set to the current CMS time zone', () => {
        ConfigService.timeZone = 'Europe/Amsterdam';
        expect(OpenUiService.getProperties({}).timeZone).toBe('Europe/Amsterdam');
      });
    });

    describe('in user data', () => {
      it('sets the id to the current CMS user name', () => {
        ConfigService.cmsUser = 'editor';
        expect(OpenUiService.getProperties({}).user.id).toBe('editor');
      });

      it('sets the firstName to the current CMS users first name', () => {
        ConfigService.cmsUserFirstName = 'Ed';
        expect(OpenUiService.getProperties({}).user.firstName).toBe('Ed');
      });

      it('sets the lastName to the current CMS users last name', () => {
        ConfigService.cmsUserLastName = 'Itor';
        expect(OpenUiService.getProperties({}).user.lastName).toBe('Itor');
      });

      it('sets the displayName to the current CMS users display name', () => {
        ConfigService.cmsUserDisplayName = 'Ed Itor';
        expect(OpenUiService.getProperties({}).user.displayName).toBe('Ed Itor');
      });
    });

    describe('version', () => {
      it('is set to the current CMS version', () => {
        ConfigService.cmsVersion = '13.0.0';
        expect(OpenUiService.getProperties({}).version).toBe('13.0.0');
      });
    });
  });
});
