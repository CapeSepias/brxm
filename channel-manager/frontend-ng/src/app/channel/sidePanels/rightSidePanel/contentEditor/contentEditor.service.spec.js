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

describe('ContentEditorService', () => {
  let $q;
  let $rootScope;
  let $translate;
  let CmsService;
  let ContentEditor;
  let ContentService;
  let DialogService;
  let FeedbackService;
  let FieldService;

  const stringField = {
    id: 'ns:string',
    type: 'STRING',
  };
  const multipleStringField = {
    id: 'ns:multiplestring',
    type: 'STRING',
    multiple: true,
  };
  const emptyMultipleStringField = {
    id: 'ns:emptymultiplestring',
    type: 'STRING',
    multiple: true,
  };
  const testDocumentType = {
    id: 'ns:testdocument',
    fields: [
      stringField,
      multipleStringField,
      emptyMultipleStringField,
    ],
  };
  const testDocument = {
    id: 'test',
    info: {
      type: {
        id: 'ns:testdocument',
      },
    },
    fields: {
      'ns:string': [
        {
          value: 'String value',
        },
      ],
      'ns:multiplestring': [
        {
          value: 'One',
        },
        {
          value: 'Two',
        },
      ],
      'ns:emptymultiplestring': [],
    },
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ContentService = jasmine.createSpyObj('ContentService', ['createDraft', 'getDocumentType', 'saveDraft', 'deleteDraft', 'deleteDocument']);
    FeedbackService = jasmine.createSpyObj('FeedbackService', ['showError']);
    FieldService = jasmine.createSpyObj('FieldService', ['setDocumentId']);

    angular.mock.module(($provide) => {
      $provide.value('ContentService', ContentService);
      $provide.value('FeedbackService', FeedbackService);
      $provide.value('FieldService', FieldService);
    });

    inject((_$q_, _$rootScope_, _$translate_, _CmsService_, _ContentEditor_, _DialogService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $translate = _$translate_;
      CmsService = _CmsService_;
      ContentEditor = _ContentEditor_;
      DialogService = _DialogService_;
    });

    spyOn(CmsService, 'closeDocumentWhenValid');
    spyOn(CmsService, 'reportUsageStatistic');

    spyOn(DialogService, 'show');
  });

  describe('opens a document', () => {
    beforeEach(() => {
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.resolve(testDocument));
      ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
    });

    function expectDocumentLoaded() {
      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
      expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');

      expect(ContentEditor.getDocument()).toEqual(testDocument);
      expect(ContentEditor.getDocumentType()).toEqual(testDocumentType);
      expect(ContentEditor.isDocumentDirty()).toBeFalsy();
      expect(ContentEditor.isEditing()).toBe(true);
      expect(ContentEditor.getError()).toBeUndefined();
    }

    it('and does not report unsupported fields when there are none', () => {
      ContentEditor.open('test');
      $rootScope.$digest();

      expectDocumentLoaded();
      expect(CmsService.reportUsageStatistic).not.toHaveBeenCalled();
    });

    it('reports all field types in a document that are not yet supported by the content editor', () => {
      testDocumentType.unsupportedFieldTypes = ['Date', 'selection:selection'];

      ContentEditor.open('test');
      $rootScope.$digest();

      expectDocumentLoaded();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingUnsupportedFields', {
        unsupportedFieldTypes: 'Date,selection:selection',
      });
    });

    describe('and sets an error when it', () => {
      function expectError(error) {
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getDocumentType()).toBeUndefined();
        expect(ContentEditor.isDocumentDirty()).toBeFalsy();
        expect(ContentEditor.isEditing()).toBe(false);
        expect(ContentEditor.getError()).toEqual(error);
      }

      function expectDefaultError() {
        expectError({
          titleKey: 'FEEDBACK_DEFAULT_TITLE',
          messageKey: 'FEEDBACK_DEFAULT_MESSAGE',
          linkToContentEditor: true,
        });
      }

      it('opens a document without content', () => {
        const emptyDocument = {
          id: 'test',
          displayName: 'Display Name',
          info: {
            type: { id: 'ns:testdocument' },
          },
          fields: {},
        };
        ContentService.createDraft.and.returnValue($q.resolve(emptyDocument));

        ContentEditor.open(emptyDocument.id);
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expectError({
          titleKey: 'FEEDBACK_NOT_EDITABLE_HERE_TITLE',
          messageKey: 'FEEDBACK_NO_EDITABLE_CONTENT_MESSAGE',
          messageParams: {
            displayName: 'Display Name',
          },
          linkToContentEditor: true,
        });
      });

      it('opens a document with pending invalid changes in the draft', () => {
        CmsService.closeDocumentWhenValid.and.returnValue($q.reject());

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_DRAFT_INVALID_TITLE',
          messageKey: 'FEEDBACK_DRAFT_INVALID_MESSAGE',
          linkToContentEditor: true,
        });
      });

      it('opens a document owned by another user', () => {
        const response = {
          reason: 'OTHER_HOLDER',
          params: {
            displayName: 'Display Name',
            userId: 'jtester',
            userName: 'John Tester',
          },
        };
        ContentService.createDraft.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
          messageKey: 'FEEDBACK_HELD_BY_OTHER_USER_MESSAGE',
          messageParams: {
            displayName: 'Display Name',
            user: 'John Tester',
          },
        });
      });

      it('opens a document owned by another user and falls back to the user\'s id if there is no display name', () => {
        const response = {
          reason: 'OTHER_HOLDER',
          params: {
            userId: 'tester',
          },
        };
        ContentService.createDraft.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
          messageKey: 'FEEDBACK_HELD_BY_OTHER_USER_MESSAGE',
          messageParams: {
            user: 'tester',
          },
        });
      });

      it('opens a document with a publication request', () => {
        const response = {
          reason: 'REQUEST_PENDING',
          params: {
            displayName: 'Display Name',
          },
        };
        ContentService.createDraft.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
          messageKey: 'FEEDBACK_REQUEST_PENDING_MESSAGE',
          messageParams: {
            displayName: 'Display Name',
          },
        });
      });

      it('opens a document which is not a document', () => {
        const response = {
          reason: 'NOT_A_DOCUMENT',
        };
        ContentService.createDraft.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_NOT_A_DOCUMENT_TITLE',
          messageKey: 'FEEDBACK_NOT_A_DOCUMENT_MESSAGE',
          linkToContentEditor: true,
        });
      });

      it('opens a non-existing document', () => {
        ContentService.createDraft.and.returnValue($q.reject({ status: 404 }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expectError({
          titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
          messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
          disableContentButtons: true,
        });
      });

      it('opens a document with random data in the response', () => {
        const response = { bla: 'test' };
        ContentService.createDraft.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expectDefaultError();
      });

      it('opens a document with no data in the response', () => {
        ContentService.createDraft.and.returnValue($q.reject({}));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expectDefaultError();
      });

      it('opens a document with an unknown error reason', () => {
        const response = {
          reason: 'unknown',
        };
        ContentService.createDraft.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expectError(undefined);
      });

      it('opens a document without a type', () => {
        const doc = {
          info: {
            type: {
              id: 'document:type',
            },
          },
          fields: {
            bla: 1,
          },
          displayName: 'Document Display Name',
        };
        ContentService.createDraft.and.returnValue($q.resolve(doc));
        ContentService.getDocumentType.and.returnValue($q.reject({}));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.createDraft).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).toHaveBeenCalledWith('document:type');
        expectDefaultError();
      });
    });
  });

  it('marks a document dirty', () => {
    expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    ContentEditor.markDocumentDirty();
    expect(ContentEditor.isDocumentDirty()).toBe(true);
  });

  describe('save', () => {
    it('happens with a dirty document', () => {
      const savedDoc = {
        id: '123',
      };
      ContentService.saveDraft.and.returnValue($q.resolve(savedDoc));

      ContentEditor.document = testDocument;
      ContentEditor.markDocumentDirty();
      ContentEditor.save();

      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

      $rootScope.$digest();

      expect(ContentEditor.getDocument()).toEqual(savedDoc);
      expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    });

    it('does not happen with a pristine document', () => {
      ContentEditor.document = testDocument;

      ContentEditor.save();
      $rootScope.$digest();

      expect(ContentService.saveDraft).not.toHaveBeenCalled();
    });

    describe('shows error feedback when it', () => {
      it('fails', () => {
        const response = {
          reason: 'TEST',
        };
        ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

        ContentEditor.document = testDocument;
        ContentEditor.markDocumentDirty();
        ContentEditor.save();

        expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_TEST');
      });

      it('fails because another user is now the holder', () => {
        const response = {
          reason: 'OTHER_HOLDER',
          params: {
            userId: 'tester',
          },
        };
        ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

        ContentEditor.document = testDocument;
        ContentEditor.markDocumentDirty();
        ContentEditor.save();

        expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_OTHER_HOLDER', { user: 'tester' });
      });

      it('fails because another *named* user is now the holder', () => {
        const response = {
          reason: 'OTHER_HOLDER',
          params: {
            userId: 'tester',
            userName: 'Joe Tester',
          },
        };
        ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

        ContentEditor.document = testDocument;
        ContentEditor.markDocumentDirty();
        ContentEditor.save();

        expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_OTHER_HOLDER', { user: 'Joe Tester' });
      });

      describe('fails because of an invalid field', () => {
        beforeEach(() => {
          const saveResponse = angular.copy(testDocument);
          saveResponse.fields['ns:string'] = [
            {
              value: '',
              errorInfo: {
                code: 'REQUIRED_FIELD_EMPTY',
              },
            },
          ];

          ContentService.saveDraft.and.returnValue($q.reject({ data: saveResponse }));

          ContentEditor.document = testDocument;
          ContentEditor.documentType = testDocumentType;
          ContentEditor.markDocumentDirty();
        });

        it('reloads the document type', () => {
          const reloadedDocumentType = angular.copy(testDocumentType);
          ContentService.getDocumentType.and.returnValue($q.resolve(reloadedDocumentType));

          ContentEditor.save();

          expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

          $rootScope.$digest();

          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_INVALID_DATA');
          expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
          expect(ContentEditor.getDocumentType()).toBe(reloadedDocumentType);
        });

        it('shows an error when reloading the document type fails', () => {
          ContentService.getDocumentType.and.returnValue($q.reject({ status: 404 }));

          ContentEditor.save();

          expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

          $rootScope.$digest();

          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_INVALID_DATA');
          expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
          expect(ContentEditor.getDocumentType()).toBe(testDocumentType);
          expect(ContentEditor.getError()).toEqual({
            titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
            messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
            disableContentButtons: true,
          });
        });
      });

      it('fails because there is no data returned', () => {
        ContentService.saveDraft.and.returnValue($q.reject({}));

        ContentEditor.document = testDocument;
        ContentEditor.markDocumentDirty();
        ContentEditor.save();

        expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UNABLE_TO_SAVE');
      });
    });
  });

  describe('confirm discard changes', () => {
    const showPromise = {};

    beforeEach(() => {
      ContentEditor.document = {
        displayName: 'Test',
      };
      spyOn($translate, 'instant');
      spyOn(DialogService, 'confirm').and.callThrough();
      DialogService.show.and.returnValue(showPromise);
    });

    it('shows a dialog', () => {
      ContentEditor.markDocumentDirty();

      const result = ContentEditor.confirmDiscardChanges('MESSAGE_KEY');

      expect(DialogService.confirm).toHaveBeenCalled();
      expect($translate.instant).toHaveBeenCalledWith('MESSAGE_KEY', {
        documentName: 'Test',
      });
      expect(DialogService.show).toHaveBeenCalled();
      expect(result).toBe(showPromise);
    });

    it('shows a dialog with a title', () => {
      ContentEditor.markDocumentDirty();

      const result = ContentEditor.confirmDiscardChanges('MESSAGE_KEY', 'TITLE_KEY');

      expect(DialogService.confirm).toHaveBeenCalled();
      expect($translate.instant).toHaveBeenCalledWith('MESSAGE_KEY', {
        documentName: 'Test',
      });
      expect($translate.instant).toHaveBeenCalledWith('TITLE_KEY', {
        documentName: 'Test',
      });
      expect(DialogService.show).toHaveBeenCalled();
      expect(result).toBe(showPromise);
    });

    it('does not show a dialog when the document has not changed', (done) => {
      ContentEditor.confirmDiscardChanges().then(() => {
        expect(DialogService.show).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not show a dialog when the editor is killed', (done) => {
      ContentEditor.markDocumentDirty();
      ContentEditor.kill();
      ContentEditor.confirmDiscardChanges().then(() => {
        expect(DialogService.show).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });
  });

  describe('confirm save or discard changes', () => {
    beforeEach(() => {
      testDocument.displayName = 'Test';
      ContentEditor.document = testDocument;
      spyOn($translate, 'instant');
    });

    it('shows a dialog and saves changes', (done) => {
      ContentEditor.markDocumentDirty();
      DialogService.show.and.returnValue($q.resolve('SAVE'));
      ContentService.saveDraft.and.returnValue($q.resolve(testDocument));

      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').then((action) => {
        expect(action).toBe('SAVE');
        expect($translate.instant).toHaveBeenCalledWith('TEST_MESSAGE_KEY', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('SAVE_CHANGES_TITLE');
        expect(DialogService.show).toHaveBeenCalled();
        expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
        done();
      });
      $rootScope.$digest();
    });

    it('shows a dialog and discards changes', (done) => {
      ContentEditor.markDocumentDirty();
      DialogService.show.and.returnValue($q.resolve('DISCARD'));

      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').then((action) => {
        expect(action).toBe('DISCARD');
        expect($translate.instant).toHaveBeenCalledWith('TEST_MESSAGE_KEY', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('SAVE_CHANGES_TITLE');
        expect(DialogService.show).toHaveBeenCalled();
        expect(ContentService.saveDraft).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('shows a dialog and does nothing', (done) => {
      ContentEditor.markDocumentDirty();
      DialogService.show.and.returnValue($q.reject());

      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').catch(() => {
        expect($translate.instant).toHaveBeenCalledWith('TEST_MESSAGE_KEY', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('SAVE_CHANGES_TITLE');
        expect(DialogService.show).toHaveBeenCalled();
        expect(ContentService.saveDraft).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not show a dialog when the document has not changed', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').then(() => {
        expect(DialogService.show).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not show a dialog when the editor is killed', (done) => {
      ContentEditor.markDocumentDirty();
      ContentEditor.kill();
      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').then(() => {
        expect(DialogService.show).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });
  });

  describe('delete draft', () => {
    it('happens when a document is edited and the editor is not killed', () => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentService.deleteDraft.and.returnValue($q.resolve());

      ContentEditor.deleteDraft();
      $rootScope.$digest();

      expect(ContentService.deleteDraft).toHaveBeenCalledWith(testDocument.id);
    });

    it('does not happens when no document is being edited', (done) => {
      ContentEditor.deleteDraft().then(() => {
        expect(ContentService.deleteDraft).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not happens when the editor is killed', (done) => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;
      ContentEditor.kill();

      ContentEditor.deleteDraft().then(() => {
        expect(ContentService.deleteDraft).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });
  });

  describe('delete document', () => {
    it('happens when a document is edited and the editor is not killed', () => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentService.deleteDocument.and.returnValue($q.resolve());

      ContentEditor.deleteDocument();
      $rootScope.$digest();

      expect(ContentService.deleteDocument).toHaveBeenCalledWith(testDocument.id);
    });

    it('does not happens when no document is being edited', (done) => {
      ContentEditor.deleteDocument().then(() => {
        expect(ContentService.deleteDocument).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not happens when the editor is killed', (done) => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;
      ContentEditor.kill();

      ContentEditor.deleteDocument().then(() => {
        expect(ContentService.deleteDocument).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('shows an error when deleting the document fails', () => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentService.deleteDocument.and.returnValue($q.reject({
        data: {
          reason: 'NOT_ALLOWED',
          params: {
            foo: 1,
          },
        },
      }));

      ContentEditor.deleteDocument();
      $rootScope.$digest();

      expect(ContentService.deleteDocument).toHaveBeenCalledWith(testDocument.id);
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_NOT_ALLOWED', { foo: 1 });
    });
  });

  function expectClear() {
    expect(ContentEditor.getDocument()).toBeUndefined();
    expect(ContentEditor.getDocumentId()).toBeUndefined();
    expect(ContentEditor.getDocumentType()).toBeUndefined();
    expect(ContentEditor.getError()).toBeUndefined();
    expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    expect(ContentEditor.isEditing()).toBeFalsy();
  }

  it('is cleared initially', () => {
    expectClear();
  });

  describe('close', () => {
    it('clears an opened document', () => {
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.resolve(testDocument));
      ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
      ContentEditor.open('test');

      ContentEditor.close();

      expectClear();
    });

    it('clears an error', () => {
      ContentEditor.error = {
        titleKey: 'FEEDBACK_DEFAULT_TITLE',
      };

      ContentEditor.close();

      expectClear();
    });

    it('resets the kill state', () => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentEditor.kill();

      ContentEditor.deleteDraft();
      $rootScope.$digest();

      expect(ContentService.deleteDraft).not.toHaveBeenCalled();

      ContentEditor.close();

      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentEditor.deleteDraft();
      $rootScope.$digest();

      expect(ContentService.deleteDraft).toHaveBeenCalled();
    });
  });
});
