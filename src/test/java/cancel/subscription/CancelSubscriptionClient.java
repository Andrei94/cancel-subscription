package cancel.subscription;

import io.micronaut.function.client.FunctionClient;
import io.micronaut.http.annotation.Body;
import io.reactivex.Single;

import javax.inject.Named;

@FunctionClient
public interface CancelSubscriptionClient {
	@Named("cancel-subscription")
	Single<Boolean> apply(@Body CancelSubscription body);
}
