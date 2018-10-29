/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import Penpal from 'penpal';

interface UiProperties {
  baseUrl: string;
  extension: {
    config: string,
  },
  locale: string;
  timeZone: string;
  user: string;
  version: string;
}

interface UiExtensionParent {
  getProperties: () => Promise<UiProperties>,
}

type PenpalError = Error & { code?: string };

enum UiExtensionErrorCode {
  'NotInIframe' = 'NotInIframe',
  'IncompatibleParent' = 'IncompatibleParent',
  'ConnectionDestroyed' = 'ConnectionDestroyed',
  'InternalError' = 'InternalError',
}

class UiExtensionError {
  constructor(public code: UiExtensionErrorCode,
              public message: string) {
  }

  static fromPenpalError(error: PenpalError): UiExtensionError {
    const errorCode = UiExtensionError.convertPenpalErrorCode(error);
    return new UiExtensionError(errorCode, error.message);
  }

  static convertPenpalErrorCode(error: PenpalError): UiExtensionErrorCode {
    switch (error.code) {
      case Penpal.ERR_NOT_IN_IFRAME:
        return UiExtensionErrorCode.NotInIframe;
      case Penpal.ERR_CONNECTION_DESTROYED:
        return UiExtensionErrorCode.ConnectionDestroyed;
      default:
        return UiExtensionErrorCode.InternalError;
    }
  }
}

const convertPenpalError = (error: PenpalError): Promise<any> => {
  return Promise.reject(UiExtensionError.fromPenpalError(error));
};

abstract class UiScope {
  constructor(protected parent: UiExtensionParent) {
  }
}

class Ui extends UiScope implements UiProperties {
  baseUrl: string;
  extension: {
    config: string,
  };
  locale: string;
  timeZone: string;
  user: string;
  version: string;

  init() {
    if (!this.parent.getProperties) {
      return Promise.reject(new UiExtensionError(UiExtensionErrorCode.IncompatibleParent, 'missing getProperties()'))
    }
    try {
      return this.parent.getProperties()
        .then((properties) => {
          Object.assign(this, properties);
          return this;
        })
        .catch(convertPenpalError)
    } catch (error) {
      return convertPenpalError(error);
    }
  }
}

type UiExtensionConfig = {
  Promise?: typeof Promise,
}

export default class UiExtension {
  static register(config: UiExtensionConfig = {}) {
    if (config.Promise) {
      Penpal.Promise = config.Promise;
    }

    return UiExtension.connect()
      .then((parent: UiExtensionParent) => {
        const ui = new Ui(parent);
        return ui.init();
      });
  }

  private static connect(): Promise<UiExtensionParent> {
    const parentOrigin = new URLSearchParams(window.location.search).get('br.parentOrigin');
    try {
      return Penpal.connectToParent({
        parentOrigin,
      }).promise;
    } catch (penpalError) {
      return convertPenpalError(penpalError);
    }
  }
}

// enable UiExtension.register() in ui-extension.min.js
export const register = UiExtension.register;
