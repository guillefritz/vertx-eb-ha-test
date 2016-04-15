package ar.gmf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.handler.sockjs.Transport;

public class Server extends AbstractVerticle {

	protected final static Logger logger = LoggerFactory.getLogger(Server.class);

	@Override
	public void start() throws Exception {
		super.start();

		int port = config().getInteger("port");
		
		Router router = Router.router(vertx);

		router.route().handler(BodyHandler.create(System.getProperty("java.io.tmpdir")+"/uploadFiles"));
		router.route().handler(CookieHandler.create());
		
		BridgeOptions options = new BridgeOptions().addInboundPermitted(new PermittedOptions()).addOutboundPermitted(new PermittedOptions());
		SockJSHandlerOptions sockJsOptions = new SockJSHandlerOptions();
		sockJsOptions.addDisabledTransport(Transport.XHR.toString());
		router.route("/eventbus/*").handler(SockJSHandler.create(vertx, sockJsOptions).bridge(options));

		// Serve the static pages
		StaticHandler staticHandler = StaticHandler.create();
		staticHandler.setCachingEnabled(false);
		staticHandler.setDirectoryListing(true);
		staticHandler.setFilesReadOnly(false);
		// staticHandler.setIndexPage(path+"static/index.html");
		router.route("/static/*").handler(staticHandler);

		router.get("/emigrar-" + port).handler(ctx -> {
			vertx.eventBus().send("emigrar-"+port, "", r -> ctx.response().end("ok"));
		});
		
		router.get("/").handler(ctx -> {
			ctx.response().putHeader("location", "/static/index.html").setStatusCode(302).end();
		});
		
		vertx.createHttpServer().requestHandler(router::accept).listen(port, h -> {
			if (h.succeeded())
				logger.info("[Starting Vertx EB server at port {}]", port);
			else
				logger.error("[ERROR starting Vertx EB server at port {}]: {}", port, h.cause());
		});

	}

}
