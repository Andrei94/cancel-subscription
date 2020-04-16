package cancel.subscription;

import io.micronaut.core.annotation.*;

@Introspected
public class CancelSubscription {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

