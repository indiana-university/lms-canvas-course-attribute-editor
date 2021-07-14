package edu.iu.uits.lms.courseattributeeditor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "course-attribute-editor")
@Getter
@Setter
public class ToolConfig {

   private String version;
   private String env;
}
