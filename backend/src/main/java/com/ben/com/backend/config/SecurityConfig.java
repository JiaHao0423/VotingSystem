package com.ben.com.backend.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
	private List<String> allowedOrigins;

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	@Order(1)
	SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/api/admin/**")
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
				.httpBasic(Customizer.withDefaults());

		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain voterSecurityFilterChain(
			HttpSecurity http,
			SecurityContextRepository securityContextRepository
	) throws Exception {
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository)
						.requireExplicitSave(false)
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/api/community", "/api/units/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/qr/preview").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/verify", "/api/auth/qr").permitAll()
						.requestMatchers("/api/auth/me").hasRole("VOTER")
						.requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
						.requestMatchers("/api/proposals/**").hasRole("VOTER")
						.anyRequest().permitAll()
				);

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		var config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
