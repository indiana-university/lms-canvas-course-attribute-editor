package edu.iu.uits.lms.courseattributeeditor.repository;

import edu.iu.uits.lms.courseattributeeditor.model.CourseAttributeAuditLog;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;

@Component
public interface CourseAttributeAuditLogRepository extends PagingAndSortingRepository<CourseAttributeAuditLog, Long> {

}
