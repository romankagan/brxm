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

import { Location } from '@angular/common';
import { Inject, Injectable, OnDestroy } from '@angular/core';
import { NavigationTrigger, NavItem, NavLocation } from '@bloomreach/navapp-communication';
import { EMPTY, Observable, of, Subject, Subscription, throwError } from 'rxjs';
import { fromPromise } from 'rxjs/internal-compatibility';
import { catchError, finalize, mapTo, switchMap, tap } from 'rxjs/operators';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { AppError } from '../error-handling/models/app-error';
import { CriticalError } from '../error-handling/models/critical-error';
import { InternalError } from '../error-handling/models/internal-error';
import { NotFoundError } from '../error-handling/models/not-found-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { AppSettings } from '../models/dto/app-settings.dto';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { APP_SETTINGS } from './app-settings';
import { BusyIndicatorService } from './busy-indicator.service';
import { ConnectionService } from './connection.service';
import { NavItemService } from './nav-item.service';
import { UrlMapperService } from './url-mapper.service';

interface Route {
  path: string;
  navItem: NavItem;
}

interface Navigation {
  url: string;
  navItem: NavItem;
  appPathAddOn: string;
  state: { [key: string]: string };
  source: NavigationTrigger;
  app: ClientApp;
  replaceState: boolean;
  resolve: () => void;
  reject: (reason?: any) => void;
}

type Transition = Partial<Navigation>;

@Injectable({
  providedIn: 'root',
})
export class NavigationService implements OnDestroy {
  private routes: Route[];
  private locationSubscription: Subscription;
  private readonly transitions = new Subject<Transition | Error>();
  private currentNavItem: NavItem;

  constructor(
    @Inject(APP_SETTINGS) private appSettings: AppSettings,
    private breadcrumbsService: BreadcrumbsService,
    private busyIndicatorService: BusyIndicatorService,
    private clientAppService: ClientAppService,
    private connectionService: ConnectionService,
    private errorHandlingService: ErrorHandlingService,
    private location: Location,
    private menuStateService: MenuStateService,
    private navItemService: NavItemService,
    private urlMapperService: UrlMapperService,
  ) {
    this.connectionService
      .navigate$
      .subscribe(navLocation => this.navigateByNavLocation(navLocation, NavigationTrigger.AnotherApp));

    this.connectionService
      .updateNavLocation$
      .subscribe(navLocation => this.updateByNavLocation(navLocation));

    this.setupNavigations();
  }

  private get basePath(): string {
    return this.appSettings.basePath;
  }

  private get homeUrl(): string {
    const homeMenuItem = this.menuStateService.homeMenuItem;

    if (!homeMenuItem) {
      throw new CriticalError('Configuration error', 'Unable to find home item');
    }

    return this.urlMapperService.mapNavItemToBrowserUrl(homeMenuItem.navItem);
  }

  initialNavigation(): Promise<void> {
    const navItems = this.navItemService.navItems;
    this.routes = this.generateRoutes(navItems);

    this.setUpLocationChangeListener();

    const url = `${this.basePath}${this.appSettings.initialPath}`;

    return this.scheduleNavigation(url, NavigationTrigger.NotDefined, {}, true);
  }

