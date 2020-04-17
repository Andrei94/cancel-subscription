package cancel.subscription;

import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class CancelSubscriptionFunctionTest {
	@Inject
	CancelSubscriptionClient client;

	@Test
	public void testFunction() {
		CancelSubscription body = new CancelSubscription();
		body.setCustomerId("cancel-subscription");
		assertEquals(false, client.apply(body).blockingGet());
	}
}
