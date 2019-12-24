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

import { Injectable } from '@angular/core';
import { NavItem } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';

@Injectable({
  providedIn: 'root',
})
export class NavItemService {
  private sortedNavItems: NavItem[] = [];

  constructor(private readonly logger: NGXLogger) {}

  get navItems(): NavItem[] {
    return this.sortedNavItems;
  }

  set navItems(value: NavItem[]) {
    this.sortedNavItems = value.slice();

    this.sortedNavItems.sort((a, b) => b.appPath.length - a.appPath.length);
  }

  findNavItem(path: string, iframeUrlOrPath?: string): NavItem {
    let isIframeUrl = false;

    try {
      isIframeUrl = !!new URL(iframeUrlOrPath).pathname;
    } catch {}

    const appPathPredicate = (x: NavItem) => path.startsWith(x.appPath);
    const iframeUrlPredicate = (x: NavItem) => {
      if (isIframeUrl) {
        return x.appIframeUrl === iframeUrlOrPath;
      }

      try {
        return new URL(x.appIframeUrl).pathname === iframeUrlOrPath;
      } catch {
        this.logger.warn(`Unable to parse nav items's url "${x.appIframeUrl}"`);
        return false;
      }
    };

    return this.navItems.find(x => iframeUrlOrPath ? iframeUrlPredicate(x) && appPathPredicate(x) : appPathPredicate(x));
  }
}
