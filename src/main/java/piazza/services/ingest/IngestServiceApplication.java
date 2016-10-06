/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package piazza.services.ingest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

//import piazza.commons.elasticsearch.NativeElasticsearchTemplateConfiguration;


//@Configuration
//@EnableAutoConfiguration 
//@ContextConfiguration(classes = NativeElasticsearchTemplateConfiguration.class)
@SpringBootApplication
@ComponentScan({"piazza, util"})
public class IngestServiceApplication {

	public static void main(String[] args) {

		SpringApplication.run(IngestServiceApplication.class, args); //NOSONAR
	}
	
	@Configuration
	protected static class GatewayConfig extends WebMvcConfigurerAdapter {
		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(new HandlerInterceptorAdapter() {
				@Override
				public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
					if ((request.getScheme().equals("http")) || (request.getHeader("X-Forwarded-Proto").equals("http"))) {
						String redirectUrl = String.format("%s://%s%s", "https", request.getServerName(), request.getRequestURI());
						response.sendRedirect(redirectUrl);
						return false;
					}
					return true;
				}
			});
		}
	}
}
