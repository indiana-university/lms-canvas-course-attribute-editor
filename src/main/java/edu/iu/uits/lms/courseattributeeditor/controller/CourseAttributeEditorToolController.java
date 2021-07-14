package edu.iu.uits.lms.courseattributeeditor.controller;

import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.api.SectionsApi;
import canvas.client.generated.model.Course;
import canvas.client.generated.model.Section;
import edu.iu.uits.lms.courseattributeeditor.config.ToolConfig;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import iuonly.client.generated.api.SudsApi;
import iuonly.client.generated.model.SudsCourse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
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
               SudsCourse sudsCourseArchive = sudsApi.getSudsArchiveCourseBySiteId(course.getSisCourseId());
               // check archive table
               if (sudsCourseArchive != null) {
                  // we have a match, so this course is not editable
                  model.addAttribute("editable", false);
               } else {
                  // Found in Canvas, but not in Suds or archive table, so it is editable
                  model.addAttribute("editable", true);
               }
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

      model.addAttribute("breadcrumb", true);
      model.addAttribute("tier3breadcrumb", true);
      model.addAttribute("searchBox", editId);
      model.addAttribute("pageTitle", "Edit Course");

      // code to lookup search criteria
      Course course = coursesApi.getCourse(editId);
      // boo-urns that this is a separate call and not included in coursesApi.getCourse(searchBox);
      List<Section> listOfSections = coursesApi.getCourseSections(editId);

      // all this is to determine if the section is an official SIS provisioned course
      List<SectionWithSISCheck> sections = new ArrayList<>();

      for (Section section : listOfSections) {
         SectionWithSISCheck sectionWithSISCheck = new SectionWithSISCheck();
         SudsCourse sudsCourse = sudsApi.getSudsCourseBySiteId(section.getSisSectionId());
         boolean isSisProvisioned = false;
         if (sudsCourse != null) {
            // we have a match, so this section is not editable
            isSisProvisioned = true;
         } else {
            SudsCourse sudsCourseArchive = sudsApi.getSudsArchiveCourseBySiteId(section.getSisSectionId());
            if (sudsCourseArchive != null) {
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
                  sectionWithSISCheck.setDupe(swsc.isDupe());
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

   @RequestMapping("/submit/{editId}")
   @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
   public ModelAndView submit(Model model, HttpServletRequest request, @PathVariable("editId") String editId,
                              @RequestParam Map<String, String> params, @RequestParam String action) {
      log.debug("in /submit");
      getTokenWithoutContext();

      // cancel button. Send back to results screen w/o changes
      if ("cancel".equals(action)) {
         return find(model, request, editId);
      }

      String courseTitle = params.get("course-title");
      String sisCourseId = params.get("course-sis-id");

      model.addAttribute("overrideSisCourseId", sisCourseId);
      model.addAttribute("overrideCourseTitle", courseTitle);

      List<String> alreadyInUseList = new ArrayList<>();
      boolean errors = false;

      if (!sisCourseId.equals("")) {
         // TODO pass course info in as a variable to not look it up again?
         Course course = coursesApi.getCourse(editId);

         // need this to not have a false positive on looking up if a sis_course_id already exists in Canvas
         boolean newCourseSisId = true;
         if (course.getSisCourseId() != null) {
            newCourseSisId = !course.getSisCourseId().equals(sisCourseId);
         }

         if (newCourseSisId) {
            Course existingCourseLookup = coursesApi.getCourseWithSisId(sisCourseId, "sis_course_id");

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
            // if it's an empty string, don't bother with error checking
            if (!sisSectionId.equals("")) {
               Section section = sectionsApi.getSectionWithSisId(sisSectionId, "sis_section_id");

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
            sections.add(sectionWithSISCheck);

            // reset variables for next iteration
            readyToUpdateSection = false;
            sectionId = "";
            name = "";
            sisSectionId = "";
         }
      }

      // any already in use errors?
      if (errors) {
         model.addAttribute("sectionOverride", sections);
         model.addAttribute("alreadyInUseList", alreadyInUseList);
         return edit(editId, model, request);
      }

      // No errors, so let's update stuff, sadly one rest call at a time!
      Course course = coursesApi.updateCourseNameAndSisCourseId(editId, courseTitle, sisCourseId);

      // if this failed, return
      if (course == null) {
         model.addAttribute("sectionOverride", sections);
         model.addAttribute("canvasError", true);
         return edit(editId, model, request);
      }

      // update section
      for (SectionWithSISCheck sectionWithSISCheck : sections) {
         Section section = sectionsApi.updateSectionNameAndSisCourseId(sectionWithSISCheck.getSection().getId(), sectionWithSISCheck.getSection().getName(), sectionWithSISCheck.getSection().getSisSectionId());

         // if this failed, go ahead and return
         if (section == null) {
            model.addAttribute("sectionOverride", sections);
            model.addAttribute("canvasError", true);
            return edit(editId, model, request);
         }
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
      boolean isDupe;
   }
}
