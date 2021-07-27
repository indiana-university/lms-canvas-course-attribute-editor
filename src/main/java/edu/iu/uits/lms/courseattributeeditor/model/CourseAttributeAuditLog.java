package edu.iu.uits.lms.courseattributeeditor.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.iu.uits.lms.common.date.DateFormatUtil;
import lombok.Data;
import lombok.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
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


