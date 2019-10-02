/*
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
import { BehaviorSubject, Observable } from 'rxjs';

import { NavItemService } from '../../services/nav-item.service';
import { MenuItemContainer } from '../models/menu-item-container.model';
import { MenuItemLink } from '../models/menu-item-link.model';
import { MenuItem } from '../models/menu-item.model';

import { MenuBuilderService } from './menu-builder.service';

@Injectable()
export class MenuStateService {
  private menuItems: MenuItem[] = [];
  private homeMenuItemLink: MenuItemLink;
  private activePath = new BehaviorSubject<MenuItem[]>([]);
  private collapsed = true;
  private currentDrawerMenuItem: MenuItemContainer;

  constructor(
    private menuBuilderService: MenuBuilderService,
    private navItemService: NavItemService,
  ) { }

  get menu(): MenuItem[] {
    return this.menuItems;
  }

  get homeMenuItem(): MenuItemLink {
    return this.homeMenuItemLink;
  }

  get activePath$(): Observable<MenuItem[]> {
    return this.activePath.asObservable();
  }

  get isMenuCollapsed(): boolean {
    return this.collapsed;
  }

  get isDrawerOpened(): boolean {
    return !!this.currentDrawerMenuItem;
  }

  get drawerMenuItem(): MenuItemContainer {
    return this.currentDrawerMenuItem;
  }

  init(): void {
    const navItems = this.navItemService.navItems;
    this.menuItems = this.menuBuilderService.buildMenu(navItems);
    this.homeMenuItemLink = this.findHomeMenuItemLink(this.menuItems);
  }

  deactivateMenuItem(): void {
    this.activePath.next([]);
  }

  activateMenuItem(appId: string, path: string): void {
    const navItem = this.navItemService.findNavItem(appId, path);

    if (!navItem) {
      throw new Error(`It's impossible to find an appropriate menu element for appId=${appId} and path=${path}`);
    }

    this.setActiveItem(navItem.id);
  }

  isMenuItemActive(item: MenuItem): boolean {
    const currentBreadcrumbs = this.activePath.value;

    return currentBreadcrumbs.some(x => x === item);
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
  }

  openDrawer(item: MenuItemContainer): void {
    this.currentDrawerMenuItem = item;
  }

  closeDrawer(): void {
    this.currentDrawerMenuItem = undefined;
  }

  private setActiveItem(activeItemId: string): void {
    this.closeDrawer();

    const prevActivePath = this.activePath.value;
    const activePath = this.buildActivePath(this.menuItems, activeItemId);

    const arePathsEqual = prevActivePath &&
      prevActivePath.length === activePath.length &&
      prevActivePath.every((x, i) => x === activePath[i]);

    if (!arePathsEqual) {
      this.activePath.next(activePath);
    }
  }

  private buildActivePath(
    menu: MenuItem[],
    activeMenuItemId: string,
  ): MenuItem[] {
    return menu.reduce((activePath, item) => {
      if (item instanceof MenuItemContainer) {
        const subActivePath = this.buildActivePath(
          item.children,
          activeMenuItemId,
        );

        if (subActivePath.length > 0) {
          subActivePath.unshift(item);
          activePath = activePath.concat(subActivePath);
        }
      } else if (item.id === activeMenuItemId) {
        activePath.push(item);
      }

      return activePath;
    }, []);
  }

  private findHomeMenuItemLink(menu: MenuItem[]): MenuItemLink {
    if (menu.length === 0) {
      return;
    }

    const firstMenuItem = menu[0];

    if (firstMenuItem instanceof MenuItemLink) {
      return firstMenuItem;
    }

    if (firstMenuItem instanceof MenuItemContainer) {
      return this.findHomeMenuItemLink(firstMenuItem.children);
    }
  }
}
