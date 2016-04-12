package ar.gmf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class VertxConfig {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
//	@Bean(destroyMethod = "shutdown")
//	public HazelcastInstance hazelcastInstance(Environment env) {
//		Config config = new ClasspathXmlConfig("cluster.xml");
//		config.setInstanceName("simViewHZ");
//		return Hazelcast.newHazelcastInstance(config);
//	}
//
//	@Bean(destroyMethod = "close")
//	public Vertx vertx(Environment env, HazelcastInstance hazelcastInstance) throws Throwable {
//		return clusteredVertx(new VertxOptions().setClusterManager(new HazelcastClusterManager(hazelcastInstance)).setClustered(true).setHAEnabled(true));
//	}
	@Bean(destroyMethod = "close")
	public Vertx vertx(Environment env) throws Throwable {
		return clusteredVertx((new VertxOptions()).setClustered(true).setHAEnabled(true));
	}
	
	@Bean(destroyMethod = "close")
	public SessionStore sessionStore(Vertx vertx) {
		return ClusteredSessionStore.create(vertx);
	}
	
	private Vertx clusteredVertx(VertxOptions options) throws Throwable {
		return clusteredVertx(handler -> Vertx.clusteredVertx(options, handler));
	}

	protected <T> T clusteredVertx(Consumer<Handler<AsyncResult<T>>> consumer) throws Throwable {
		AtomicReference<AsyncResult<T>> result = new AtomicReference<>();
		CountDownLatch countDownLatch = new CountDownLatch(1);
		clusteredVertx(consumer, ar -> {
			result.set(ar);
			countDownLatch.countDown();
		});
		if (!countDownLatch.await(2, TimeUnit.MINUTES)) {
			throw new RuntimeException("Timed out trying to join cluster!");
		}
		AsyncResult<T> ar = result.get();
		if (ar.succeeded()) {
			return ar.result();
		} else {
			throw ar.cause();
		}
	}

	private <T> void clusteredVertx(Consumer<Handler<AsyncResult<T>>> consumer, Handler<AsyncResult<T>> handler) {
		logger.info("Iniciando Vert.x");
		consumer.accept(handler);
	}

	@Bean
	public EventBus eventBus(Vertx vertx) {
		return vertx.eventBus();
	}
}
