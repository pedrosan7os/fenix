package ServidorApresentacao.publico;

import junit.framework.Test;
import junit.framework.TestSuite;
import ServidorApresentacao.TestCasePresentation;

/**
 * @author tfc130
 *
 */
public class PrepareConsultRoomsFormActionTest extends TestCasePresentation {

  public static void main(java.lang.String[] args) {
    junit.textui.TestRunner.run(suite());
  }
    
  public static Test suite() {
    TestSuite suite = new TestSuite(PrepareConsultRoomsFormActionTest.class);
        
    return suite;
  }

  public void setUp() {
    super.setUp();
    // define ficheiro de configura��o Struts a utilizar
    setServletConfigFile("/WEB-INF/web.xml");
  }
  
  
  
  public PrepareConsultRoomsFormActionTest(String testName) {
    super(testName);
  }

  public void testSuccessfulPrepareConsultRooms() {      
    // define mapping de origem
    setRequestPathInfo("publico", "/prepareConsultRooms");

	// colocar qq coisa na sess�o.
	// O simulator tem que l� ter qq coisa para funcionar.
	getSession();

    // invoca ac��o
    actionPerform();

    // verifica reencaminhamento
    verifyForward("Sucess");

    //verifica ausencia de erros
    verifyNoActionErrors();
  }

}