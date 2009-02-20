<%@ page language="java" %>
<%@ taglib uri='/WEB-INF/hst-core.tld' prefix='hc'%>

<%
System.out.println("Console out from general_layout.jsp");
%>

<html>
<head>

<!-- include header -->
<hc:content path="h" />

</head>
<body>

<script language="javascript">
<!--
function <hc:namespace/>showPopup() {
    alert("Hello from general_layout  component!");
}
//-->
</script>

<span class="title">The new Hst is Coooooooooooool</span>

<div class="page">
    <div>
    
    <a href="javascript:<hc:namespace/>showPopup();">Show</a>
    
    <hc:url var="firstUrl" type="render">
      <hc:param name="page" value="1" />
    </hc:url>
    <hc:url var="lastUrl" type="render">
      <hc:param name="page" value="9" />
    </hc:url>
    
    <a href="<%=firstUrl%>">First</a>
    <a href="<%=lastUrl%>">Last</a>
    
    <div>
        header parameters: <%=request.getParameterMap()%>
    </div>
    
    </div>
    
    <hc:content path="b" />
    
</div>

</body>
</html>