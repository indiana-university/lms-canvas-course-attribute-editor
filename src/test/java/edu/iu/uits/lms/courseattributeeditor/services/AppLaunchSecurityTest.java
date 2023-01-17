package edu.iu.uits.lms.courseattributeeditor.services;

import edu.iu.uits.lms.canvas.config.CanvasClientTestConfig;
import edu.iu.uits.lms.courseattributeeditor.repository.CourseAttributeAuditLogRepository;
import edu.iu.uits.lms.iuonly.jarexport.IuClientTestConfig;
import edu.iu.uits.lms.iuonly.model.DeptProvisioningUser;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.LtiClientTestConfig;
import edu.iu.uits.lms.courseattributeeditor.config.ToolConfig;
import edu.iu.uits.lms.courseattributeeditor.controller.CourseAttributeEditorToolController;
import edu.iu.uits.lms.lti.config.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CourseAttributeEditorToolController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@Import({ToolConfig.class, CanvasClientTestConfig.class, LtiClientTestConfig.class, IuClientTestConfig.class})
public class AppLaunchSecurityTest {

   @Autowired
   private MockMvc mvc;

//   @MockBean
//   private SudsApi sudsApi;

   @MockBean
   private DeptProvisioningUser deptProvisioningUser;

   @MockBean
   private CourseAttributeAuditLogRepository courseAttributeAuditLogRepository;

   @Test
   public void appNoAuthnLaunch() throws Exception {
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/app/index")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void appAuthnLaunch() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId", "1234", LTIConstants.INSTRUCTOR_AUTHORITY);

      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/app/index")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
   }

   @Test
   public void randomUrlNoAuth() throws Exception {
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void randomUrlWithAuth() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId", "1234", LTIConstants.BASE_USER_AUTHORITY);
      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
   }
}
