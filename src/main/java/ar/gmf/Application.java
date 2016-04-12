package ar.gmf;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.VerticleFactory;

@SpringBootApplication
@Import(VertxConfig.class)
public class Application {

	protected final static Logger logger = LoggerFactory.getLogger(Application.class);

	@Value("${server.port}")
	Integer port;
	@Autowired
	Vertx vertx;
	@Autowired
	AutowireCapableBeanFactory autowireCapableBeanFactory;

	public static void main(String[] args) throws Exception {

		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		System.setProperty("vertx.cacheDirBase", "/tmp/.vertx");
		System.setProperty("vertx.disableFileCaching", "true");

		SpringApplication.run(Application.class, args);
	}

	@PostConstruct
	public void deployVerticle() {

		vertx.registerVerticleFactory(new VerticleFactory() {
			@Override
			public String prefix() {
				return "spring";
			}
			@Override
			public Verticle createVerticle(String verticleName, ClassLoader classLoader) throws Exception {
				verticleName = VerticleFactory.removePrefix(verticleName);
				return (Verticle)autowireCapableBeanFactory.getBean(Class.forName(verticleName));
			}
		});
		
		JsonObject config = new JsonObject();
		config.put("port", port);

		DeploymentOptions options = new DeploymentOptions();
//		options.setHa(false);
		options.setConfig(config);

		vertx.deployVerticle("ar.gmf.Server", options, h -> {
			if (h.succeeded())
				logger.info("deploy Server {}", h.result());
			else
				logger.error("deploy Server error {} ", h.cause());
		});

//		if (port != 8080) 
		{
			initConsumer = vertx.eventBus().consumer("initClick", msg -> {
				deployClickVerticle(msg.body().toString(), msg);
			});
		}
		
		vertx.eventBus().consumer("emigrar-" + port, msg -> {
			initConsumer.unregister(h -> {
				logger.info("unregister initConsumer");
				
				depIds.stream().forEach(d -> {
					VertxImpl vertxImpl = (VertxImpl)vertx;
					DeploymentOptions deploymentOptions = vertxImpl.getDeployment(d).deploymentOptions();
					
					vertx.eventBus().send("initClick", deploymentOptions.getConfig().getString("session"), h2 -> {
						vertx.eventBus().send("emigrar-v-" + d, "");
					});
					
//					vertx.deployVerticle(vertxImpl.getDeployment(d).verticleIdentifier(), deploymentOptions, h2 -> {
//						vertx.eventBus().send("emigrar-v-" + d, "");
//					});
				});
				
				msg.reply("ok");
			});
		});
	}
	
	MessageConsumer<Object> initConsumer;
	List<String> depIds = new ArrayList<>();
	
	private void deployClickVerticle(String session, Message<Object> msg) {
		DeploymentOptions options = new DeploymentOptions();
		options.setHa(true);
		
		JsonObject config = new JsonObject();
		config.put("session", session);
		options.setConfig(config);
		
		vertx.deployVerticle("spring:ar.gmf.ClickVerticle", options, h -> {
			if (h.succeeded()) {
				logger.info("deploy ClickVerticle {}", h.result());
				depIds.add(h.result());
				if(msg!=null)
					msg.reply(h.result());
			} else {
				logger.error("deploy ClickVerticle error {} ", h.cause());
				if(msg!=null)
					msg.reply(h.cause());
			}
		});
	}
	
//	private void deployClickVerticle2(String session, Message<Object> msg) {
//		DeploymentOptions options = new DeploymentOptions();
//		options.setHa(true);
//		
//		JsonObject config = new JsonObject();
//		config.put("session", session);
//		options.setConfig(config);		
//		
//		ClickVerticle clickVerticle = new ClickVerticle();
//		
//		vertx.deployVerticle(clickVerticle, options, h -> {
//			if (h.succeeded()) {
//				logger.info("deploy ClickVerticle {}", h.result());
//				depIds.add(h.result());
//			} else
//				logger.error("deploy ClickVerticle error {} ", h.cause());
//		});
//	}
}
