<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%--
    Copyright 2007 Hippo
    
    Licensed under the Apache License, Version 2.0 (the  "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS"
    BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<c:set var="webpage" value="${global['site/pages']['news/news']}" scope="request"/>
<html xmlns="http://www.w3.org/1999/xhtml"><head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <link href="style.css" rel="stylesheet" type="text/css"/>
</head><body>

  <jsp:include page="navigation.jsp"/>

  <h1>${webpage['demo:pageTitle']}</h1>

  <p>
    <% int i=0; %>
    <c:forEach var="iterator" items="${global['site/messages']}">
      <h:access var="message" value="${iterator[iterator._name]}">
        <a href="message.jsp?message=${message._name}"><fmt:formatDate pattern="dd-MM-yy" value="${message['demo:date']}"/>&nbsp;${message['demo:title']}</a>
          <% if(i++<3) { %>
            <br/>
              <h:access var="image" value="${message['demo:image']}">
                <img src="/images${image._path}" align="left"/>
              </h:access>
              ${message['demo:description']}
          <% } %>
        <br clear="all"/><br/>
      </h:access>
    </c:forEach>
  </p>

</body></html>
