<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<tiles:insert page="/fenixLayout_2col.jsp" flush="true">
  <tiles:put name="title" value=".IST - SOP" />
  <tiles:put name="serviceName" value="Servi�o de Organiza��o Pedag�gica" />
  <tiles:put name="navGeral" value="/sop/commonNavGeralSopRoom.jsp" />
  <tiles:put name="navLocal" value="/sop/commonNavLocalSalasSop.jsp" />
  <tiles:put name="body-context" value=""/>  
  <tiles:put name="body" value="/sop/verSala_bd.jsp" />
  <tiles:put name="footer" value="/sop/commonFooterSop.jsp" />
</tiles:insert>