package edu.iu.uits.lms.courseattributeeditor.model;

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

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.iu.uits.lms.common.date.DateFormatUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NonNull;

import java.util.Date;

@Entity
@Table(name = "COURSE_ATTRIBUTE_AUDIT_LOG")
@SequenceGenerator(name = "COURSE_ATTRIBUTE_AUDIT_LOG_ID_SEQ", sequenceName = "COURSE_ATTRIBUTE_AUDIT_LOG_ID_SEQ", allocationSize = 1)
@Data
public class CourseAttributeAuditLog {
    @Id
    @GeneratedValue(generator = "COURSE_ATTRIBUTE_AUDIT_LOG_ID_SEQ")
    private Long id;

    @Column(name = "USERNAME")
    @NonNull
    private String username;

    @Column(name = "CANVAS_COURSE_ID")
    private String canvasCourseId;

    @Column(name = "OLD_COURSE_NAME")
    private String oldCourseName;

    @Column(name = "NEW_COURSE_NAME")
    private String newCourseName;

    @Column(name = "OLD_COURSE_SIS_ID")
    private String oldCourseSisId;

    @Column(name = "NEW_COURSE_SIS_ID")
    private String newCourseSisId;

    @Column(name = "OLD_COURSE_CODE")
    private String oldCourseCode;

    @Column(name = "NEW_COURSE_CODE")
    private String newCourseCode;

    @Column(name = "CANVAS_SECTION_ID")
    private String canvasSectionId;

    @Column(name = "OLD_SECTION_NAME")
    private String oldSectionName;

    @Column(name = "NEW_SECTION_NAME")
    private String newSectionName;

    @Column(name = "OLD_SECTION_SIS_ID")
    private String oldSectionSisId;

    @Column(name = "NEW_SECTION_SIS_ID")
    private String newSectionSisId;

    @JsonFormat(pattern= DateFormatUtil.JSON_DATE_FORMAT)
    @Column(name = "DATE_CHANGED")
    private Date dateChanged;

    @PreUpdate
    @PrePersist
    public void updateTimeStamps() {
        if (dateChanged==null) {
            dateChanged = new Date();
        }
    }
}


