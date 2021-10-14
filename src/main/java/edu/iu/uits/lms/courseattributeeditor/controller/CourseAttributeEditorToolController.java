package edu.iu.uits.lms.courseattributeeditor.controller;

import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.api.SectionsApi;
import canvas.client.generated.model.Course;
import canvas.client.generated.model.CourseSectionUpdateWrapper;
import canvas.client.generated.model.Section;
import edu.iu.uits.lms.courseattributeeditor.config.ToolConfig;
import edu.iu.uits.lms.courseattributeeditor.model.CourseAttributeAuditLog;
import edu.iu.uits.lms.courseattributeeditor.repository.CourseAttributeAuditLogRepository;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import iuonly.client.generated.api.SudsApi;
import iuonly.client.generated.model.SudsCourse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/app")
@Slf4j
public class CourseAttributeEditorToolController extends LtiAuthenticationTokenAwareController {

   @Autowired
   private ToolConfig toolConfig;

   @Autowired
   private CoursesApi coursesApi;

   @Autowired
   private SectionsApi sectionsApi;

   @Autowired
   private SudsApi sudsApi;

   @Autowired
   private CourseAttributeAuditLogRepository courseAttributeAuditLogRepository;

   @RequestMapping("/index")
   @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
   public ModelAndView index(Model model, HttpServletRequest request) {
      log.debug("in /index");
      getTokenWithoutContext();

      model.addAttribute("breadcrumb", false);

      return new ModelAndView("index");
   }

   @RequestMapping("/find")
   @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
   public ModelAndView find(Model model, HttpServletRequest request, @RequestParam("searchBox") String searchBox) {
      log.debug("in /find");
      getTokenWithoutContext();

      if (searchBox == null || searchBox.isEmpty()) {
         model.addAttribute("blankSearch", true);
         return new ModelAndView("index");
      }

      model.addAttribute("breadcrumb", true);
      model.addAttribute("tier2breadcrumb", true);
      model.addAttribute("searchBox", searchBox);

      // code to lookup search criteria
      Course course = coursesApi.getCourse(searchBox);

      if (course != null) {
         // set this regardless of rest of the logic
         model.addAttribute("courseFound", true);
         model.addAttribute("pageTitle", "Canvas Course ID: " + searchBox);
         model.addAttribute("course", course);
         // boo-urns that this is a separate call and not included in coursesApi.getCourse(searchBox);
         List<Section> sections = coursesApi.getCourseSections(searchBox);
         model.addAttribute("sections", sections);

         if (course.getSisCourseId() != null) {
            // check Suds to see if this is a legit course
            SudsCourse sudsCourse = sudsApi.getSudsCourseBySiteId(course.getSisCourseId());
            if (sudsCourse != null) {
               // we have a match, so this course is not editable
               model.addAttribute("editable", false);
            } else {
               // check archive table
               SudsCourse sudsCourseArchive = sudsApi.getSudsArchiveCourseBySiteId(course.getSisCourseId());
               model.addAttribute("editable", sudsCourseArchive == null);
            }
         } else {
            // this does not have a sis course id, therefore it is editable
            model.addAttribute("editable", true);
         }
      } else {
         model.addAttribute("pageTitle", "Canvas Course not found");
         model.addAttribute("displayNotFound", true);
      }

      return new ModelAndView("index");
   }

   /**
    * this is the direct link back to the find page with the search parameter
    * @param model
    * @param request
    * @param searchBox
    * @return
    */
   @RequestMapping("/find/{searchBox}")
   @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
   public ModelAndView returnTofind(Model model, HttpServletRequest request, @PathVariable("searchBox") String searchBox) {
      return find(model, request, searchBox);
   }

