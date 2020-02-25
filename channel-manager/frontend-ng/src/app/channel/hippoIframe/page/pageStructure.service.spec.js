/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

function containerComment(label, type, uuid) {
  return `<!-- {
    "HST-Type": "CONTAINER_COMPONENT",
    "HST-Label": "${label}",
    "HST-XType": "${type}",
    "uuid": "${uuid}"
  } -->`;
}

function itemComment(label, uuid) {
  return `<!-- {
    "HST-Type": "CONTAINER_ITEM_COMPONENT",
    "HST-Label": "${label}",
    "uuid": "${uuid}"
  } -->`;
}

function manageContentLinkComment(documentTemplateQuery) {
  return `<!-- {
    "HST-Type": "MANAGE_CONTENT_LINK",
    "documentTemplateQuery": "${documentTemplateQuery}"
  } -->`;
}

function editMenuLinkComment(uuid) {
  return `<!-- {
    "HST-Type": "EDIT_MENU_LINK",
    "uuid": "${uuid}"
  } -->`;
}

function unprocessedHeadContributionsComment(headElements) {
  return `<!-- {
    "HST-Type": "HST_UNPROCESSED_HEAD_CONTRIBUTIONS",
    "headElements": ["${headElements}"]
  } -->`;
}

function endComment(uuid) {
  return `<!-- {
    "HST-End": "true",
    "uuid": "${uuid}"
  } -->`;
}

