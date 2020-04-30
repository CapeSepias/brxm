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

import { mocked } from 'ts-jest/utils';
import { SimpleChange, NO_ERRORS_SCHEMA } from '@angular/core';
import { async, getTestBed, ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { destroy, initialize, isPage, Component, Configuration, Page, PageModel } from '@bloomreach/spa-sdk';

import { BrNodeTypePipe } from '../br-node-type.pipe';
import { BrPageComponent } from './br-page.component';

jest.mock('@bloomreach/spa-sdk');

describe('BrPageComponent', () => {
  let component: BrPageComponent;
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<BrPageComponent>;
  let page: jest.Mocked<Page>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ BrNodeTypePipe, BrPageComponent ],
      imports: [ HttpClientTestingModule ],
      schemas: [ NO_ERRORS_SCHEMA ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    httpMock = getTestBed().get(HttpTestingController);
    fixture = TestBed.createComponent(BrPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    page = {
      getComponent: jest.fn(),
      sync: jest.fn(),
    } as unknown as jest.Mocked<Page>;

    jest.resetAllMocks();

    mocked(isPage).mockImplementation((value) => value === page);
  });

  describe('context', () => {
    it('should be undefined when the state is not set', () => {
      expect(component.context).toBeUndefined();
    });

    it('should be undefined when there is no root component', () => {
      component.state.next(page);

      expect(component.context).toBeUndefined();
    });

    it('should be undefined when there is no root component', () => {
      component.state.next(page);

      expect(component.context).toBeUndefined();
    });

    it('should contain a root component', () => {
      const root = {} as Component;
      page.getComponent.mockReturnValue(root);
      component.state.next(page);

      expect(component.context?.$implicit).toBe(root);
      expect(component.context?.component).toBe(root);
    });

    it('should contain a page object', () => {
      page.getComponent.mockReturnValue({} as Component);
      component.state.next(page);

      expect(component.context?.page).toBe(page);
    });
  });

  describe('ngAfterContentChecked', () => {
    it('should sync a page', () => {
      component.state.next(page);
      component.ngAfterContentChecked();

      expect(page.sync).toBeCalled();
    });

    it('should not sync a page twice', () => {
      component.state.next(page);
      component.ngAfterContentChecked();
      component.ngAfterContentChecked();

      expect(page.sync).toBeCalledTimes(1);
    });

    it('should not fail if the page is not ready', () => {
      expect(() => component.ngAfterContentChecked()).not.toThrow();
    });
  });

  describe('ngOnChanges', () => {
    it('should use a page instance from inputs when configuraton is changed', () => {
      component.configuration = {} as Configuration;
      component.page = page;
      component.ngOnChanges({
        configuration: new SimpleChange(undefined, component.configuration, true),
        page: new SimpleChange(undefined, component.page, true),
      });

      expect(initialize).not.toBeCalled();
      expect(component.state.getValue()).toBe(component.page);
    });

    it('should destroy a previous page', () => {
      const previousPage = {} as Page;

      mocked(isPage).mockImplementation(Array.prototype.includes.bind([page, previousPage]));
      component.configuration = {} as Configuration;
      component.page = page;
      component.state.next(previousPage);

      component.ngOnChanges({
        configuration: new SimpleChange({}, component.configuration, false),
        page: new SimpleChange(previousPage, component.page, false),
      });

      expect(destroy).toBeCalledWith(previousPage);
    });

    it('should initialize a new page when a page input was not changed', () => {
      mocked(initialize).mockResolvedValueOnce(page);
      component.configuration = {} as Configuration;
      component.page = page;
      component.ngOnChanges({
        configuration: new SimpleChange({}, component.configuration, false),
      });

      expect(initialize).toBeCalled();
    });

    it('should use a page instance from inputs when configuration was not changed', () => {
      component.page = page;
      component.ngOnChanges({
        page: new SimpleChange(undefined, component.page, true),
      });

      expect(initialize).not.toBeCalled();
      expect(component.state.getValue()).toBe(component.page);
    });

    it('should initialize a page from the configuration', async () => {
      mocked(initialize).mockResolvedValueOnce(page);
      component.configuration = { cmsBaseUrl: 'something' } as Configuration;
      component.ngOnChanges({
        configuration: new SimpleChange(undefined, component.configuration, true),
      });

      await new Promise(process.nextTick);

      expect(initialize).toBeCalledWith(
        expect.objectContaining({
          cmsBaseUrl: 'something',
          httpClient: expect.any(Function),
        }),
        undefined,
      );
      expect(component.state.getValue()).toBe(page);
    });

    it('should initialize a page from the page model', async () => {
      mocked(initialize).mockResolvedValueOnce(page);
      component.configuration = { cmsBaseUrl: 'something' } as Configuration;
      component.page = {} as PageModel;
      component.ngOnChanges({
        configuration: new SimpleChange(undefined, component.configuration, true),
        page: new SimpleChange(undefined, component.page, true),
      });

      await new Promise(process.nextTick);

      expect(initialize).toBeCalledWith(expect.any(Object), component.page);
      expect(component.state.getValue()).toBe(page);
    });

    it('should pass a compatible http client', () => {
      mocked(initialize).mockResolvedValueOnce(page);
      component.ngOnChanges({
        configuration: new SimpleChange(undefined, {}, true),
      });

      const [[{ httpClient }]] = mocked(initialize).mock.calls;
      const response = httpClient({
        data: 'something',
        headers: { 'Some-Header': 'value' },
        method: 'POST',
        url: 'http://www.example.com',
      });
      const request = httpMock.expectOne('http://www.example.com');

      expect(request.request.headers.get('Some-Header')).toBe('value');
      expect(request.request.body).toBe('something');
      expect(request.request.method).toBe('POST');
      expect(request.request.url).toBe('http://www.example.com');

      request.flush('something');

      expect(response).resolves.toEqual({ data: 'something' });
    });
  });

  describe('ngOnDestroy', () => {
    it('should destroy a stored page', () => {
      component.state.next(page);
      component.ngOnDestroy();

      expect(destroy).toBeCalledWith(page);
    });

    it('should not destroy a page if it was not initialized', () => {
      component.ngOnDestroy();

      expect(destroy).not.toBeCalled();
    });
  });
});
