import { Observable } from 'rxjs/Observable';
import { TemplateQuery } from './create-content.types';

export class CreateContentServiceMock {
  getTemplateQuery(id): Observable<TemplateQuery> {
    return Observable.of(null);
  }

  generateDocumentUrlByName(name, locale: string = ''): Observable<string> {
    return Observable.of(name.replace(/\s+/g, '-').toLowerCase()); // will transform "TestName123" into "test-name-123"
  }
}

export class FeedbackServiceMock {
  showError(key: string, params: Map<string, any>): void {}
}