describe('PageStructureService', () => {
  let $document;
  let $log;
  let $rootScope;
  let $q;

  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let HstCommentsProcessorService;
  let HstComponentService;
  let HstService;
  let MarkupService;
  let ModelFactoryService;
  let PageStructureService;

  let registered;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe.page');

    inject((
      _$document_,
      _$log_,
      _$q_,
      _$rootScope_,
      _$window_,
      _ChannelService_,
      _EditComponentService_,
      _FeedbackService_,
      _HippoIframeService_,
      _HstCommentsProcessorService_,
      _HstComponentService_,
      _HstService_,
      _MarkupService_,
      _ModelFactoryService_,
      _PageStructureService_,
    ) => {
      $document = _$document_;
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      HstCommentsProcessorService = _HstCommentsProcessorService_;
      HstComponentService = _HstComponentService_;
      HstService = _HstService_;
      MarkupService = _MarkupService_;
      ModelFactoryService = _ModelFactoryService_;
      PageStructureService = _PageStructureService_;
    });

    registered = [];

    spyOn(ChannelService, 'recordOwnChange');
    spyOn(HstCommentsProcessorService, 'run').and.returnValue(registered);
  });

  beforeEach(() => {
    jasmine.getFixtures().load('channel/hippoIframe/page/pageStructure.service.fixture.html');
  });

  describe('initially', () => {
    it('should have no page', () => {
      expect(PageStructureService.getPage()).not.toBeDefined();
    });

    it('should have no embedded links', () => {
      expect(PageStructureService.getEmbeddedLinks()).toEqual([]);
    });
  });

  describe('parseElements', () => {
    it('creates a new page from the HST comment elements in the document', () => {
      spyOn(ModelFactoryService, 'createPage');
      const comments = [{ id: 1 }, { id: 2 }];
      HstCommentsProcessorService.run.and.returnValue(comments);
      ModelFactoryService.createPage.and.returnValue('new-page');

      PageStructureService.parseElements(document);

      expect(HstCommentsProcessorService.run).toHaveBeenCalledWith(document);
      expect(ModelFactoryService.createPage).toHaveBeenCalledWith(comments);
      expect(PageStructureService.getPage()).toBe('new-page');
    });

    it('emits event "iframe:page:change" after page elements have been parsed', () => {
      const comment = { json: { id: 1 } };
      HstCommentsProcessorService.run.and.returnValue([comment]);
      const onChange = jasmine.createSpy('on-change');
      const offChange = $rootScope.$on('iframe:page:change', onChange);

      PageStructureService.parseElements(document);
      expect(onChange).toHaveBeenCalled();

      offChange();
    });
  });

  describe('renderComponent', () => {
    it('gracefully handles requests to re-render an undefined or null component', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup');

      $q.all(
        PageStructureService.renderComponent(),
        PageStructureService.renderComponent(null),
      ).then(() => {
        expect(MarkupService.fetchComponentMarkup).not.toHaveBeenCalled();
        done();
      });

      $rootScope.$digest();
    });

    it('loads the component markup from the backend using the MarkupService', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.resolve('markup'));
      const component = {};
      const properties = {};

      PageStructureService.renderComponent(component, properties)
        .then(() => {
          expect(MarkupService.fetchComponentMarkup).toHaveBeenCalledWith(component, properties);
          done();
        });

      $rootScope.$digest();
    });

    it('shows an error message and reloads the page when a component has been deleted', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.reject({ status: 404 }));
      spyOn(FeedbackService, 'showDismissible');

      PageStructureService.renderComponent({}).catch(() => {
        expect(FeedbackService.showDismissible).toHaveBeenCalledWith('FEEDBACK_NOT_FOUND_MESSAGE');
        done();
      });

      $rootScope.$digest();
    });

    it('does nothing if markup for a component cannot be retrieved but status is not 404', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.reject({}));
      spyOn(FeedbackService, 'showError');

      PageStructureService.renderComponent({}).then(() => {
        expect(FeedbackService.showError).not.toHaveBeenCalled();
        done();
      });

      $rootScope.$digest();
    });
  });

  describe('addComponentToContainer', () => {
    let mockComponent;
    let mockContainer;

    beforeEach(() => {
      mockComponent = {
        id: 'mock-component',
        name: 'Mock Component',
      };
      mockContainer = jasmine.createSpyObj(['getId']);
      mockContainer.getId.and.returnValue('mock-container');
    });

    it('uses the HstService to add a new catalog component to the backend', (done) => {
      spyOn(HstService, 'addHstComponent').and.returnValue(
        $q.resolve({ id: 'new-component' }),
      );

      PageStructureService.addComponentToContainer(mockComponent, mockContainer)
        .then(() => {
          expect(HstService.addHstComponent).toHaveBeenCalledWith(mockComponent, 'mock-container');
          done();
        });

      $rootScope.$digest();
    });

    it('shows the default error message when failed to add a new component from catalog', (done) => {
      spyOn(FeedbackService, 'showError');
      spyOn(HstService, 'addHstComponent').and.returnValue(
        $q.reject({
          error: 'cafebabe-error-key',
          parameterMap: {},
        }),
      );

      PageStructureService.addComponentToContainer(mockComponent, mockContainer)
        .catch(() => {
          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT', {
            component: 'Mock Component',
          });
          done();
        });

      $rootScope.$digest();
    });

    it('shows the locked error message when adding a new component on a container locked by another user', (done) => {
      spyOn(FeedbackService, 'showError');
      spyOn(HstService, 'addHstComponent').and.returnValue(
        $q.reject({
          error: 'ITEM_ALREADY_LOCKED',
          parameterMap: {
            lockedBy: 'another-user',
            lockedOn: 1234,
          },
        }),
      );

      PageStructureService.addComponentToContainer(mockComponent, mockContainer);
      $rootScope.$digest();

      expect(HstService.addHstComponent).toHaveBeenCalledWith(mockComponent, 'mock-container');
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT_ITEM_ALREADY_LOCKED', {
        lockedBy: 'another-user',
        lockedOn: 1234,
        component: 'Mock Component',
      });
      done();
    });

    it('records a change after adding a new component to a container successfully', (done) => {
      spyOn(HstService, 'addHstComponent').and.returnValue($q.resolve({ id: 'newUuid' }));

      PageStructureService.addComponentToContainer(mockComponent, mockContainer)
        .then((newComponentId) => {
          expect(HstService.addHstComponent).toHaveBeenCalledWith(mockComponent, 'mock-container');
          expect(ChannelService.recordOwnChange).toHaveBeenCalled();
          expect(newComponentId).toEqual('newUuid');
          done();
        });
      $rootScope.$digest();
    });
  });

  describe('renderComponent', () => {
    it('gracefully handles requests to re-render an undefined or null component', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup');

      $q.all(
        PageStructureService.renderComponent(),
        PageStructureService.renderComponent(null),
      ).then(() => {
        expect(MarkupService.fetchComponentMarkup).not.toHaveBeenCalled();
        done();
      });

      $rootScope.$digest();
    });

    it('loads the component markup from the backend using the MarkupService', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.resolve('markup'));
      const component = {};
      const properties = {};

      PageStructureService.renderComponent(component, properties)
        .then(() => {
          expect(MarkupService.fetchComponentMarkup).toHaveBeenCalledWith(component, properties);
          done();
        });

      $rootScope.$digest();
    });

    it('shows an error message and reloads the page when a component has been deleted', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.reject({ status: 404 }));
      spyOn(FeedbackService, 'showDismissible');

      PageStructureService.renderComponent({}).catch(() => {
        expect(FeedbackService.showDismissible).toHaveBeenCalledWith('FEEDBACK_NOT_FOUND_MESSAGE');
        done();
      });

      $rootScope.$digest();
    });

    it('does nothing if markup for a component cannot be retrieved but status is not 404', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.reject({}));
      spyOn(FeedbackService, 'showError');

      PageStructureService.renderComponent({}).then(() => {
        expect(FeedbackService.showError).not.toHaveBeenCalled();
        done();
      });

      $rootScope.$digest();
    });
  });

  describe('addComponentToContainer', () => {
    let mockComponent;
    let mockContainer;

    beforeEach(() => {
      mockComponent = {
        id: 'mock-component',
        name: 'Mock Component',
      };
      mockContainer = jasmine.createSpyObj(['getId']);
      mockContainer.getId.and.returnValue('mock-container');
    });

    it('uses the HstService to add a new catalog component to the backend', (done) => {
      spyOn(HstService, 'addHstComponent').and.returnValue(
        $q.resolve({ id: 'new-component' }),
      );

      PageStructureService.addComponentToContainer(mockComponent, mockContainer)
        .then(() => {
          expect(HstService.addHstComponent).toHaveBeenCalledWith(mockComponent, 'mock-container');
          done();
        });

      $rootScope.$digest();
    });

    it('shows the default error message when failed to add a new component from catalog', (done) => {
      spyOn(FeedbackService, 'showError');
      spyOn(HstService, 'addHstComponent').and.returnValue(
        $q.reject({
          error: 'cafebabe-error-key',
          parameterMap: {},
        }),
      );

      PageStructureService.addComponentToContainer(mockComponent, mockContainer)
        .catch(() => {
          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT', {
            component: 'Mock Component',
          });
          done();
        });

      $rootScope.$digest();
    });

    it('shows the locked error message when adding a new component on a container locked by another user', (done) => {
      spyOn(FeedbackService, 'showError');
      spyOn(HstService, 'addHstComponent').and.returnValue(
        $q.reject({
          error: 'ITEM_ALREADY_LOCKED',
          parameterMap: {
            lockedBy: 'another-user',
            lockedOn: 1234,
          },
        }),
      );

      PageStructureService.addComponentToContainer(mockComponent, mockContainer);
      $rootScope.$digest();

      expect(HstService.addHstComponent).toHaveBeenCalledWith(mockComponent, 'mock-container', undefined);
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT_ITEM_ALREADY_LOCKED', {
        lockedBy: 'another-user',
        lockedOn: 1234,
        component: 'Mock Component',
      });
      done();
    });

    it('records a change after adding a new component to a container successfully', (done) => {
      spyOn(HstService, 'addHstComponent').and.returnValue($q.resolve({ id: 'newUuid' }));

      PageStructureService.addComponentToContainer(mockComponent, mockContainer)
        .then((newComponentId) => {
          expect(HstService.addHstComponent).toHaveBeenCalledWith(mockComponent, 'mock-container', undefined);
          expect(ChannelService.recordOwnChange).toHaveBeenCalled();
          expect(newComponentId).toEqual('newUuid');
          done();
        });
      $rootScope.$digest();
    });
  });

  const childComment = element => [...element.childNodes]
    .filter(child => child.nodeType === Node.COMMENT_NODE)
    .shift();

  const lastComment = element => [...element.childNodes]
    .filter(child => child.nodeType === Node.COMMENT_NODE)
    .pop();

  const previousComment = (element) => {
    while (element.previousSibling) {
      element = element.previousSibling;
      if (element.nodeType === 8) {
        return element;
      }
    }
    return null;
  };

  const nextComment = (element) => {
    while (element.nextSibling) {
      element = element.nextSibling;
      if (element.nodeType === 8) {
        return element;
      }
    }
    return null;
  };

  const registerParsedElement = element => registered.push({ element, json: JSON.parse(element.data) });

  const registerEmptyVBoxContainer = () => {
    const container = $j('#container-vbox-empty', $document)[0];

    registerParsedElement(previousComment(container));
    registerParsedElement(nextComment(container));

    return container;
  };

  const registerVBoxContainer = (callback) => {
    const container = $j('#container-vbox', $document)[0];

    registerParsedElement(previousComment(container));
    if (callback) {
      callback();
    }
    registerParsedElement(nextComment(container));

    return container;
  };

  const registerVBoxComponent = (id, callback) => {
    const component = $j(`#${id}`, $document)[0];

    registerParsedElement(childComment(component));
    if (callback) {
      callback();
    }
    registerParsedElement(lastComment(component));

    return component;
  };

  const registerNoMarkupContainer = (callback, id = '#container-no-markup') => {
    const container = $j(id, $document)[0];

    registerParsedElement(childComment(container));
    if (callback) {
      callback();
    }
    registerParsedElement(lastComment(container));

    return container;
  };

  const registerEmptyNoMarkupContainer = () => registerNoMarkupContainer(undefined, '#container-no-markup-empty');

  const registerLowercaseNoMarkupContainer = () => registerNoMarkupContainer(
    undefined,
    '#container-no-markup-lowercase',
  );

  const registerEmptyLowercaseNoMarkupContainer = () => {
    registerNoMarkupContainer(undefined, '#container-no-markup-lowercase-empty');
  };

  const registerNoMarkupContainerWithoutTextNodesAfterEndComment = () => {
    registerNoMarkupContainer(undefined, '#container-no-markup-without-text-nodes-after-end-comment');
  };

  const registerNoMarkupComponent = (callback) => {
    const component = $j('#component-no-markup', $document)[0];
    registerParsedElement(previousComment(component));
    if (callback) {
      callback();
    }
    registerParsedElement(nextComment(component));

    return component;
  };

  const registerEmptyNoMarkupComponent = () => {
    registerParsedElement(nextComment(childComment($j('#container-no-markup', $document)[0])));
  };

  const registerEmbeddedLink = (selector) => {
    registerParsedElement(childComment($j(selector, $document)[0]));
  };

  const registerHeadContributions = (selector) => {
    registerParsedElement(childComment($j(selector, $document)[0]));
  };

  it('registers containers in the correct order', () => {
    const container1 = registerVBoxContainer();
    const container2 = registerNoMarkupContainer();
    PageStructureService.parseElements();

    const page = PageStructureService.getPage();
    const containers = page.getContainers();
    expect(containers.length).toEqual(2);

    expect(containers[0].getType()).toEqual('container');
    expect(containers[0].isEmpty()).toEqual(true);
    expect(containers[0].getComponents()).toEqual([]);
    expect(containers[0].getBoxElement()[0]).toEqual(container1);
    expect(containers[0].getLabel()).toEqual('vBox container');

    expect(containers[1].getType()).toEqual('container');
    expect(containers[1].isEmpty()).toEqual(true);
    expect(containers[1].getComponents()).toEqual([]);
    expect(containers[1].getBoxElement()[0]).toEqual(container2);
    expect(containers[1].getLabel()).toEqual('NoMarkup container');

    expect(page.hasContainer(containers[0])).toEqual(true);
    expect(page.hasContainer(containers[1])).toEqual(true);
  });

  it('adds components to the most recently registered container', () => {
    let componentA;
    let componentB;

    registerVBoxContainer();
    registerVBoxContainer(() => {
      componentA = registerVBoxComponent('componentA');
      componentB = registerVBoxComponent('componentB');
    });

    PageStructureService.parseElements();

    const containers = PageStructureService.getPage().getContainers();
    expect(containers.length).toEqual(2);
    expect(containers[0].isEmpty()).toEqual(true);
    expect(containers[1].isEmpty()).toEqual(false);
    expect(containers[1].getComponents().length).toEqual(2);

    expect(containers[1].getComponents()[0].getType()).toEqual('component');
    expect(containers[1].getComponents()[0].getBoxElement()[0]).toBe(componentA);
    expect(containers[1].getComponents()[0].getLabel()).toEqual('component A');
    expect(containers[1].getComponents()[0].container).toEqual(containers[1]);

    expect(containers[1].getComponents()[1].getType()).toEqual('component');
    expect(containers[1].getComponents()[1].getBoxElement()[0]).toBe(componentB);
    expect(containers[1].getComponents()[1].getLabel()).toEqual('component B');
    expect(containers[1].getComponents()[1].container).toEqual(containers[1]);
  });

  it('registers edit menu links', () => {
    registerEmbeddedLink('#edit-menu-in-page');
    PageStructureService.parseElements();

    const editMenuLinks = PageStructureService.getEmbeddedLinks();
    expect(editMenuLinks.length).toBe(1);
    expect(editMenuLinks[0].getId()).toBe('menu-in-page');
  });

  it('registers manage content links', () => {
    registerEmbeddedLink('#manage-content-in-page');
    PageStructureService.parseElements();

    const manageContentLinks = PageStructureService.getEmbeddedLinks();
    const manageContentLink = manageContentLinks[0];
    expect(manageContentLink.getDefaultPath()).toBe('test-default-path');
    expect(manageContentLink.getDocumentTemplateQuery()).toBe('new-test-document');
    expect(manageContentLink.getParameterName()).toBe('test-component-parameter');
    expect(manageContentLink.getParameterValue()).toBe('test-component-value');
    expect(manageContentLink.getPickerConfig()).toEqual({
      configuration: 'test-component-picker configuration',
      initialPath: 'test-component-picker-initial-path',
      isRelativePath: false,
      remembersLastVisited: false,
      rootPath: 'test-component-picker-root-path',
      selectableNodeTypes: ['test-node-type-1', 'test-node-type-2'],
    });
    expect(manageContentLink.getRootPath()).toBe('test-root-path');
    expect(manageContentLink.isParameterValueRelativePath()).toBe(true);
  });

  it('recognizes a manage content link for a parameter that stores an absolute path', () => {
    registerEmbeddedLink('#manage-content-with-absolute-path');
    PageStructureService.parseElements();

    const manageContentLinks = PageStructureService.getEmbeddedLinks();
    const manageContentLink = manageContentLinks[0];
    expect(manageContentLink.getDocumentTemplateQuery()).toBe('new-test-document');
    expect(manageContentLink.getDefaultPath()).toBe('test-default-path');
    expect(manageContentLink.getRootPath()).toBe('test-root-path');
    expect(manageContentLink.getParameterName()).toBe('test-component-parameter');
    expect(manageContentLink.getParameterValue()).toBe('test-component-value');
    expect(manageContentLink.getPickerConfig()).toEqual({
      configuration: 'test-component-picker configuration',
      initialPath: 'test-component-picker-initial-path',
      isRelativePath: false,
      remembersLastVisited: false,
      rootPath: 'test-component-picker-root-path',
      selectableNodeTypes: ['test-node-type-1', 'test-node-type-2'],
    });
    expect(manageContentLink.isParameterValueRelativePath()).toBe(false);
  });

  it('registers processed and unprocessed head contributions', () => {
    registerHeadContributions('#processed-head-contributions');
    registerHeadContributions('#unprocessed-head-contributions');
    PageStructureService.parseElements();

    expect([...PageStructureService.headContributions]).toEqual([
      '<title>processed</title>',
      '<script>window.processed = true</script>',
      '<link href="unprocessed.css">',
    ]);
  });

  it('clears the page structure', () => {
    registerVBoxContainer();
    registerEmbeddedLink('#manage-content-in-page');
    registerEmbeddedLink('#edit-menu-in-page');
    registerHeadContributions('#processed-head-contributions');
    PageStructureService.parseElements();

    expect(PageStructureService.getPage()).toBeDefined();
    expect(PageStructureService.getEmbeddedLinks().length).toEqual(2);
    expect(PageStructureService.headContributions.size).toBe(2);

    PageStructureService.clearParsedElements();

    expect(PageStructureService.getPage()).toBeUndefined();
    expect(PageStructureService.getEmbeddedLinks().length).toEqual(0);
    expect(PageStructureService.headContributions.size).toBe(0);
  });

  it('finds the DOM element of a no-markup container as parent of the comment', () => {
    const container = registerNoMarkupContainer();
    PageStructureService.parseElements();

    const containers = PageStructureService.getPage().getContainers();
    expect(containers.length).toEqual(1);
    expect(containers[0].getBoxElement()[0]).toBe(container);
  });

  it('finds the DOM element of a component of a no-markup container as next sibling of the comment', () => {
    let component;

    registerNoMarkupContainer(() => {
      component = registerNoMarkupComponent();
    });
    PageStructureService.parseElements();

    const containers = PageStructureService.getPage().getContainers();
    expect(containers.length).toEqual(1);
    expect(containers[0].isEmpty()).toEqual(false);
    expect(containers[0].getComponents().length).toEqual(1);
    expect(containers[0].getComponents()[0].getBoxElement()[0]).toBe(component);
    expect(containers[0].getComponents()[0].hasNoIFrameDomElement()).not.toEqual(true);
  });

  it('registers no iframe box element in case of a no-markup, empty component', () => {
    registerNoMarkupContainer(() => registerEmptyNoMarkupComponent());
    PageStructureService.parseElements();

    const containers = PageStructureService.getPage().getContainers();
    expect(containers.length).toEqual(1);
    expect(containers[0].isEmpty()).toEqual(false);
    expect(containers[0].getComponents().length).toEqual(1);
    expect(containers[0].getComponents()[0].getBoxElement().length).toEqual(0);
  });

  it('detects if a container contains DOM elements that represent a container-item', () => {
    registerVBoxContainer();
    registerNoMarkupContainer();
    registerLowercaseNoMarkupContainer();
    PageStructureService.parseElements();

    const containers = PageStructureService.getPage().getContainers();
    expect(containers[0].isEmptyInDom()).toEqual(false);
    expect(containers[1].isEmptyInDom()).toEqual(false);
    expect(containers[2].isEmptyInDom()).toEqual(false);
  });

  it('detects if a container does not contain DOM elements that represent a container-item', () => {
    registerEmptyVBoxContainer();
    registerEmptyNoMarkupContainer();
    registerEmptyLowercaseNoMarkupContainer();
    PageStructureService.parseElements();

    const containers = PageStructureService.getPage().getContainers();
    expect(containers[0].isEmptyInDom()).toEqual(true);
    expect(containers[1].isEmptyInDom()).toEqual(true);
    expect(containers[2].isEmptyInDom()).toEqual(true);
  });

  it('returns a known component', () => {
    registerVBoxContainer();
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    const pageComponent = PageStructureService.getPage().getComponentById('aaaa');

    expect(pageComponent).not.toBeNull();
    expect(pageComponent.getId()).toEqual('aaaa');
    expect(pageComponent.getLabel()).toEqual('component A');
  });

  it('removes a valid component and calls HST successfully', () => {
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.when([]));
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(''));

    PageStructureService.removeComponentById('aaaa');

    $rootScope.$digest();

    expect(HstComponentService.deleteComponent).toHaveBeenCalledWith('container-vbox', 'aaaa');
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
  });

  it('removes a valid component but fails to call HST due to an unknown reason, then iframe should be reloaded and a feedback toast should be shown', () => { // eslint-disable-line max-len
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    spyOn(FeedbackService, 'showError');
    spyOn(HippoIframeService, 'reload').and.returnValue($q.when(''));
    // mock the call to HST to be failed
    spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.reject({ error: 'unknown', parameterMap: {} }));

    PageStructureService.removeComponentById('aaaa');
    $rootScope.$digest();

    expect(HstComponentService.deleteComponent).toHaveBeenCalledWith('container-vbox', 'aaaa');

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_COMPONENT',
      jasmine.objectContaining({ component: 'component A' }));
  });

  it('removes a valid component but fails to call HST due to locked component then iframe should be reloaded and a feedback toast should be shown', () => { // eslint-disable-line max-len
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    spyOn(FeedbackService, 'showError');
    spyOn(HippoIframeService, 'reload').and.returnValue($q.when(''));
    // mock the call to HST to be failed
    spyOn(HstComponentService, 'deleteComponent')
      .and.returnValue($q.reject({ error: 'ITEM_ALREADY_LOCKED', parameterMap: {} }));

    PageStructureService.removeComponentById('aaaa');
    $rootScope.$digest();

    expect(HstComponentService.deleteComponent).toHaveBeenCalledWith('container-vbox', 'aaaa');

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_COMPONENT_ITEM_ALREADY_LOCKED',
      jasmine.objectContaining({ component: 'component A' }));
  });

  it('removes an invalid component', () => {
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.when([]));

    PageStructureService.removeComponentById('unknown-component');
    $rootScope.$digest();

    expect(HstComponentService.deleteComponent).not.toHaveBeenCalled();
  });

  it('returns a container by iframe element', () => {
    registerVBoxContainer();
    const containerElement = registerNoMarkupContainer();
    PageStructureService.parseElements();

    const container = PageStructureService.getContainerByIframeElement(containerElement);

    expect(container).not.toBeNull();
    expect(container.getId()).toEqual('container-no-markup');
  });

  it('attaches the embedded link to the enclosing component', () => {
    registerVBoxContainer(() => {
      registerVBoxComponent('componentA', () => {
        registerEmbeddedLink('#edit-menu-in-component-a');
      });
      registerVBoxComponent('componentB');
      registerEmbeddedLink('#manage-content-in-container-vbox');
    });
    registerEmbeddedLink('#edit-menu-in-page');
    registerEmbeddedLink('#manage-content-in-page');
    PageStructureService.parseElements();

    const page = PageStructureService.getPage();
    const [containerVBox] = page.getContainers();
    const [componentA] = containerVBox.getComponents();
    const attachedEmbeddedLinks = PageStructureService.getEmbeddedLinks();

    expect(attachedEmbeddedLinks.length).toBe(4);
    expect(attachedEmbeddedLinks[0].getComponent()).toBe(componentA);
    expect(attachedEmbeddedLinks[1].getComponent()).toBe(containerVBox);
    expect(attachedEmbeddedLinks[2].getComponent()).toBeUndefined();
    expect(attachedEmbeddedLinks[3].getComponent()).toBeUndefined();

    expect(attachedEmbeddedLinks[0].getBoxElement().length).toBe(1);
    expect(attachedEmbeddedLinks[0].getBoxElement().attr('class')).toBe('hst-fab');
  });

  it('re-renders a component with an edit menu link', () => {
    // set up page structure with component and edit menu link in it
    registerVBoxContainer(() => {
      registerVBoxComponent('componentA', () => {
        registerEmbeddedLink('#edit-menu-in-component-a');
      });
    });
    registerEmbeddedLink('#edit-menu-in-page');
    PageStructureService.parseElements();

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-edit-menu-in-component-a">
          ${editMenuLinkComment('updated-menu-in-component-a')}
        </p>
      ${endComment('aaaa')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const page = PageStructureService.getPage();
    const component = page.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    const [updatedComponentA] = page.getContainers()[0].getComponents();
    const editMenuLinks = PageStructureService.getEmbeddedLinks();

    expect(editMenuLinks.length).toBe(2);
    expect(editMenuLinks[0].getComponent()).toBeUndefined();
    expect(editMenuLinks[1].getComponent()).toBe(updatedComponentA);
  });

  it('re-renders a component with no more content link', () => {
    // set up page structure with component and content link in it
    registerNoMarkupContainer(() => {
      registerNoMarkupComponent(() => {
        registerEmbeddedLink('#manage-content-in-component-no-markup');
      });
    });
    PageStructureService.parseElements();

    const updatedMarkup = `
      ${itemComment('Component in NoMarkup container', 'component-no-markup')}
        <div id="component-no-markup">
          <p>Some markup in component D</p>
        </div>
      ${endComment('component-no-markup')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getPage().getComponentById('component-no-markup');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    expect(PageStructureService.getEmbeddedLinks().length).toBe(0);
  });

  it('re-renders a component, adding an edit menu link', () => {
    // set up page structure with component and edit menu link in it
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    expect(PageStructureService.getEmbeddedLinks().length).toBe(0);

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-edit-menu-in-component-a">
          ${editMenuLinkComment('updated-menu-in-component-a')}
        </p>
      ${endComment('aaaa')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const page = PageStructureService.getPage();
    const component = page.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    const [updatedComponentA] = page.getContainers()[0].getComponents();
    const embeddedLinks = PageStructureService.getEmbeddedLinks();
    expect(embeddedLinks.length).toBe(1);
    expect(embeddedLinks[0].getComponent()).toBe(updatedComponentA);
  });

  it('gracefully re-renders a component twice quickly after eachother', () => {
    // set up page structure with component
    registerVBoxContainer(() => registerVBoxComponent('componentB'));
    PageStructureService.parseElements();

    const updatedMarkup = `
      ${itemComment('component B', 'bbbb')}
        <p>Re-rendered component B</p>
      ${endComment('bbbb')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getPage().getComponentById('bbbb');
    PageStructureService.renderComponent(component);
    PageStructureService.renderComponent(component);

    expect(() => {
      $rootScope.$digest();
    }).not.toThrow();
  });

  it('does not add a re-rendered and incorrectly commented component to the page structure', () => {
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-edit-menu-in-component-a">
          ${editMenuLinkComment('updated-menu-in-component-a')}
        </p>
      `;
    spyOn($log, 'error');
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const page = PageStructureService.getPage();
    const component = page.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    expect(page.getContainers().length).toBe(1);
    expect(page.getContainers()[0].getComponents().length).toBe(0);
    expect($log.error).toHaveBeenCalled();
  });

  it('knows that a re-rendered component contains new head contributions', () => {
    const onNewHeadContributions = jasmine.createSpy('new-head-contributions');
    const offNewHeadContributions = $rootScope.$on('hippo-iframe:new-head-contributions', onNewHeadContributions);

    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-component-with-new-head-contribution">
        </p>
      ${endComment('aaaa')}
      ${unprocessedHeadContributionsComment('<script>window.newScript=true</script>')}
      `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const page = PageStructureService.getPage();
    const component = page.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    const [updatedComponent] = page.getContainers()[0].getComponents();
    expect(onNewHeadContributions).toHaveBeenCalledWith(jasmine.any(Object), updatedComponent);

    offNewHeadContributions();
  });

  it('knows that a re-rendered component does not contain new head contributions', () => {
    const onNewHeadContributions = jasmine.createSpy('new-head-contributions');
    const offNewHeadContributions = $rootScope.$on('hippo-iframe:new-head-contributions', onNewHeadContributions);

    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
      <p id="updated-component-with-new-head-contribution">
      </p>
      ${endComment('aaaa')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getPage().getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    expect(onNewHeadContributions).not.toHaveBeenCalled();

    offNewHeadContributions();
  });

  it('knows that a re-rendered component does not contain new head contributions if they have already been rendered by the page', () => { // eslint-disable-line max-len
    const onNewHeadContributions = jasmine.createSpy('new-head-contributions');
    const offNewHeadContributions = $rootScope.$on('hippo-iframe:new-head-contributions', onNewHeadContributions);

    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    registerHeadContributions('#processed-head-contributions');
    PageStructureService.parseElements();

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-component-with-new-head-contribution">
        </p>
      ${endComment('aaaa')}
      ${unprocessedHeadContributionsComment('<script>window.processed = true</script>')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getPage().getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    expect(onNewHeadContributions).not.toHaveBeenCalled();

    offNewHeadContributions();
  });

  it('does not reload the page when a component is re-rendered with custom properties', () => {
    const onNewHeadContributions = jasmine.createSpy('new-head-contributions');
    const offNewHeadContributions = $rootScope.$on('hippo-iframe:new-head-contributions', onNewHeadContributions);

    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-component-with-new-head-contribution">
        </p>
      ${endComment('aaaa')}
      ${unprocessedHeadContributionsComment('<script>window.newScript=true</script>')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getPage().getComponentById('aaaa');
    const propertiesMap = {
      parameter: 'customValue',
    };
    PageStructureService.renderComponent(component, propertiesMap);
    $rootScope.$digest();

    expect(onNewHeadContributions).not.toHaveBeenCalled();

    offNewHeadContributions();
  });

  it('notifies change listeners when updating a component', () => {
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    const page = PageStructureService.getPage();
    const [container] = page.getContainers();
    const [component] = container.getComponents();
    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-component-with-new-head-contribution">
        </p>
      ${endComment('aaaa')}
    `;

    spyOn(PageStructureService, '_notifyChangeListeners').and.callThrough();

    PageStructureService._updateComponent(component, updatedMarkup);
    expect(PageStructureService._notifyChangeListeners).toHaveBeenCalled();
  });

  it('re-renders a NoMarkup container', () => {
    registerNoMarkupContainer();
    PageStructureService.parseElements();

    const page = PageStructureService.getPage();
    const [container] = page.getContainers();
    container.getEndComment().after('<p>Trailing element, to be removed</p>'); // insert trailing dom element
    expect(container.getEndComment().next().length).toBe(1);
    const updatedMarkup = `
      ${containerComment('NoMarkup container', 'HST.nomarkup', 'container-nomarkup')}
        ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
        ${endComment('aaaa')}
      ${endComment('container-nomarkup')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    PageStructureService.renderContainer(container);
    $rootScope.$digest();

    const [newContainer] = page.getContainers();
    expect(newContainer).not.toBe(container);
    expect(newContainer.getEndComment().next().length).toBe(0);
  });

  it('re-renders a NoMarkup container without any text nodes after the end comment', () => {
    registerNoMarkupContainerWithoutTextNodesAfterEndComment();
    PageStructureService.parseElements();

    const page = PageStructureService.getPage();
    const [container] = page.getContainers();
    const updatedMarkup = `
      ${containerComment('Empty NoMarkup container', 'HST.NoMarkup', 'no-markup-no-text-nodes-after-end-comment')}
      ${endComment('no-markup-no-text-nodes-after-end-comment')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    PageStructureService.renderContainer(container);
    $rootScope.$digest();

    const [newContainer] = page.getContainers();
    expect(newContainer).not.toBe(container);
    expect(newContainer.isEmpty()).toBe(true);
  });

  it('re-renders a container with an edit menu link', (done) => {
    // set up page structure with component and edit menu link in it
    registerVBoxContainer(() => {
      registerVBoxComponent('componentA', () => registerEmbeddedLink('#edit-menu-in-component-a'));
      registerEmbeddedLink('#manage-content-in-container-vbox');
    });
    registerEmbeddedLink('#edit-menu-in-page');
    registerEmbeddedLink('#manage-content-in-page');
    PageStructureService.parseElements();

    const page = PageStructureService.getPage();
    const [container] = page.getContainers();
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
          ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
        <p id="new-manage-content-in-container-vbox">
          ${manageContentLinkComment('new-manage-content-in-container-vbox')}
        </p>
      </div>
      ${endComment('container-vbox')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    PageStructureService.renderContainer(container).then((newContainer) => {
      expect(page.getContainers().length).toBe(1);
      expect(page.getContainers()[0]).toBe(newContainer);

      // edit menu link in component A is no longer there
      const embeddedLinks = PageStructureService.getEmbeddedLinks();
      expect(embeddedLinks.length).toBe(3);
      expect(embeddedLinks[0].getId()).toBe('menu-in-page');
      expect(embeddedLinks[1].getDocumentTemplateQuery()).toBe('new-test-document');
      expect(embeddedLinks[2].getDocumentTemplateQuery()).toBe('new-manage-content-in-container-vbox');
      expect(embeddedLinks[2].getComponent()).toBe(newContainer);
      done();
    });
    $rootScope.$digest();
  });

  it('known that a re-rendered container contains new head contributions', (done) => {
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    const [container] = PageStructureService.getPage().getContainers();
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
          ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
      </div>
      ${endComment('container-vbox')}
      ${unprocessedHeadContributionsComment('<script>window.newScript=true</script>')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    spyOn($rootScope, '$emit');

    PageStructureService.renderContainer(container).then((newContainer) => {
      expect($rootScope.$emit).toHaveBeenCalledWith('hippo-iframe:new-head-contributions', newContainer);
      done();
    });
    $rootScope.$digest();
  });

  it('notifies change listeners when updating a container', (done) => {
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    const [container] = PageStructureService.getPage().getContainers();
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
        ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
      </div>
      ${endComment('container-vbox')}
    `;

    spyOn(PageStructureService, '_notifyChangeListeners').and.callThrough();
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));

    PageStructureService.renderContainer(container).then(() => {
      expect(PageStructureService._notifyChangeListeners).toHaveBeenCalled();
      done();
    });

    $rootScope.$digest();
  });

  it('knows that a re-rendered container does not contain new head contributions', (done) => {
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    PageStructureService.parseElements();

    const [container] = PageStructureService.getPage().getContainers();
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
          ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
      </div>
      ${endComment('container-vbox')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    spyOn($rootScope, '$emit');

    PageStructureService.renderContainer(container).then(() => {
      expect($rootScope.$emit).not.toHaveBeenCalledWith('hippo-iframe:new-head-contributions', jasmine.anything());
      done();
    });
    $rootScope.$digest();
  });

  it('known that a re-rendered container does not contain new head contributions if they have already been rendered by the page', (done) => { // eslint-disable-line max-len
    registerVBoxContainer(() => registerVBoxComponent('componentA'));
    registerHeadContributions('#processed-head-contributions');
    PageStructureService.parseElements();

    const [container] = PageStructureService.getPage().getContainers();
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
          ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
      </div>
      ${endComment('container-vbox')}
      ${unprocessedHeadContributionsComment('<script>window.processed = true</script>')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    spyOn($rootScope, '$emit');

    PageStructureService.renderContainer(container).then(() => {
      expect($rootScope.$emit).not.toHaveBeenCalledWith('hippo-iframe:new-head-contributions', jasmine.anything());
      done();
    });
    $rootScope.$digest();
  });

  function expectUpdateHstContainer(id, container) {
    expect(HstService.updateHstContainer).toHaveBeenCalledWith(id, container.getHstRepresentation());
  }

  describe('move component', () => {
    function componentIds(container) {
      return container.getComponents().map(component => component.getId());
    }

    it('can move the first component to the second position in the container', () => {
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA');
        registerVBoxComponent('componentB');
      });
      PageStructureService.parseElements();

      const [container] = PageStructureService.getPage().getContainers();
      const [componentA] = container.getComponents();

      spyOn(HstService, 'updateHstContainer');
      expect(componentIds(container)).toEqual(['aaaa', 'bbbb']);

      PageStructureService.moveComponent(componentA, container, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container);
      expect(componentIds(container)).toEqual(['bbbb', 'aaaa']);
      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('can move the second component to the first position in the container', () => {
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA');
        registerVBoxComponent('componentB');
      });
      PageStructureService.parseElements();

      const [container] = PageStructureService.getPage().getContainers();
      const [componentA, componentB] = container.getComponents();

      spyOn(HstService, 'updateHstContainer');
      expect(componentIds(container)).toEqual(['aaaa', 'bbbb']);

      PageStructureService.moveComponent(componentB, container, componentA);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container);
      expect(componentIds(container)).toEqual(['bbbb', 'aaaa']);
      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('can move a component to another container', () => {
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA');
        registerVBoxComponent('componentB');
      });
      registerEmptyVBoxContainer();
      PageStructureService.parseElements();

      const [container1, container2] = PageStructureService.getPage().getContainers();
      const [component] = container1.getComponents();

      spyOn(HstService, 'updateHstContainer');
      expect(componentIds(container1)).toEqual(['aaaa', 'bbbb']);
      expect(componentIds(container2)).toEqual([]);

      PageStructureService.moveComponent(component, container2, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container1);
      expectUpdateHstContainer('container-vbox-empty', container2);
      expect(componentIds(container1)).toEqual(['bbbb']);
      expect(componentIds(container2)).toEqual(['aaaa']);
      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('shows an error when a component is moved within a container that just got locked by another user', () => {
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA');
        registerVBoxComponent('componentB');
      });
      PageStructureService.parseElements();

      const [container] = PageStructureService.getPage().getContainers();
      const [component] = container.getComponents();

      spyOn(HstService, 'updateHstContainer').and.returnValue($q.reject());
      spyOn(FeedbackService, 'showError');

      PageStructureService.moveComponent(component, container, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container);
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
        component: 'component A',
      });
      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });

    it('shows an error when a component is moved out of a container that just got locked by another user', () => {
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA');
      });
      registerEmptyVBoxContainer();
      PageStructureService.parseElements();

      const [container1, container2] = PageStructureService.getPage().getContainers();
      const [component] = container1.getComponents();

      spyOn(HstService, 'updateHstContainer').and.returnValues($q.reject(), $q.resolve());
      spyOn(FeedbackService, 'showError');

      PageStructureService.moveComponent(component, container2, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container1);
      expectUpdateHstContainer('container-vbox-empty', container2);
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
        component: 'component A',
      });
      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });

    it('shows an error when a component is moved into a container that just got locked by another user', () => {
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA');
      });
      registerEmptyVBoxContainer();
      PageStructureService.parseElements();

      const [container1, container2] = PageStructureService.getPage().getContainers();
      const [component] = container1.getComponents();

      spyOn(HstService, 'updateHstContainer').and.returnValues($q.resolve(), $q.reject());
      spyOn(FeedbackService, 'showError');

      PageStructureService.moveComponent(component, container2, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container1);
      expectUpdateHstContainer('container-vbox-empty', container2);
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
        component: 'component A',
      });
      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });
  });
});
