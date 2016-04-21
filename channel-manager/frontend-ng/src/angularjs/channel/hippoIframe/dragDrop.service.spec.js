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

describe('DragDropService', () => {
  'use strict';

  let $q;
  let DragDropService;
  let ScalingService;
  let PageStructureService;
  let HstService;
  let ChannelService;
  let FeedbackService;

  let iframe;
  let base;

  let container1;
  let component1;
  let component2;
  let container2;

  beforeEach(() => {
    module('hippo-cm.channel.hippoIframe');

    inject((_$q_,
            _DragDropService_,
            _ScalingService_,
            _PageStructureService_,
            _HstService_,
            _ChannelService_,
            _FeedbackService_) => {
      $q = _$q_;
      DragDropService = _DragDropService_;
      ScalingService = _ScalingService_;
      PageStructureService = _PageStructureService_;
      HstService = _HstService_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
    });

    spyOn(ChannelService, 'recordOwnChange');
    jasmine.getFixtures().load('channel/hippoIframe/dragDrop.service.fixture.html');

    iframe = $j('#testIframe');
    base = $j('#testBase');
  });

  function createContainer(number) {
    const iframeContainerComment = iframe.contents().find(`#iframeContainerComment${number}`)[0];
    PageStructureService.registerParsedElement(iframeContainerComment, {
      uuid: `container${number}`,
      'HST-Type': 'CONTAINER_COMPONENT',
      'HST-XType': 'HST.Transparent',
    });
    return PageStructureService.containers[PageStructureService.containers.length - 1];
  }

  function createComponent(number) {
    const iframeComponentComment = iframe.contents().find(`#iframeComponentComment${number}`)[0];
    PageStructureService.registerParsedElement(iframeComponentComment, {
      uuid: `component${number}`,
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
      'HST-Label': `Component ${number}`,
    });
    return PageStructureService.getComponentById(`component${number}`);
  }

  function componentIds(container) {
    return container.getComponents().map((component) => component.getId());
  }

  function loadIframeFixture(callback) {
    DragDropService.init(iframe, base);
    iframe.one('load', () => {
      const iframeWindow = iframe[0].contentWindow;

      container1 = createContainer(1);
      component1 = createComponent(1, container1);
      component2 = createComponent(2, container1);

      container2 = createContainer(2);

      DragDropService.enable([container1, container2]).then(() => {
        try {
          callback(iframeWindow);
        } catch (e) {
          // Karma silently swallows stack traces for synchronous tests, so log them in an explicit fail
          fail(e);
        }
      });
    });
    iframe.attr('src', `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/dragDrop.service.iframe.fixture.html`);
  }

  function boundEventHandlerCount(jqueryElement, event) {
    const eventHandlers = $._data(jqueryElement[0], 'events');
    return eventHandlers && eventHandlers.hasOwnProperty(event) ? eventHandlers[event].length : 0;
  }

  it('is not dragging initially', () => {
    expect(DragDropService.isDragging()).toBeFalsy();
  });

  it('injects dragula.js into the iframe', (done) => {
    loadIframeFixture((iframeWindow) => {
      expect(typeof iframeWindow.dragula).toEqual('function');
      expect(DragDropService.drake).toBeDefined();
      expect(DragDropService.isDragging()).toBeFalsy();
      done();
    });
  });

  it('destroys dragula on iframe unload', (done) => {
    loadIframeFixture((iframeWindow) => {
      expect(DragDropService.drake).not.toBeNull();
      $(iframeWindow).trigger('unload');
      expect(DragDropService.drake).toBeNull();
      expect(DragDropService.isDragging()).toBeFalsy();
      done();
    });
  });

  it('forwards a shifted mouse event to the iframe when it starts dragging in a non-scaled iframe', (done) => {
    loadIframeFixture(() => {
      const mockedMouseDownEvent = {
        clientX: 100,
        clientY: 200,
      };
      const iframeComponentElement1 = component1.getBoxElement()[0];

      iframe.offset({
        left: 10,
        top: 20,
      });
      spyOn(ScalingService, 'getScaleFactor').and.returnValue(1.0);
      spyOn(iframeComponentElement1, 'dispatchEvent');

      DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

      expect(DragDropService.isDraggingOrClicking()).toBeTruthy();
      expect(DragDropService.isDragging()).toBeFalsy();

      const dispatchedEvent = iframeComponentElement1.dispatchEvent.calls.argsFor(0)[0];
      expect(dispatchedEvent.type).toEqual('mousedown');
      expect(dispatchedEvent.bubbles).toEqual(true);
      expect(dispatchedEvent.clientX).toEqual(90);
      expect(dispatchedEvent.clientY).toEqual(180);

      done();
    });
  });

  it('forwards a shifted mouse event to the iframe when it starts dragging in a scaled iframe', (done) => {
    loadIframeFixture(() => {
      const mockedMouseDownEvent = {
        clientX: 150,
        clientY: 150,
      };
      const iframeComponentElement1 = component1.getBoxElement()[0];

      base.offset({
        left: 10,
        top: 10,
      });
      iframe.offset({
        left: 20,
        top: 20,
      });
      iframe.width(200);

      spyOn(ScalingService, 'getScaleFactor').and.returnValue(0.5);
      spyOn(iframeComponentElement1, 'dispatchEvent');

      DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

      const dispatchedEvent = iframeComponentElement1.dispatchEvent.calls.argsFor(0)[0];

      expect(dispatchedEvent.type).toEqual('mousedown');
      expect(dispatchedEvent.bubbles).toEqual(true);
      expect(dispatchedEvent.clientX).toEqual(80);
      expect(dispatchedEvent.clientY).toEqual(260);
      done();
    });
  });

  it('stops dragging or clicking when disabled', (done) => {
    loadIframeFixture(() => {
      const mockedMouseDownEvent = {
        clientX: 100,
        clientY: 200,
      };

      DragDropService.startDragOrClick(mockedMouseDownEvent, component1);
      expect(DragDropService.isDraggingOrClicking()).toBeTruthy();
      expect(DragDropService.isDragging()).toBeFalsy();

      DragDropService.disable();
      expect(DragDropService.isDraggingOrClicking()).toBeFalsy();
      expect(DragDropService.isDragging()).toBeFalsy();

      done();
    });
  });

  it('shows a component\'s properties when a component receives a mouseup event', (done) => {
    loadIframeFixture(() => {
      const mockedMouseDownEvent = {
        clientX: 100,
        clientY: 200,
      };

      DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

      spyOn(PageStructureService, 'showComponentProperties');

      const componentElement1 = component1.getBoxElement();
      componentElement1.on('mouseup', () => {
        expect(PageStructureService.showComponentProperties).toHaveBeenCalledWith(component1);
        done();
      });
      componentElement1.trigger('mouseup');
    });
  });

  it('cancels the click simulation for showing a component\'s properties when the mouse cursor leaves a disabled component', (done) => {
    spyOn(container1, 'isDisabled').and.returnValue(true);
    loadIframeFixture(() => {
      const mockedMouseDownEvent = {
        clientX: 100,
        clientY: 200,
      };
      const componentElement1 = component1.getBoxElement();
      DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

      expect(DragDropService.isDraggingOrClicking()).toEqual(true);
      expect(boundEventHandlerCount(componentElement1, 'mouseup')).toEqual(1);
      expect(boundEventHandlerCount(componentElement1, 'mouseout')).toEqual(1);

      componentElement1.one('mouseout.test', () => {
        expect(DragDropService.isDraggingOrClicking()).toEqual(false);
        expect(boundEventHandlerCount(componentElement1, 'mouseup')).toEqual(0);
        expect(boundEventHandlerCount(componentElement1, 'mouseout')).toEqual(0);
        done();
      });

      componentElement1.trigger('mouseout');
    });
  });

  it('can move the first component to the second position in the container', (done) => {
    loadIframeFixture(() => {
      const componentElement1 = component1.getBoxElement();
      const containerElement1 = container1.getBoxElement();

      spyOn(HstService, 'doPost');
      expect(componentIds(container1)).toEqual(['component1', 'component2']);

      DragDropService._onDrop(componentElement1, containerElement1, containerElement1, undefined);

      expect(HstService.doPost).toHaveBeenCalledWith(container1.getHstRepresentation(), 'container1', 'update');
      expect(componentIds(container1)).toEqual(['component2', 'component1']);

      done();
    });
  });

  it('can move the second component to the first position in the container', (done) => {
    loadIframeFixture(() => {
      const componentElement1 = component1.getBoxElement();
      const componentElement2 = component2.getBoxElement();
      const containerElement = container1.getBoxElement();

      spyOn(HstService, 'doPost');
      expect(componentIds(container1)).toEqual(['component1', 'component2']);

      DragDropService._onDrop(componentElement2, containerElement, containerElement, componentElement1);

      expect(HstService.doPost).toHaveBeenCalledWith(container1.getHstRepresentation(), 'container1', 'update');
      expect(componentIds(container1)).toEqual(['component2', 'component1']);

      done();
    });
  });

  it('can move a component to another container', (done) => {
    loadIframeFixture(() => {
      const componentElement1 = component1.getBoxElement();
      const containerElement1 = container1.getBoxElement();
      const containerElement2 = container2.getBoxElement();

      spyOn(HstService, 'doPost');
      expect(componentIds(container1)).toEqual(['component1', 'component2']);
      expect(componentIds(container2)).toEqual([]);

      DragDropService._onDrop(componentElement1, containerElement2, containerElement1, undefined);

      expect(HstService.doPost).toHaveBeenCalledWith(container1.getHstRepresentation(), 'container1', 'update');
      expect(HstService.doPost).toHaveBeenCalledWith(container2.getHstRepresentation(), 'container2', 'update');
      expect(componentIds(container1)).toEqual(['component2']);
      expect(componentIds(container2)).toEqual(['component1']);

      done();
    });
  });

  it('does not register a disabled container', () => {
    spyOn(container1, 'isDisabled').and.returnValue(true);
    loadIframeFixture(() => {
      expect(DragDropService.drake.containers).toEqual([container2.getBoxElement()]);
    });
  });

  it('shows an error when a component is moved within a container that just got locked by another user', (done) => {
    loadIframeFixture(() => {
      spyOn(HstService, 'doPost').and.returnValue($q.reject());
      spyOn(FeedbackService, 'showError');

      // re-render container1 with a different DOM element so we can verify the element got changed
      const rerenderedContainer1 = createContainer(3);
      spyOn(PageStructureService, 'renderContainer').and.returnValue($q.resolve(rerenderedContainer1));

      const componentElement1 = component1.getBoxElement();
      const containerElement1 = container1.getBoxElement();
      DragDropService._onDrop(componentElement1, containerElement1, containerElement1, undefined);

      // wait until the current $digest is done before expecting results
      setTimeout(() => {
        expect(HstService.doPost).toHaveBeenCalledWith(container1.getHstRepresentation(), 'container1', 'update');
        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
          component: 'Component 1',
        });
        expect(DragDropService.drake.containers).toEqual([
          rerenderedContainer1.getBoxElement(),
          container2.getBoxElement(),
        ]);
        done();
      }, 0);
    });
  });

  it('shows an error when a component is moved out of a container that just got locked by another user', (done) => {
    loadIframeFixture(() => {
      spyOn(HstService, 'doPost').and.returnValues($q.reject(), $q.resolve());
      spyOn(FeedbackService, 'showError');

      // re-render container1 and container2 with a different DOM elements so we can verify the elements got changed
      const rerenderedContainer1 = createContainer(3);
      const rerenderedContainer2 = createContainer(4);
      spyOn(PageStructureService, 'renderContainer').and.returnValues(
        $q.resolve(rerenderedContainer1),
        $q.resolve(rerenderedContainer2)
      );

      const componentElement1 = component1.getBoxElement();
      const containerElement1 = container1.getBoxElement();
      const containerElement2 = container2.getBoxElement();
      DragDropService._onDrop(componentElement1, containerElement2, containerElement1, undefined);

      // wait until the current $digest is done before expecting results
      setTimeout(() => {
        expect(HstService.doPost).toHaveBeenCalledWith(container1.getHstRepresentation(), 'container1', 'update');
        expect(HstService.doPost).toHaveBeenCalledWith(container2.getHstRepresentation(), 'container2', 'update');
        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
          component: 'Component 1',
        });
        expect(DragDropService.drake.containers).toEqual([
          rerenderedContainer1.getBoxElement(),
          rerenderedContainer2.getBoxElement(),
        ]);
        done();
      }, 0);
    });
  });

  it('shows an error when a component is moved into a container that just got locked by another user', (done) => {
    loadIframeFixture(() => {
      spyOn(HstService, 'doPost').and.returnValues($q.resolve(), $q.reject());
      spyOn(FeedbackService, 'showError');

      // re-render container1 and container2 with a different DOM elements so we can verify the elements got changed
      const rerenderedContainer1 = createContainer(3);
      const rerenderedContainer2 = createContainer(4);
      spyOn(PageStructureService, 'renderContainer').and.returnValues(
        $q.resolve(rerenderedContainer1),
        $q.resolve(rerenderedContainer2)
      );

      const componentElement1 = component1.getBoxElement();
      const containerElement1 = container1.getBoxElement();
      const containerElement2 = container2.getBoxElement();
      DragDropService._onDrop(componentElement1, containerElement2, containerElement1, undefined);

      // wait until the current $digest is done before expecting results
      setTimeout(() => {
        expect(HstService.doPost).toHaveBeenCalledWith(container1.getHstRepresentation(), 'container1', 'update');
        expect(HstService.doPost).toHaveBeenCalledWith(container2.getHstRepresentation(), 'container2', 'update');
        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
          component: 'Component 1',
        });
        expect(DragDropService.drake.containers).toEqual([
          rerenderedContainer1.getBoxElement(),
          rerenderedContainer2.getBoxElement(),
        ]);
        done();
      }, 0);
    });
  });
});
