package reactor.graph.examples;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Jon Brisbin
 */
public class TwitterClient {

	private final String                consumerKey    = System.getProperty("twitter.consumerKey");
	private final String                consumerSecret = System.getProperty("twitter.consumerSecret");
	private final String                token          = System.getProperty("twitter.token");
	private final String                tokenSecret    = System.getProperty("twitter.tokenSecret");
	private final BlockingQueue<String> msgQueue       = new LinkedBlockingDeque<>(10000);
	private final BlockingQueue<Event>  evtQueue       = new LinkedBlockingDeque<>(1000);

	private final StatusesFilterEndpoint endpoint;
	private final Authentication         auth;
	private final BasicClient            client;

	public TwitterClient() {
		this.endpoint = new StatusesFilterEndpoint();
		this.endpoint.trackTerms(Arrays.asList("bieber"));
		this.auth = new OAuth1(consumerKey, consumerSecret, token, tokenSecret);
		this.client = new ClientBuilder()
				.name("bieber-tracker")
				.hosts(new HttpHosts(Constants.STREAM_HOST))
				.authentication(this.auth)
				.endpoint(this.endpoint)
				.processor(new StringDelimitedProcessor(msgQueue))
				.eventMessageQueue(evtQueue)
				.build();
	}

	public void start() {
		client.connect();
	}

	public void stop() {
		client.stop(0);
	}

	public BlockingQueue<String> getMessageQueue() {
		return msgQueue;
	}

}
