<%--
  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="document" type="org.onehippo.forge.polldemo.beans.TextDocument"--%>
<%--@elvariable id="headTitle" type="java.lang.String"--%>

<c:choose>
  <c:when test="${empty document}">
    <tag:pagenotfound/>
  </c:when>
  <c:otherwise>
    <c:if test="${not empty document.title}">
      <hst:element var="headTitle" name="title">
        <c:out value="${document.title}"/>
      </hst:element>
      <hst:headContribution keyHint="headTitle" element="${headTitle}"/>
    </c:if>
    
    <hst:cmseditlink hippobean="${document}"/>
    <h2>${document.title}</h2>
    <p>${document.summary}</p>
    <hst:html hippohtml="${document.html}"/>
  </c:otherwise>
</c:choose>