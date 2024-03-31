package factoid.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
      .info(apiInfo());
  }

  private Info apiInfo() {
    return new Info()
      .title("Pathway Commons Biofactoid/BioPAX data converters")
      .description("An experimental/research RESTful web service to convert Biofactoid documents to BioPAX or SBGN formats.")
      .version("2")
      .contact(apiContact())
      .license(apiLicence());
  }

  private License apiLicence() {
    return new License()
      .name("MIT Licence")
      .url("https://opensource.org/licenses/mit-license.php");
  }

  private Contact apiContact() {
    return new Contact()
      .name("Pathway Commons")
      .email("pathway-commons-help@googlegroups.com")
      .url("https://www.pathwaycommons.org");
  }

}
