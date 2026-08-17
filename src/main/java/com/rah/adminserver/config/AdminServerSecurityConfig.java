package com.rah.adminserver.config;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

import de.codecentric.boot.admin.server.config.AdminServerProperties;

@Configuration
@EnableWebFluxSecurity // Activa la seguridad reactiva para WebFlux
public class AdminServerSecurityConfig {

	private final String adminContextPath;

	public AdminServerSecurityConfig(AdminServerProperties adminServerProperties) {
		this.adminContextPath = Optional.ofNullable(adminServerProperties.getContextPath())
				.filter(path -> !path.isBlank()).orElse("");
	}

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		// Configura el manejador de redirección tras un logout exitoso
		RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
		logoutSuccessHandler.setLogoutSuccessUrl(URI.create(this.adminContextPath + "/login"));

		return http.authorizeExchange(exchanges -> exchanges
				// 1. Permitir acceso público a los recursos estáticos del Dashboard
				.pathMatchers(this.adminContextPath + "/assets/**", this.adminContextPath + "/login").permitAll()
				// 2. Cualquier otra petición al Dashboard requiere autenticación
				.anyExchange().authenticated())
				// 3. Configura el formulario de Login nativo del Admin Server
				.formLogin(formLogin -> formLogin.loginPage(this.adminContextPath + "/login"))
				// 4. Configura el Logout
				.logout(logout -> logout.logoutUrl(this.adminContextPath + "/logout")
						.logoutSuccessHandler(logoutSuccessHandler))
				/*
				 * 5. Deshabilitar CSRF solo para los endpoints donde los microservicios se
				 * registran e interactúan
				 * 
				 * IMPORTANTE: requireCsrfProtectionMatcher define qué PETICIONES requieren
				 * token CSRF.
				 */
				.csrf(csrf -> csrf.csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
						// Le pasamos el matcher que "no requiere" CSRF para las rutas excluidas.
						.requireCsrfProtectionMatcher(this.csrfExclusionsMatcher()))
				// 6. Se activa la autenticación basica para los servicios
				.httpBasic(Customizer.withDefaults()).build();
	}

	/**
	 * Devuelve NOT_MATCH (sin protección CSRF) para rutas excluidas.
	 * 
	 * Esto permite que microservicios se registren sin token CSRF.
	 * 
	 * @return
	 */
	ServerWebExchangeMatcher csrfExclusionsMatcher() {

		return exchange -> {
			String path = exchange.getRequest().getPath().value();
			HttpMethod method = exchange.getRequest().getMethod();

			List<String> urlsAllowed = List.of("/login", "/logout", "/instances", "/actuator/").stream()
					.map(this.adminContextPath::concat).toList();

			boolean isExcluded = switch (method.name()) {
			case "GET" -> Boolean.TRUE;
			case "POST" -> urlsAllowed.stream().anyMatch(path::startsWith);
			case "DELETE" -> path.matches(this.adminContextPath + "/instances/[^/]+");
			default -> urlsAllowed.stream().anyMatch(path::startsWith);
			};

			// Si está excluida -> NOT_MATCH (no se requiere CSRF).
			// Si no está excluida -> MATCH (se requiere CSRF).
			if (isExcluded) {
				return ServerWebExchangeMatcher.MatchResult.notMatch();
			}

			return ServerWebExchangeMatcher.MatchResult.match();
		};

	}
}
