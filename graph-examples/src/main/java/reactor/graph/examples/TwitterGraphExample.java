package reactor.graph.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Environment;
import reactor.event.dispatch.Dispatcher;
import reactor.function.Consumer;
import reactor.function.Function;
import reactor.function.Predicate;
import reactor.graph.Graph;
import reactor.tuple.Tuple;
import reactor.tuple.Tuple2;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * To run this example, you must specify your Twitter API consumer and oauth secrets. If you set the system properties
 * on the command line when running this class, it should Just Work:
 * <p>
 * <pre>
 *     -Dtwitter.consumerKey=
 *     -Dtwitter.consumerSecret=
 *     -Dtwitter.token=
 *     -Dtwitter.tokenSecret=
 *   </pre>
 * </p>
 * <p>
 * Alternatively, you can fill in your own values in the example code, replacing the calls the System.getProperty().
 * </p>
 *
 * @author Jon Brisbin
 */
public class TwitterGraphExample {

	static final Logger LOG = LoggerFactory.getLogger(TwitterGraphExample.class);

	static Map<String, AtomicLong> mentionCounts = new ConcurrentHashMap<>();
	static Map<String, AtomicLong> tagCounts     = new ConcurrentHashMap<>();
	static ObjectMapper            mapper        = new ObjectMapper();

	static {
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	}

	public static void main(String... args) throws InterruptedException, IOException {
		Environment env = new Environment();
		Dispatcher workQueue = env.getDispatcher("workQueue");

		TwitterClient twitter = new TwitterClient();

		Graph<String> graph = Graph.create(env);

		// Count 'mentions' separately. That's any hashtag containing the word 'bieber'.
		graph.node("tag.mentions", workQueue)
		     .consume(new Counter(mentionCounts));

		// Count hashtags and naively trend them.
		graph.node("tag.trending", workQueue)
		     .consume(new Counter(tagCounts))
		     .when(new IsTopTag())
		     .then(new Function<String, Tuple2<String, Long>>() {
			     @Override
			     public Tuple2<String, Long> apply(String tag) {
				     return Tuple.of(tag, tagCounts.get(tag).get());
			     }
		     })
		     .consume(new Consumer<Tuple2<String, Long>>() {
			     @Override
			     public void accept(Tuple2<String, Long> tup) {
				     LOG.info("tag [{}] now at: {}", tup.getT1(), tup.getT2());
			     }
		     });

		graph.node("start")
		     .when(new Predicate<String>() {
			     @Override
			     public boolean test(String tag) {
				     return tag.contains("bieber");
			     }
		     })
		     .routeTo("tag.mentions")
		     .otherwise()
		     .routeTo("tag.trending");

		graph.startNode("start");

		twitter.start();
		String msg;
		while(null != (msg = twitter.getMessageQueue().take())) {
			String user = JsonPath.read(msg, "$.user.screen_name");
			String tweet = JsonPath.read(msg, "$.text");
			LOG.info("@{} has this to say about Justin Bieber: {}", user, tweet);
			List<String> tags = JsonPath.read(msg, "$.entities.hashtags[*].text");
			for(String tag : tags) {
				graph.accept(tag);
			}
		}
		twitter.stop();
	}

	static class Counter implements Consumer<String> {
		final Map<String, AtomicLong> metrics;

		Counter(Map<String, AtomicLong> metrics) {
			this.metrics = metrics;
		}

		@Override
		public void accept(String tag) {
			AtomicLong counter = metrics.get(tag);
			if(null == counter) {
				counter = new AtomicLong(0);
				metrics.put(tag, counter);
			}
			counter.incrementAndGet();
		}
	}

	static class IsTopTag implements Predicate<String> {
		@Override
		public boolean test(String tag) {
			int top = -1;
			Map.Entry<String, AtomicLong> topEntry = null;
			for(Map.Entry<String, AtomicLong> entry : tagCounts.entrySet()) {
				if(entry.getValue().get() > top) {
					top = (int)entry.getValue().get();
					topEntry = entry;
				}
			}

			// Is this the top tag?
			return (null != topEntry && topEntry.getKey().equals(tag));
		}
	}

}
