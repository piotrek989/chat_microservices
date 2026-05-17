package com.chatapp.authservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		long startTime = System.currentTimeMillis();
		String method = request.getMethod();
		String path = request.getRequestURI();

		logger.info("[REQUEST] {} {}", method, path);

		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = System.currentTimeMillis() - startTime;
			int status = response.getStatus();
			HttpStatus httpStatus = HttpStatus.resolve(status);
			String statusText = httpStatus != null ? httpStatus.getReasonPhrase() : "";
			String statusLabel = statusText.isEmpty() ? String.valueOf(status) : status + " " + statusText;

			logger.info("[RESPONSE] {} {} -> {} [{} ms]", method, path, statusLabel, durationMs);
		}
	}
}
