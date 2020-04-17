package cancel.subscription;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderAsync;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderAsyncClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminDeleteUserRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.VersionListing;
import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Subscription;
import com.braintreegateway.exceptions.NotFoundException;
import io.micronaut.function.FunctionBean;
import io.micronaut.function.executor.FunctionInitializer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@FunctionBean("cancel-subscription")
public class CancelSubscriptionFunction extends FunctionInitializer implements Function<CancelSubscription, Boolean> {
	private final BraintreeGateway gateway = BraintreeGatewayFactory.fromConfigFile(new File("gateway.properties"));
	private final Logger logger = LoggerFactory.getLogger(CancelSubscriptionFunction.class);

	@Override
	public Boolean apply(CancelSubscription request) {
		if(Objects.isNull(request.getCustomerId()) || Objects.isNull(request.getUsername()))
			return false;
		Optional<String> subscriptionId;
		try {
			subscriptionId = gateway.customer().find(request.getCustomerId())
					.getCreditCards().stream()
					.flatMap(creditCard -> creditCard.getSubscriptions().stream())
					.filter(subscription -> !subscription.getStatus().equals(Subscription.Status.CANCELED))
					.map(Subscription::getId)
					.findFirst();
		} catch(NotFoundException ex) {
			logger.error("Customer {} not found", request.getCustomerId());
			return false;
		}
		if(subscriptionId.isPresent()) {
			logger.error("Found subscription {} for customer {}", subscriptionId.get(), request.getCustomerId());
			gateway.subscription().cancel(subscriptionId.get()).isSuccess();
			logger.error("Subscription cancelled");
			deleteUserFromUserPool(request);
			deleteUserFromInstance(request.getUsername());
			deleteObjectVersions(request);
			return true;
		}
		return false;
	}

	private void deleteUserFromUserPool(CancelSubscription msg) {
		AWSCognitoIdentityProviderAsync awsCognitoIdentityProvider = AWSCognitoIdentityProviderAsyncClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
		logger.error("Deleting user {} from user pool", msg.getUsername());
		awsCognitoIdentityProvider.adminDeleteUserAsync(new AdminDeleteUserRequest().withUserPoolId("us-east-1_Z40UJVKbI").withUsername(msg.getUsername()));
	}

	private void deleteUserFromInstance(String user) {
		OkHttpClient httpClient = new OkHttpClient();
		getIpAddresses().parallelStream().forEach(ip ->
				{
					try {
						httpClient.newCall(new Request.Builder().url("http://" + ip + ":8080" + "/volume/deleteUser/" + user)
								.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "")).build())
								.execute();
					} catch(IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
		);
	}

	private List<String> getIpAddresses() {
		AmazonEC2 client = AmazonEC2ClientBuilder.defaultClient();
		DescribeInstancesResult backedupInstances = client.describeInstances(new DescribeInstancesRequest().
				withFilters(new Filter("tag:project", Collections.singletonList("backedup")),
						new Filter("instance-state-name", Collections.singletonList("running"))));
		return backedupInstances.getReservations().stream()
				.flatMap(reservation -> reservation.getInstances().stream()).collect(Collectors.toList())
				.stream().map(Instance::getPublicIpAddress).collect(Collectors.toList());
	}

	private void deleteObjectVersions(CancelSubscription msg) {
		AmazonS3 amazonS3 = AmazonS3ClientBuilder.defaultClient();
		VersionListing versionListing = amazonS3.listVersions("backedup-storage-2", msg.getUsername());
		while(true) {
			versionListing.getVersionSummaries()
					.parallelStream()
					.forEach(s3VersionSummary -> amazonS3.deleteVersion("backedup-storage-2", s3VersionSummary.getKey(), s3VersionSummary.getVersionId()));
			if(versionListing.isTruncated()) {
				versionListing = amazonS3.listNextBatchOfVersions(versionListing);
			} else {
				break;
			}
		}
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