package edu.iu.uits.lms.courseattributeeditor.controller;

import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.LtiController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import io.jsonwebtoken.Claims;
import iuonly.client.generated.api.DeptProvisioningUserApi;
import iuonly.client.generated.model.DeptProvisioningUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tsugi.basiclti.BasicLTIConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static canvas.helpers.CanvasConstants.ADMIN_ROLE;

@Controller
@RequestMapping({"/lti"})
@Slf4j
public class CourseAttributeEditorLtiController extends LtiController {

    @Autowired
    @Qualifier("deptProvisioningUserApiViaAnonymous")
    private DeptProvisioningUserApi deptProvisioningUserApi;

    private boolean openLaunchUrlInNewWindow = false;

    @Override
    protected String getLaunchUrl(Map<String, String> launchParams) {
        return "/app/index/";
    }

    @Override
    protected Map<String, String> getParametersForLaunch(Map<String, String> payload, Claims claims) {
        Map<String, String> paramMap = new HashMap<String, String>(1);

        paramMap.put(CUSTOM_CANVAS_COURSE_ID, payload.get(CUSTOM_CANVAS_COURSE_ID));
        paramMap.put(BasicLTIConstants.ROLES, payload.get(BasicLTIConstants.ROLES));
        paramMap.put(CUSTOM_CANVAS_USER_LOGIN_ID, payload.get(CUSTOM_CANVAS_USER_LOGIN_ID));
        paramMap.put(BasicLTIConstants.CONTEXT_TITLE, payload.get(BasicLTIConstants.CONTEXT_TITLE));
        paramMap.put(BasicLTIConstants.LIS_PERSON_CONTACT_EMAIL_PRIMARY, payload.get(BasicLTIConstants.LIS_PERSON_CONTACT_EMAIL_PRIMARY));
        paramMap.put(BasicLTIConstants.LIS_PERSON_SOURCEDID, payload.get(BasicLTIConstants.LIS_PERSON_SOURCEDID));

        openLaunchUrlInNewWindow = Boolean.valueOf(payload.get(CUSTOM_OPEN_IN_NEW_WINDOW));

        return paramMap;
    }

    @Override
    protected void preLaunchSetup(Map<String, String> launchParams, HttpServletRequest request, HttpServletResponse response) {
        String userId = launchParams.get(CUSTOM_CANVAS_USER_LOGIN_ID);

        String rolesString = "NotAuthorized";

        DeptProvisioningUser user = deptProvisioningUserApi.findByUsername(userId);

        if (user != null) {
            rolesString = "Instructor";
            request.getSession().setAttribute("groups", user.getGroupCode());
        }

        // add the current role to the string check. if it's one of our default roles, then it's cool
        rolesString += "," + launchParams.get(BasicLTIConstants.ROLES);

        String[] userRoles = rolesString.split(",");
        String authority = returnEquivalentAuthority(Arrays.asList(userRoles), getDefaultInstructorRoles());
        log.debug("LTI equivalent authority: " + authority);

        String systemId = launchParams.get(BasicLTIConstants.TOOL_CONSUMER_INSTANCE_GUID);
        String courseId = launchParams.get(CUSTOM_CANVAS_COURSE_ID);

        LtiAuthenticationToken token = new LtiAuthenticationToken(userId, courseId, systemId,
                AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, authority), getToolContext());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Override
    protected String getToolContext() {
        return "lms_lti_course_attribute_editor";
    }

    @Override
    protected LAUNCH_MODE launchMode() {
        if (openLaunchUrlInNewWindow)
            return LAUNCH_MODE.WINDOW;

        return LAUNCH_MODE.FORWARD;
    }
}