  ngOnDestroy(): void {
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }
  }

  navigateByNavItem(navItem: NavItem, triggeredBy: NavigationTrigger, breadcrumbLabel?: string): Promise<void> {
    const browserUrl = this.urlMapperService.mapNavItemToBrowserUrl(navItem);
    return this.navigateByUrl(browserUrl, triggeredBy, breadcrumbLabel);
  }

  navigateByNavLocation(navLocation: NavLocation, triggeredBy: NavigationTrigger): Promise<void> {
    let browserUrl: string;

    try {
      browserUrl = this.urlMapperService.mapNavLocationToBrowserUrl(navLocation, false)[0];
    } catch (e) {
      this.errorHandlingService.setNotFoundError(
        undefined,
        `An attempt to navigate was failed due to app path is not allowable: '${navLocation.path}'`,
      );

      return;
    }

    return this.navigateByUrl(browserUrl, triggeredBy, navLocation.breadcrumbLabel);
  }

  updateByNavLocation(navLocation: NavLocation): void {
    let browserUrl: string;
    let navItem: NavItem;

    try {
      [browserUrl, navItem] = this.urlMapperService.mapNavLocationToBrowserUrl(navLocation, true);
    } catch (e) {
      this.errorHandlingService.setNotFoundError(
        undefined,
        `An attempt to update the app url was failed due to app path is not allowable: '${navLocation.path}'`,
      );

      return;
    }

    this.currentNavItem = navItem;
    this.menuStateService.activateMenuItem(navItem.appIframeUrl, navItem.appPath);
    this.breadcrumbsService.setSuffix(navLocation.breadcrumbLabel);

    this.setBrowserUrl(browserUrl, { breadcrumbLabel: navLocation.breadcrumbLabel });
  }

  navigateToDefaultAppPage(triggeredBy: NavigationTrigger): Promise<void> {
    if (!this.currentNavItem) {
      return Promise.resolve();
    }

    return this.navigateByNavItem(this.currentNavItem, triggeredBy, '');
  }

  navigateToHome(triggeredBy: NavigationTrigger): Promise<void> {
    return this.navigateByUrl(this.homeUrl, triggeredBy);
  }

  private navigateByUrl(url: string, triggeredBy: NavigationTrigger, breadcrumbLabel?: string): Promise<void> {
    this.errorHandlingService.clearError();

    return this.scheduleNavigation(url, triggeredBy, { breadcrumbLabel });
  }

  private setUpLocationChangeListener(): void {
    if (this.locationSubscription) {
      return;
    }

    this.locationSubscription = this.location.subscribe(change => {
      this.errorHandlingService.clearError();

      this.scheduleNavigation(change.url, NavigationTrigger.PopState, change.state);
    }) as any;
  }

  private setupNavigations(): void {
    this.transitions.pipe(
      tap(() => {
        this.busyIndicatorService.show();
      }),
      switchMap((t: Transition) => this.processTransition(t).pipe(
        catchError(error => {
          if (typeof error === 'string') {
            error = new InternalError(undefined, error);
          }

          return of(error);
        }),
        finalize(() => {
          // Always resolve a promise (for now) to overcome consequent problems of handling promise rejection
          // t.reject(error);
          t.resolve();

          this.busyIndicatorService.hide();
        }),
      )),
    ).subscribe((t: Navigation | Error) => {
      if (t instanceof AppError) {
        this.errorHandlingService.setError(t);

        return;
      }

      if (t instanceof Error) {
        this.errorHandlingService.setInternalError(undefined, t.message);

        return;
      }
    });
  }

  private scheduleNavigation(
    url: string,
    source: NavigationTrigger,
    state: { [key: string]: string } = {},
    replaceState = false,
  ): Promise<void> {
    let resolve: () => void;
    let reject: () => void;

    const promise = new Promise<void>((res, rej) => {
      resolve = res;
      reject = rej;
    });

    this.transitions.next({
      url,
      state,
      source,
      replaceState,
      resolve,
      reject,
    });

    return promise;
  }

  private processTransition(transition: Transition): Observable<Navigation> {
    return of(transition).pipe(
      // Eagerly update the browser url
      tap(t => {
        if (t.source !== NavigationTrigger.PopState) {
          this.setBrowserUrl(t.url, t.state, t.replaceState);
        }
      }),
      // Resolving the url
      switchMap((t: Transition) => {
        const route = this.matchRoute(t.url);

        if (!route) {
          this.menuStateService.deactivateMenuItem();
          this.currentNavItem = undefined;

          return throwError(new NotFoundError(`Unknown url: ${t.url}`));
        }

        if (!t.url.startsWith(route.path)) {
          t.url = route.path;
          t.state = {};
        }

        const appPathAddOn = t.url.slice(route.path.length);

        return of({ ...t, navItem: route.navItem, appPathAddOn });
      }),
      // Ensure the app with the found id exists and it has the connected API
      switchMap(t => {
        const appId = t.navItem.appIframeUrl;
        const app = this.clientAppService.getApp(appId);

        if (!app) {
          return throwError(new NotFoundError(
            undefined,
            `There is no app with id="${appId}"`,
          ));
        }

        if (!app.api) {
          return throwError(new InternalError(
            undefined,
            `The app with id="${appId}" is not connected to the nav app`,
          ));
        }

        return of({ ...t, app });
      }),
      // Process beforeNavigation
      switchMap(t => {
        if (!t.app.api.beforeNavigation) {
          return of(t);
        }

        return fromPromise(t.app.api.beforeNavigation()).pipe(
          switchMap(allowedToContinue => allowedToContinue ? of(t) : EMPTY),
        );
      }),
      // Eagerly update the menu and the breadcrumb label
      tap(t => {
        const { breadcrumbLabel } = t.state;

        this.currentNavItem = t.navItem;
        this.menuStateService.activateMenuItem(t.navItem.appIframeUrl, t.navItem.appPath);
        this.breadcrumbsService.setSuffix(breadcrumbLabel);
      }),
      // Activate the app
      tap(t => {
        const appId = t.navItem.appIframeUrl;

        this.clientAppService.activateApplication(appId);
      }),
      // Process navigation
      switchMap(t => {
        const appPath = Location.joinWithSlash(t.navItem.appPath, t.appPathAddOn);
        const appPathWithoutLeadingSlash = this.urlMapperService.trimLeadingSlash(appPath);
        const appPathPrefix = new URL(t.navItem.appIframeUrl).pathname;

        const navigationPromise = t.app.api.navigate(
          { pathPrefix: appPathPrefix, path: appPathWithoutLeadingSlash },
          t.source,
        );

        return fromPromise(navigationPromise).pipe(
          mapTo(t as Navigation),
        );
      }),
    );
  }

  private setBrowserUrl(url: string, state: { [key: string]: any }, replaceState = false): void {
    if (!state.breadcrumbLabel) {
      delete state.breadcrumbLabel;
    }

    if (this.location.isCurrentPathEqualTo(url) || replaceState) {
      this.location.replaceState(url, '', state);
    } else {
      this.location.go(url, '', state);
    }
  }

  private generateRoutes(navItems: NavItem[]): Route[] {
    return navItems.map(navItem => ({
      path: this.urlMapperService.mapNavItemToBrowserUrl(navItem),
      navItem,
    }));
  }

  private matchRoute(url: string): Route {
    if (Location.stripTrailingSlash(url) === Location.stripTrailingSlash(this.basePath)) {
      url = this.homeUrl;
    }

    return this.routes.find(x => url.startsWith(x.path));
  }
}
