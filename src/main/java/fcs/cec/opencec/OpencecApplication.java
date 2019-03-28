package fcs.cec.opencec;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import nz.net.ultraq.thymeleaf.LayoutDialect;

@SpringBootApplication
public class OpencecApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpencecApplication.class, args);
	}

	@Bean
	public SpringTemplateEngine templateEngine(ApplicationContext ctx) {
		SpringTemplateEngine templateEngine = new SpringTemplateEngine();

		SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
		templateResolver.setCharacterEncoding("UTF-8");
		templateResolver.setApplicationContext(ctx);
		templateResolver.setPrefix("templates/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateEngine.setTemplateResolver(templateResolver);
		templateEngine.addDialect(new LayoutDialect());
		return templateEngine;
	}

//	@Bean
//	public FilterRegistrationBean<RequestResponseLoggingFilter> loggingFilter() {
//		FilterRegistrationBean<RequestResponseLoggingFilter> registrationBean = new FilterRegistrationBean<>();
//		registrationBean.setFilter(new RequestResponseLoggingFilter());
//		registrationBean.addUrlPatterns("/lesson/*");
//		registrationBean.addUrlPatterns("/checkVideo");
//		return registrationBean;
//	}
}
