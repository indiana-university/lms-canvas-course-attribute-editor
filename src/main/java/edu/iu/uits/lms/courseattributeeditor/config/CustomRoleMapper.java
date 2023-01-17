package edu.iu.uits.lms.courseattributeeditor.config;

import edu.iu.uits.lms.iuonly.model.DeptProvisioningUser;
import edu.iu.uits.lms.iuonly.services.DeptProvisioningUserServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class CustomRoleMapper extends LmsDefaultGrantedAuthoritiesMapper {
    @Autowired
    private DeptProvisioningUserServiceImpl deptProvisioningUserService;

    public CustomRoleMapper(DefaultInstructorRoleRepository defaultInstructorRoleRepository, DeptProvisioningUserServiceImpl deptProvisioningUserService) {
        super(defaultInstructorRoleRepository);
        this.deptProvisioningUserService = deptProvisioningUserService;
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        List<GrantedAuthority> remappedAuthorities = new ArrayList<>();
        remappedAuthorities.addAll(authorities);
        for (GrantedAuthority authority : authorities) {
            OidcUserAuthority userAuth = (OidcUserAuthority) authority;
            OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(userAuth.getAttributes());
            log.debug("LTI Claims: {}", userAuth.getAttributes());

            String userId = oidcTokenUtils.getUserLoginId();

            String rolesString = "NotAuthorized";

            ArrayList<String> roles = new ArrayList<>();

            DeptProvisioningUser user = deptProvisioningUserService.findByUsername(userId);

            if (user != null) {
                rolesString = LTIConstants.CANVAS_INSTRUCTOR_ROLE;
            }

            roles.add(rolesString);

            // add the current roles to the string check. If it's one of our default roles, then it's cool
            roles.addAll(List.of(oidcTokenUtils.getRoles()));

            String userRoles[] = roles.toArray(new String[roles.size()]);

            String newAuthString = returnEquivalentAuthority(userRoles, getDefaultInstructorRoles());

            OidcUserAuthority newUserAuth = new OidcUserAuthority(newAuthString, userAuth.getIdToken(), userAuth.getUserInfo());
            remappedAuthorities.add(newUserAuth);
        }

        return remappedAuthorities;
    }
}
