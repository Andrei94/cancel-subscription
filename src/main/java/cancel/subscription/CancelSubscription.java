package cancel.subscription;

import io.micronaut.core.annotation.*;

@Introspected
public class CancelSubscription {
	private String customerId;
	private String username;

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}

