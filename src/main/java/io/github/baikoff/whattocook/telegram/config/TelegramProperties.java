package io.github.baikoff.whattocook.telegram.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.telegram")
public class TelegramProperties {

	private boolean enabled;

	@NotBlank
	private String token;

	@NotEmpty
	private Set<Long> allowedUserIds = new LinkedHashSet<>();

	@NotBlank
	private String apiBaseUrl = "https://api.telegram.org";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Set<Long> getAllowedUserIds() {
		return Set.copyOf(allowedUserIds);
	}

	public void setAllowedUserIds(Set<Long> allowedUserIds) {
		this.allowedUserIds = new LinkedHashSet<>(allowedUserIds);
	}

	public String getApiBaseUrl() {
		return apiBaseUrl;
	}

	public void setApiBaseUrl(String apiBaseUrl) {
		this.apiBaseUrl = apiBaseUrl;
	}
}
