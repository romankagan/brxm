/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { AfterContentChecked, Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { destroy, initialize, isPage, Configuration, Page, PageModel } from '@bloomreach/spa-sdk';
import { from } from 'rxjs';
import { map } from 'rxjs/operators';

/**
 * The brXM page.
 */
@Component({
  selector: 'br-page',
  templateUrl: './br-page.component.html'
})
export class BrPageComponent implements AfterContentChecked, OnChanges, OnDestroy {
  /**
   * The configuration of the SPA SDK.
   * @see https://www.npmjs.com/package/@bloomreach/spa-sdk#configuration
   */
  @Input() configuration!: Omit<Configuration, 'httpClient'>;

  /**
   * The pre-initialized page instance or prefetched page model.
   * Mostly this property should be used to transfer state from the server-side to the client-side.
   */
  @Input() page?: Page | PageModel;

  private instance?: Page = undefined;
  private isSynced = false;

  constructor(private httpClient: HttpClient) {
    this.request = this.request.bind(this);
  }

  ngAfterContentChecked(): void {
    if (this.isSynced) {
      return;
    }

    this.state?.sync();
    this.isSynced = true;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.configuration?.currentValue !== changes.configuration?.previousValue
      || changes.page?.currentValue !== changes.page?.previousValue
    ) {
      this.destroy();
      this.initialize(changes.page?.currentValue === changes.page?.previousValue);
    }
  }

  ngOnDestroy() {
    this.destroy();
  }

  get state(): Page | undefined {
    return this.instance;
  }

  set state(value: Page | undefined) {
    this.instance = value;
    this.isSynced = false;
  }

  private destroy() {
    if (!this.instance) {
      return;
    }

    destroy(this.instance);
    delete this.instance;
  }

  private initialize(force: boolean) {
    const page = force ? undefined : this.page;

    if (isPage(page)) {
      this.state = page;

      return;
    }

    from(initialize({ httpClient: this.request, ...this.configuration } as Configuration, page))
      .subscribe(state => { this.state = state; });
  }

  private request(...[{ data: body, headers, method, url }]: Parameters<Configuration['httpClient']>) {
    return this.httpClient.request<PageModel>(
      method,
      url,
      {
        body,
        headers: headers as Record<string, string | string[]>,
        responseType: 'json'
      },
    )
    .pipe(map(data => ({data})))
    .toPromise();
  }
}
