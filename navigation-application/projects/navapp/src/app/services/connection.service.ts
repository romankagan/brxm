/*!
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Inject, Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { ChildPromisedApi, ClientError, connectToChild, NavLocation, ParentApi } from '@bloomreach/navapp-communication';
import { Subject } from 'rxjs';

import { version } from '../../../../../package.json';
import { AppSettings } from '../models/dto/app-settings.dto';
import { UserSettings } from '../models/dto/user-settings.dto';

import { APP_SETTINGS } from './app-settings';
import { USER_SETTINGS } from './user-settings';

export interface ChildConnection {
  url: string;
  iframe: HTMLIFrameElement;
  api: ChildPromisedApi;
}

@Injectable({
  providedIn: 'root',
})
export class ConnectionService {
  private connections = new Map<string, ChildConnection>();
  private renderer: Renderer2 = this.rendererFactory.createRenderer(undefined, undefined);

  showMask$ = new Subject<void>();
  hideMask$ = new Subject<void>();
  navigate$ = new Subject<NavLocation>();
  updateNavLocation$ = new Subject<NavLocation>();
  onError$ = new Subject<ClientError>();
  onUserActivity$ = new Subject<void>();
  onSessionExpired$ = new Subject<string>();

  constructor(
    @Inject(APP_SETTINGS) private appSettings: AppSettings,
    @Inject(USER_SETTINGS) private userSettings: UserSettings,
    private rendererFactory: RendererFactory2,
  ) { }

  get parentApiMethods(): ParentApi {
    return {
      getConfig: () => ({
        apiVersion: version,
        userSettings: this.userSettings,
      }),
      showMask: () => this.showMask$.next(),
      hideMask: () => this.hideMask$.next(),
      navigate: (location: NavLocation) => this.navigate$.next(location),
      updateNavLocation: (location: NavLocation) => this.updateNavLocation$.next(location),
      onError: (clientError: ClientError) => this.onError$.next(clientError),
      onSessionExpired: () => this.onSessionExpired$.next('SessionExpired'),
      onUserActivity: () => this.onUserActivity$.next(),
    };
  }

  getConnection(url: string): ChildConnection {
    return this.connections.get(url);
  }

  async createConnection(url: string): Promise<ChildConnection> {
    const iframe = document.createElement('iframe');
    iframe.src = url;
    iframe.style.visibility = 'hidden';
    iframe.style.position = 'absolute';
    iframe.style.width = '1px';
    iframe.style.height = '1px';
    this.renderer.appendChild(document.body, iframe);

    return await this.connectToIframe(iframe);
  }

  removeConnection(url: string): void {
    const connection = this.connections.get(url);

    if (!connection) {
      throw new Error(`Connection to ${url} does not exist`);
    }

    this.connections.delete(url);
    this.renderer.removeChild(document.body, connection.iframe);
  }

  async connectToIframe(iframe: HTMLIFrameElement): Promise<ChildConnection> {
    const url = iframe.src;
    const config = {
      iframe,
      methods: this.parentApiMethods,
      timeout: this.appSettings.iframesConnectionTimeout,
    };

    try {
      const api = await connectToChild(config);
      const connection: ChildConnection = { url, iframe, api };
      this.connections.set(url, connection);

      return connection;
    } catch (error) {
      throw new Error(`Could not create connection for ${url}: ${error}`);
    }
  }
}
