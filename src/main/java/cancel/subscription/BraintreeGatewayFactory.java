package cancel.subscription;

import com.braintreegateway.BraintreeGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BraintreeGatewayFactory {
	private static final Logger logger = LoggerFactory.getLogger(BraintreeGatewayFactory.class);

	public static BraintreeGateway fromConfigFile(File configFile) {
		InputStream inputStream = null;
		Properties properties = new Properties();

		try {
			inputStream = new FileInputStream(configFile);
			properties.load(inputStream);
		} catch(IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				if(inputStream != null) {
					inputStream.close();
				}
			} catch(IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

		return new BraintreeGateway(
				properties.getProperty("environment"),
				properties.getProperty("merchantId"),
				properties.getProperty("publicKey"),
				properties.getProperty("privateKey")
		);
	}
}