   @RequestMapping("/edit/{editId}")
   @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
   public ModelAndView edit(@PathVariable("editId") String editId, Model model, HttpServletRequest request) {
      log.debug("in /edit");
      getTokenWithoutContext();

      // code to lookup search criteria
      Course course = coursesApi.getCourse(editId);

      // check Suds to see if this is a legit course / keep users from cheating into this call
      boolean editable = true;
      SudsCourse sudsCourse = sudsApi.getSudsCourseBySiteId(course.getSisCourseId());
      if (sudsCourse != null) {
         // we have a match, so this course is not editable
         editable = false;
      } else {
         // check archive table
         SudsCourse sudsCourseArchive = sudsApi.getSudsArchiveCourseBySiteId(course.getSisCourseId());
         editable = (sudsCourseArchive == null);
      }

      if (!editable) {
         // user shouldn't be on this page so send them elsewhere
         return find(model, request, editId);
      }

      model.addAttribute("breadcrumb", true);
      model.addAttribute("tier3breadcrumb", true);
      model.addAttribute("searchBox", editId);
      model.addAttribute("pageTitle", "Edit Course");

      // boo-urns that this is a separate call and not included in coursesApi.getCourse(searchBox);
      List<Section> listOfSections = coursesApi.getCourseSections(editId);

      // all this is to determine if the section is an official SIS provisioned course
      List<SectionWithSISCheck> sections = new ArrayList<>();

      for (Section section : listOfSections) {
         SectionWithSISCheck sectionWithSISCheck = new SectionWithSISCheck();
         SudsCourse sudsSection = sudsApi.getSudsCourseBySiteId(section.getSisSectionId());
         boolean isSisProvisioned = false;
         if (sudsSection != null) {
            // we have a match, so this section is not editable
            isSisProvisioned = true;
         } else {
            SudsCourse sudsSectionArchive = sudsApi.getSudsArchiveCourseBySiteId(section.getSisSectionId());
            if (sudsSectionArchive != null) {
               // we have a match, so this section is not editable
               isSisProvisioned = true;
            }
         }

         // check for overrides since there will be errors to include
         List<SectionWithSISCheck> sectionOverrides = (List<SectionWithSISCheck>) model.getAttribute("sectionOverride");

         if (sectionOverrides != null) {
            for (SectionWithSISCheck swsc : sectionOverrides) {
               if (swsc.getSection().getId().equals(section.getId())) {
                  section.setSisSectionId(swsc.getSection().getSisSectionId());
                  section.setName(swsc.getSection().getName());

                  sectionWithSISCheck.setAlreadyInCanvas(swsc.isAlreadyInCanvas());
                  break;
               }
            }
         }

         sectionWithSISCheck.setSection(section);
         sectionWithSISCheck.setSisProvisioned(isSisProvisioned);
         sections.add(sectionWithSISCheck);
      }

      // if this variable doesn't exist, add it in so the page doesn't explode
      if (!model.containsAttribute("alreadyInUseList")) {
         List<String> alreadyInUseList = new ArrayList<>();
         model.addAttribute("alreadyInUseList", alreadyInUseList);
      }

      // add course and section info to model
      model.addAttribute("course", course);
      model.addAttribute("sections", sections);

      return new ModelAndView("edit");
   }

   @PostMapping(value = "/submit/{editId}", params = "action=cancel")
   @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
   public ModelAndView cancel(Model model, HttpServletRequest request, @PathVariable("editId") String editId) {
      log.debug("in /cancel");
      getTokenWithoutContext();
      // cancel button. Send back to results screen w/o changes
      return find(model, request, editId);
   }

