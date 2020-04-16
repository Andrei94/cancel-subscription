package cancel.subscription;

import io.micronaut.function.executor.FunctionInitializer;
import io.micronaut.function.FunctionBean;

import java.io.IOException;
import java.util.function.Function;

@FunctionBean("cancel-subscription")
public class CancelSubscriptionFunction extends FunctionInitializer implements Function<CancelSubscription, CancelSubscription> {
	@Override
	public CancelSubscription apply(CancelSubscription msg) {
		return msg;
	}

	/**
	 * This main method allows running the function as a CLI application using: echo '{}' | java -jar function.jar
	 * where the argument to echo is the JSON to be parsed.
	 */
	public static void main(String... args) throws IOException {
		CancelSubscriptionFunction function = new CancelSubscriptionFunction();
		function.run(args, (context) -> function.apply(context.get(CancelSubscription.class)));
	}
}

