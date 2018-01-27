/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('CreateContentService', () => {
  let $q;
  let $rootScope;
  let $state;
  let $translate;
  let $window;
  let ContentService;
  let CreateContentService;
  let EditContentService;
  let FeedbackService;
  let HippoIframeService;
  let RightSidePanelService;
  let Step1Service;
  let Step2Service;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    ContentService = jasmine.createSpyObj('ContentService', ['_send']);
    ContentService._send.and.returnValue(Promise.resolve());

    RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', ['startLoading', 'stopLoading', 'setTitle']);

    angular.mock.module(($provide) => {
      $provide.value('ContentService', ContentService);
      $provide.value('RightSidePanelService', RightSidePanelService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _$state_,
      _$translate_,
      _$window_,
      _CreateContentService_,
      _EditContentService_,
      _FeedbackService_,
      _HippoIframeService_,
      _Step1Service_,
      _Step2Service_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $translate = _$translate_;
      $window = _$window_;
      CreateContentService = _CreateContentService_;
      EditContentService = _EditContentService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      Step1Service = _Step1Service_;
      Step2Service = _Step2Service_;
    });

    spyOn($translate, 'instant').and.callThrough();
  });

  it('starts creating a new document', () => {
    spyOn(Step1Service, 'open').and.returnValue($q.resolve());
    const config = { templateQuery: 'tpl-query' };
    CreateContentService.start(config);
    $rootScope.$digest();

    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('CREATE_CONTENT');
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(Step1Service.open).toHaveBeenCalledWith('tpl-query', undefined, undefined);
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
    expect(CreateContentService.componentInfo).toEqual({});
  });

  it('starts creating a new document for a component', () => {
    spyOn(Step1Service, 'open').and.returnValue($q.resolve());

    const component = jasmine.createSpyObj('Component', ['getId', 'getLabel']);
    component.getId.and.returnValue('1234');
    component.getLabel.and.returnValue('Banner');
    const config = {
      templateQuery: 'tpl-query',
      containerItem: component,
      componentParameter: 'document',
      componentParameterBasePath: '/content/documents/channel',
    };
    CreateContentService.start(config);
    $rootScope.$digest();

    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('CREATE_CONTENT');
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(Step1Service.open).toHaveBeenCalledWith('tpl-query', undefined, undefined);
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
    expect(CreateContentService.componentInfo).toEqual({
      id: '1234',
      label: 'Banner',
      parameterName: 'document',
      parameterBasePath: '/content/documents/channel',
    });
  });

  it('opens the second step of creating a new document', () => {
    const docType = { displayName: 'document-type-name' };
    spyOn(Step2Service, 'open').and.returnValue($q.resolve(docType));
    CreateContentService.componentInfo = {
      id: '1234',
      label: 'Banner',
      parameterName: 'document',
      parameterBasePath: '/content/documents/channel',
    };
    CreateContentService.next({}, 'url', 'locale');
    $rootScope.$digest();

    expect($translate.instant).toHaveBeenCalledWith('CREATE_NEW_DOCUMENT_TYPE', { documentType: 'document-type-name' });
    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('CREATE_NEW_DOCUMENT_TYPE');
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(Step2Service.open).toHaveBeenCalledWith({}, 'url', 'locale', CreateContentService.componentInfo);
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
  });

  it('cancels creating a new document', () => {
    spyOn($state, 'go');
    CreateContentService.stop();
    expect($state.go).toHaveBeenCalledWith('^');
  });

  describe('validate config data for transition to step1', () => {
    it('should have a templateQuery configuration option', () => {
      spyOn(FeedbackService, 'showError');
      CreateContentService.start();
      expect(FeedbackService.showError).toHaveBeenCalledWith('Failed to open create-content-step1 sidepanel due to missing configuration option "templateQuery"');
    });
  });

  describe('finish', () => {
    it('reloads the iframe', () => {
      spyOn(HippoIframeService, 'reload');
      CreateContentService.finish('document-id');
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('switches to edit-content', () => {
      spyOn(EditContentService, 'startEditing');
      CreateContentService.finish('document-id');
      expect(EditContentService.startEditing).toHaveBeenCalledWith('document-id');
    });
  });

  it('generates a document url by executing a "slugs" backend call', () => {
    CreateContentService.generateDocumentUrlByName('name', 'nl');
    expect(ContentService._send).toHaveBeenCalledWith('POST', ['slugs'], 'name', true, { locale: 'nl' });
  });

  describe('other editor opened', () => {
    beforeEach(() => {
      const document = {
        id: 42,
      };
      const docType = { displayName: 'document-type-name' };
      spyOn(Step2Service, 'open').and.returnValue($q.resolve(docType));

      CreateContentService.next(document, 'url', 'en');
      $rootScope.$digest();

      spyOn(Step2Service, 'killEditor');
      spyOn($state, 'go').and.callThrough();
    });

    it('stops step 2 for the same document', () => {
      Step2Service.killEditor.and.returnValue(true);

      $window.CMS_TO_APP.publish('kill-editor', '42');

      expect(Step2Service.killEditor).toHaveBeenCalledWith('42');
      expect($state.go).toHaveBeenCalledWith('^');
    });

    it('does not stop step 2 for another document', () => {
      Step2Service.killEditor.and.returnValue(false);

      $window.CMS_TO_APP.publish('kill-editor', '1');

      expect(Step2Service.killEditor).toHaveBeenCalledWith('1');
      expect($state.go).not.toHaveBeenCalled();
    });

    it('only stops step 2 when it is active', () => {
      CreateContentService.stop();
      $rootScope.$digest();

      $window.CMS_TO_APP.publish('kill-editor', '42');

      expect(Step2Service.killEditor).not.toHaveBeenCalled();
    });
  });
});
