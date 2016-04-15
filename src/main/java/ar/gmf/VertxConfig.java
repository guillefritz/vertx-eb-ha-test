package ar.gmf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * Instancia de Vertx de SIM VIEW con su propio cluster de HZ (EventBus y VMs)
 * 
 * @author guille
 *
 */
public class VertxConfig {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${vertx.haEnabled:true}")
	Boolean haEnabled;
	@Autowired
	SpringVerticleFactory springVerticleFactory;
	
	/*
	 * TODO: borrar ya que se arma con HazelcastConfig
	 */
//	@Bean(destroyMethod = "shutdown")
//	public HazelcastInstance hazelcastInstance(Environment env) {
//
//		if (StringUtils.isBlank(System.getProperty("app.nodo"))) {
//			System.setProperty("app.nodo", env.getProperty("app.nodo", "TA"));
//		}
//		if (StringUtils.isBlank(System.getProperty("app.members"))) {
//			System.setProperty("app.members", env.getProperty("app.members", "127.0.0.1"));
//		}
//		if (StringUtils.isBlank(System.getProperty("spring.profiles.active"))) {
//			System.setProperty("spring.profiles.active", env.getProperty("spring.profiles.active", ""));
//		}
//
//		Config config = new ClasspathXmlConfig("clusterSIM.xml");
//		config.setLiteMember(true);
//		return Hazelcast.newHazelcastInstance(config);
//	}

	@Bean(destroyMethod = "close")
	public Vertx vertx(Environment env, SpringVerticleFactory springVerticleFactory) throws Throwable {

		if (StringUtils.isBlank(System.getProperty("app.nodo"))) {
			System.setProperty("app.nodo", env.getProperty("app.nodo", "TA"));
		}
		if (StringUtils.isBlank(System.getProperty("app.members"))) {
			System.setProperty("app.members", env.getProperty("app.members", "127.0.0.1"));
		}
		if (StringUtils.isBlank(System.getProperty("spring.profiles.active"))) {
			System.setProperty("spring.profiles.active", env.getProperty("spring.profiles.active", ""));
		}

		Config config = new ClasspathXmlConfig("clusterVIEW.xml");
		HazelcastInstance hazelcastViewInstance = Hazelcast.newHazelcastInstance(config);
		Vertx vertx = clusteredVertx(new VertxOptions().setClusterManager(new HazelcastClusterManager(hazelcastViewInstance)).setClustered(true).setHAEnabled(haEnabled));
		vertx.registerVerticleFactory(springVerticleFactory);
		return vertx;
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
