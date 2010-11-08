<%--
  Copyright 2008-2009 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>
<%@ page language="java" %>
<%@ page import="org.hippoecm.hst.security.servlet.LoginServlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<%
String username = (String) session.getAttribute("org.hippoecm.hst.security.servlet.username");
if (username == null) username = "";
String password = (String) session.getAttribute("org.hippoecm.hst.security.servlet.password");
if (password == null) password = "";
String destination = request.getParameter("destination");
if (destination == null) destination = "";
%>

<html>
  <head>
    <title>Hippo CMS 7</title>
    <link rel="stylesheet" type="text/css" href="../skin/screen.css" />
  </head>
  <body class="hippo-root">
    <div>
      <div class="hippo-login-panel">
        <form class="hippo-login-panel-form" name="signInForm" method="post" action="<c:url value="/login/proxy"/>">
          <h2><div class="hippo-global-hideme"><span>Hippo CMS 7</span></div></h2>
          <div class="hippo-login-form-container">
            <table>
              <tr>
                <td>
                  <p>Authentication failed with user, <%=username%>.</p>
                </td>
              </tr>
              <tr>
                <td>
                  <p><a href="login.jsp">Try again.</a></p>
                </td>
              </tr>
            </table>
          </div>
        </form>
        <div class="hippo-login-panel-copyright">
          &copy; 1999-2010 Hippo B.V.
        </div>
      </div>
    </div>
  </body>
</html>