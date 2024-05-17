package edu.iu.uits.lms.courseattributeeditor.controller;

/*-
 * #%L
 * course-attribute-editor
 * %%
 * Copyright (C) 2023 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.CourseSectionUpdateWrapper;
import edu.iu.uits.lms.canvas.model.Section;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.SectionService;
import edu.iu.uits.lms.courseattributeeditor.CourseAttributeEditorConstants;
import edu.iu.uits.lms.courseattributeeditor.config.ToolConfig;
import edu.iu.uits.lms.courseattributeeditor.model.CourseAttributeAuditLog;
import edu.iu.uits.lms.courseattributeeditor.repository.CourseAttributeAuditLogRepository;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
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
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/app")
@Slf4j
public class CourseAttributeEditorToolController extends OidcTokenAwareController {

   @Autowired
   private ToolConfig toolConfig;

   @Autowired
   private CourseService courseService;

   @Autowired
   private SectionService sectionService;

   @Autowired
   private SisServiceImpl sisService;

   @Autowired
   private CourseAttributeAuditLogRepository courseAttributeAuditLogRepository;

   @RequestMapping({"/index", "/launch"})
   @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
   public ModelAndView index(Model model, HttpServletRequest request) {
      log.debug("in /index");
      getTokenWithoutContext();

      return new ModelAndView("index");
   }

   @RequestMapping("/find")
   @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
   public ModelAndView find(Model model, HttpServletRequest request, @RequestParam("searchBox") String searchBox) {
      log.debug("in /find");
      getTokenWithoutContext();

      if (searchBox == null || searchBox.isEmpty()) {
         model.addAttribute("invalidSearch", true);
         model.addAttribute("invalidSearchMsg", "Canvas Course ID is required.");
         return new ModelAndView("index");
      }

      model.addAttribute("searchBox", searchBox);

      // code to lookup search criteria
      Course course = courseService.getCourse(searchBox);

      if (course != null) {
         // set this regardless of rest of the logic
         model.addAttribute("courseFound", true);
         model.addAttribute("pageTitle", "Canvas Course ID: " + searchBox);
         model.addAttribute("course", course);
         // boo-urns that this is a separate call and not included in courseService.getCourse(searchBox);
         List<Section> sections = courseService.getCourseSections(searchBox);
         model.addAttribute("sections", sections);
         model.addAttribute("caeFocus", CourseAttributeEditorConstants.DETAILS_SECTION);

         if (course.getSisCourseId() != null) {
            // check Sis to see if this is a legit course
            boolean isSis = false;

            if (course.getSisCourseId() != null) {
               isSis = sisService.isLegitSisCourse(course.getSisCourseId());
            }

            if (isSis) {
               // we have a match, so this course is not editable
               model.addAttribute("editable", false);
            } else {
               // no match, so course is editable
               model.addAttribute("editable", true);
            }
         } else {
            // this does not have a sis course id, therefore it is editable
            model.addAttribute("editable", true);
         }
      } else {
         model.addAttribute("pageTitle", "Canvas Course not found");
         model.addAttribute("invalidSearch", true);
         model.addAttribute("invalidSearchMsg", "Canvas Course ID \"" + searchBox + "\" does not exist.");
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
   @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
   public ModelAndView returnTofind(Model model, HttpServletRequest request, @PathVariable("searchBox") String searchBox) {
      return find(model, request, searchBox);
   }

   @RequestMapping("/edit/{editId}")
   @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
   public ModelAndView edit(@PathVariable("editId") String editId, Model model, HttpServletRequest request) {
      log.debug("in /edit");
      getTokenWithoutContext();

      // code to lookup search criteria
      Course course = courseService.getCourse(editId);

      // check Sis to see if this is a legit course / keep users from cheating into this call
      boolean editable = true;

      if (course.getSisCourseId() != null) {
         editable = !sisService.isLegitSisCourse(course.getSisCourseId());
      }

      if (!editable) {
         // user shouldn't be on this page so send them elsewhere
         return find(model, request, editId);
      }

      model.addAttribute("searchBox", editId);
      model.addAttribute("pageTitle", "Edit Course");

      // boo-urns that this is a separate call and not included in courseService.getCourse(searchBox);
      List<Section> listOfSections = courseService.getCourseSections(editId);

      // all this is to determine if the section is an official SIS provisioned course
      List<SectionWithSISCheck> sections = new ArrayList<>();

      for (Section section : listOfSections) {
         SectionWithSISCheck sectionWithSISCheck = new SectionWithSISCheck();

         boolean isSisProvisioned = false;

         if (section.getSis_section_id() != null) {
            // we have a match, so this section is not editable
            isSisProvisioned = sisService.isLegitSisCourse(section.getSis_section_id());
         }

         // check for overrides since there will be errors to include
         List<SectionWithSISCheck> sectionOverrides = (List<SectionWithSISCheck>) model.getAttribute("sectionOverride");

         if (sectionOverrides != null) {
            for (SectionWithSISCheck swsc : sectionOverrides) {
               if (swsc.getSection().getId().equals(section.getId())) {
                  section.setSis_section_id((swsc.getSection().getSis_section_id()));
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

      // add course and section info to model
      model.addAttribute("course", course);
      model.addAttribute("sections", sections);

      model.addAttribute("caeFocus", CourseAttributeEditorConstants.EDIT_SECTION);

      return new ModelAndView("edit");
   }

   @PostMapping(value = "/submit/{editId}", params = "action=cancel")
   @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
   public ModelAndView cancel(Model model, HttpServletRequest request, @PathVariable("editId") String editId) {
      log.debug("in /cancel");
      getTokenWithoutContext();
      // cancel button. Send back to results screen w/o changes
      return find(model, request, editId);
   }

   @RequestMapping("/submit/{editId}")
   @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
   public ModelAndView submit(Model model, HttpServletRequest request, @PathVariable("editId") String editId,
                              @RequestParam Map<String, String> params) {
      log.debug("in /submit");
      OidcAuthenticationToken token = getTokenWithoutContext();
      OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
      String username = oidcTokenUtils.getUserLoginId();

      String courseTitle = params.get("course-title");
      String courseCode = params.get("course-code");
      String sisCourseId = params.get("course-sis-id");

      model.addAttribute("overrideSisCourseId", sisCourseId);
      model.addAttribute("overrideCourseTitle", courseTitle);
      model.addAttribute("overrideCourseCode", courseCode);

      List<String> alreadyInUseList = new ArrayList<>();
      boolean errors = false;

      // TODO pass course info in as a variable to not look it up again?
      Course existingCourse = courseService.getCourse(editId);
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
            Course existingCourseLookup = courseService.getCourse("sis_course_id:" + sisCourseId);

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
            Section existingSection = sectionService.getSection(sectionId);
            String existingSectionName = existingSection.getName();
            String existingSisSectionId = "";

            if (existingSection.getSis_section_id() != null) {
               existingSisSectionId = existingSection.getSis_section_id();
            }

            if (!existingSectionName.equals(name) || !existingSisSectionId.equals(sisSectionId)) {
               sectionUpdate = true;
            }

            // if nothing changed, don't bother with any of this stuff
            if (sectionUpdate) {
               // if it's an empty string, don't bother with error checking
               if (!sisSectionId.equals("")) {
                  Section section = sectionService.getSection("sis_section_id:" + sisSectionId);

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
               sectionToUpdate.setSis_section_id(sisSectionId);
               sectionToUpdate.setName(name);

               sectionWithSISCheck.setSection(sectionToUpdate);
               sectionWithSISCheck.setOldSectionName(existingSection.getName());
               sectionWithSISCheck.setOldSectionSisId(existingSection.getSis_section_id());
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
         Course course = courseService.updateCourseNameAndSisCourseId(editId, courseSectionUpdateWrapper);

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
         courseSectionUpdateWrapper.setSisId(sectionWithSISCheck.getSection().getSis_section_id());
         Section section = sectionService.updateSectionNameAndSisCourseId(sectionWithSISCheck.getSection().getId(), courseSectionUpdateWrapper);

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
         audit.setNewSectionSisId(sectionWithSISCheck.getSection().getSis_section_id());
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