   @RequestMapping("/submit/{editId}")
   @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
   public ModelAndView submit(Model model, HttpServletRequest request, @PathVariable("editId") String editId,
                              @RequestParam Map<String, String> params) {
      log.debug("in /submit");
      LtiAuthenticationToken token = getTokenWithoutContext();

      String username = (String)token.getPrincipal();

      String courseTitle = params.get("course-title");
      String courseCode = params.get("course-code");
      String sisCourseId = params.get("course-sis-id");

      model.addAttribute("overrideSisCourseId", sisCourseId);
      model.addAttribute("overrideCourseTitle", courseTitle);
      model.addAttribute("overrideCourseCode", courseCode);

      List<String> alreadyInUseList = new ArrayList<>();
      boolean errors = false;

      // TODO pass course info in as a variable to not look it up again?
      Course existingCourse = coursesApi.getCourse(editId);
      boolean courseUpdate = false;
      String existingCourseName = existingCourse.getName();
      String existingCourseCode = existingCourse.getCourseCode();
      String existingSisCourseId = "";
      if (existingCourse.getSisCourseId() != null) {
         existingSisCourseId = existingCourse.getSisCourseId();
      }

      if (!existingCourseName.equals(courseTitle) || !existingSisCourseId.equals(sisCourseId) || !existingSisCourseId.equals(existingCourseCode)) {
         courseUpdate = true;
      }

      if (courseUpdate && !sisCourseId.equals("")) {
         // need this to not have a false positive on looking up if a sis_course_id already exists in Canvas
         boolean newCourseSisId = true;
         if (existingCourse.getSisCourseId() != null) {
            newCourseSisId = !existingCourse.getSisCourseId().equals(sisCourseId);
         }

         if (newCourseSisId) {
            Course existingCourseLookup = coursesApi.getCourse("sis_course_id:" + sisCourseId);

            if (existingCourseLookup != null) {
               errors = true;
               model.addAttribute("courseSisIdError", true);
               alreadyInUseList.add("course-sis-id");
            }
         }
      }

      // section stuff
      boolean readyToUpdateSection = false;
      String sectionId = "";
      String name = "";
      String sisSectionId = "";

      List<SectionWithSISCheck> sections = new ArrayList<>();

      for (Map.Entry<String,String> entry : params.entrySet()) {
         SectionWithSISCheck sectionWithSISCheck = new SectionWithSISCheck();

         if (entry.getKey().contains("section-name-")) {
            sectionId = entry.getKey().substring(13);
            name = entry.getValue();
         } else if (entry.getKey().contains("section-sis-id-")) {
            sisSectionId = entry.getValue();

            // sanity check
            if (sectionId.equals(entry.getKey().substring(15))) {
               readyToUpdateSection = true;
            }
         }

         if (readyToUpdateSection) {
            boolean sectionUpdate = false;
            Section existingSection = sectionsApi.getSection(sectionId);
            String existingSectionName = existingSection.getName();
            String existingSisSectionId = "";

            if (existingSection.getSisSectionId() != null) {
               existingSisSectionId = existingSection.getSisSectionId();
            }

            if (!existingSectionName.equals(name) || !existingSisSectionId.equals(sisSectionId)) {
               sectionUpdate = true;
            }

            // if nothing changed, don't bother with any of this stuff
            if (sectionUpdate) {
               // if it's an empty string, don't bother with error checking
               if (!sisSectionId.equals("")) {
                  Section section = sectionsApi.getSection("sis_section_id:" + sisSectionId);

                  if (section != null) {
                     // found something, let's make sure it didn't find itself before declaring it already in use
                     if (!section.getId().equals(sectionId)) {
                        errors = true;
                        sectionWithSISCheck.setAlreadyInCanvas(true);
                        alreadyInUseList.add("section-sis-id-" + sectionId);
                     }
                  }
               }

               Section sectionToUpdate = new Section();
               sectionToUpdate.setId(sectionId);
               sectionToUpdate.setSisSectionId(sisSectionId);
               sectionToUpdate.setName(name);

               sectionWithSISCheck.setSection(sectionToUpdate);
               sectionWithSISCheck.setOldSectionName(existingSection.getName());
               sectionWithSISCheck.setOldSectionSisId(existingSection.getSisSectionId());
               sections.add(sectionWithSISCheck);
            }

            // reset variables for next iteration
            readyToUpdateSection = false;
            sectionId = "";
            name = "";
            sisSectionId = "";
         }
      }

      // add section overrides, if needed for error pages
      model.addAttribute("sectionOverride", sections);

      // any already in use errors?
      if (errors) {
         model.addAttribute("alreadyInUseList", alreadyInUseList);
         return edit(editId, model, request);
      }

      // No errors, so let's update stuff, sadly one rest call at a time!
      if (courseUpdate) {
         CourseSectionUpdateWrapper courseSectionUpdateWrapper = new CourseSectionUpdateWrapper();
         courseSectionUpdateWrapper.setName(courseTitle);
         courseSectionUpdateWrapper.setSisId(sisCourseId);
         courseSectionUpdateWrapper.setCourseCode(courseCode);
         Course course = coursesApi.updateCourseNameAndSisCourseId(editId, courseSectionUpdateWrapper);

         // if this failed, return
         if (course == null) {
            model.addAttribute("canvasError", true);
            return edit(editId, model, request);
         }

         // no course error, log the change
         CourseAttributeAuditLog audit = new CourseAttributeAuditLog(username);
         audit.setCanvasCourseId(editId);
         audit.setOldCourseName(existingCourse.getName());
         audit.setNewCourseName(courseTitle);
         audit.setOldCourseSisId(existingCourse.getSisCourseId());
         audit.setNewCourseSisId(sisCourseId);
         audit.setOldCourseCode(existingCourse.getCourseCode());
         audit.setNewCourseCode(courseCode);
         courseAttributeAuditLogRepository.save(audit);
      }

      // update section
      // if sections turns out to be empty, this code will just be bypassed
      for (SectionWithSISCheck sectionWithSISCheck : sections) {
         CourseSectionUpdateWrapper courseSectionUpdateWrapper = new CourseSectionUpdateWrapper();
         courseSectionUpdateWrapper.setName(sectionWithSISCheck.getSection().getName());
         courseSectionUpdateWrapper.setSisId(sectionWithSISCheck.getSection().getSisSectionId());
         Section section = sectionsApi.updateSectionNameAndSisCourseId(sectionWithSISCheck.getSection().getId(), courseSectionUpdateWrapper);

         // if this failed, go ahead and return
         if (section == null) {
            model.addAttribute("canvasError", true);
            return edit(editId, model, request);
         }

         // no section error, log the change
         CourseAttributeAuditLog audit = new CourseAttributeAuditLog(username);
         audit.setCanvasCourseId(editId);
         audit.setCanvasSectionId(sectionWithSISCheck.getSection().getId());
         audit.setOldSectionName(sectionWithSISCheck.getOldSectionName());
         audit.setNewSectionName(sectionWithSISCheck.getSection().getName());
         audit.setOldSectionSisId(sectionWithSISCheck.getOldSectionSisId());
         audit.setNewSectionSisId(sectionWithSISCheck.getSection().getSisSectionId());
         courseAttributeAuditLogRepository.save(audit);
      }

      // if we made it here, success!
      model.addAttribute("updateSuccess", true);

      return find(model, request, editId);
   }

   @Data
   private class SectionWithSISCheck {
      Section section;
      boolean isSisProvisioned;
      boolean isAlreadyInCanvas;
      // the "old" section stuff are just here for convenience of the audit log
      String oldSectionName;
      String oldSectionSisId;
   }
}
