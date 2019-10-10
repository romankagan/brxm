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

import React from 'react';
import { shallow } from 'enzyme';
import { Menu, Page, Meta } from '@bloomreach/spa-sdk';
import { BrManageMenuButton } from './BrManageMenuButton';
import { BrMetaWrapper } from '../meta';

describe('BrManageMenuButton', () => {
  const context = ({
    isPreview: jest.fn(),
    getMeta: jest.fn(),
  } as unknown) as jest.Mocked<Page>;
  const props = { menu: ({} as unknown) as jest.Mocked<Menu> };

  beforeEach(() => {
    jest.restoreAllMocks();

    props.menu._meta = undefined;

    // @see https://github.com/airbnb/enzyme/issues/1553
    /// @ts-ignore
    BrManageMenuButton.contextTypes = {
      isPreview: () => false,
      getMeta: () => null,
    };
    delete BrManageMenuButton.contextType;
  });

  it('should only render in preview mode', () => {
    context.isPreview.mockReturnValueOnce(false);

    const wrapper = shallow(<BrManageMenuButton {...props} />, { context });
    expect(wrapper.html()).toBe(null);
  });

  it('should only render if meta-data is available', () => {
    context.isPreview.mockReturnValueOnce(true);

    const wrapper = shallow(<BrManageMenuButton {...props} />, { context });
    expect(wrapper.html()).toBe(null);
  });

  it('should render menu-button meta-data created with page context', () => {
    const meta: Meta[] = [];
    context.getMeta.mockReturnValueOnce(meta);
    context.isPreview.mockReturnValueOnce(true);
    props.menu._meta = {};

    const wrapper = shallow(<BrManageMenuButton {...props} />, { context });
    expect(context.getMeta).toHaveBeenCalledWith(props.menu._meta);
    expect(
      wrapper
        .find(BrMetaWrapper)
        .first()
        .prop('meta'),
    ).toBe(meta);
  });
});
