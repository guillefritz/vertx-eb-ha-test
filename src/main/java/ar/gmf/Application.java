package ar.gmf;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;

@SpringBootApplication
@Import(VertxConfig.class)
public class Application {

	private static final String SESSION2 = "session";

	protected final static Logger logger = LoggerFactory.getLogger(Application.class);

	@Value("${server.port}")
	Integer port;
	@Autowired
	Vertx vertx;
	
	public static void main(String[] args) throws Exception {

		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		System.setProperty("vertx.cacheDirBase", "/tmp/.vertx");
		System.setProperty("vertx.disableFileCaching", "true");

		SpringApplication.run(Application.class, args);
	}

	@PostConstruct
	public void deployVerticle() {
		
		JsonObject config = new JsonObject();
		config.put("port", port);

		DeploymentOptions options = new DeploymentOptions();
		options.setHa(false);
		options.setConfig(config);

		vertx.deployVerticle("ar.gmf.Server", options, h -> {
			if (h.succeeded())
				logger.info("deploy Server {}", h.result());
			else
				logger.error("deploy Server error {} ", h.cause());
		});

		initConsumer = vertx.eventBus().consumer("initClick", msg -> {
			logger.info("initClick {}", msg.body().toString());
			deployClickVerticle(msg.body().toString(), msg);
		});
		
		vertx.eventBus().consumer("emigrate-" + port, msg -> {
			initConsumer.unregister(h -> {
				logger.info("unregister initConsumer");
				
				VertxImpl vertxImpl = (VertxImpl)vertx;
				vertxImpl.deploymentIDs().stream().forEach(_d -> {
					final String d = _d;

					Deployment deployment = vertxImpl.getDeployment(d);
					if (deployment != null) {
						DeploymentOptions deploymentOptions = vertxImpl.getDeployment(d).deploymentOptions();
						if (deploymentOptions.getConfig()!=null && deploymentOptions.getConfig().getString(SESSION2) != null) {
							logger.info("shutting down -> initclick {}", d);
							vertx.eventBus().send("initClick", deploymentOptions.getConfig().getString(SESSION2), h2 -> {
								logger.info("shutting down -> initclick -> emigrate {}", d);
								vertx.eventBus().send("emigrate-v-" + d, "", h4 -> {
									logger.info("shutting down -> initclick -> emigrate -> ok {}", d);
								});
							});
						}
					}

				});
				
				//TODO: has to finish when everything has been emigrated, using CountDownLAtch or similar... 
				vertx.setTimer(10000, t -> msg.reply("ok"));
			});
		});
	}
	
	MessageConsumer<Object> initConsumer;
	List<String> depIds = new ArrayList<>();
	
	private void deployClickVerticle(String session, Message<Object> msg) {
		DeploymentOptions options = new DeploymentOptions();
		options.setHa(true);
		
		JsonObject config = new JsonObject();
		config.put(SESSION2, session);
		options.setConfig(config);
		
		vertx.deployVerticle("spring:ar.gmf.ClickVerticle", options, h -> {
			if (h.succeeded()) {
				logger.info("deploy ClickVerticle {}", h.result());
				depIds.add(h.result());
				
				vertx.eventBus().send("click-"+session, "", r-> {
					if(msg!=null)
						msg.reply(h.result()+r.result());
				});
				
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
