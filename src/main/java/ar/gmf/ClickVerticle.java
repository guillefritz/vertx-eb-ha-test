package ar.gmf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.shareddata.AsyncMap;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ClickVerticle extends AbstractVerticle implements Serializable {

	private static final long serialVersionUID = 4264536910134665055L;

	protected transient final Logger logger = LoggerFactory.getLogger(getClass());

	/*
	 * session (needed of course)
	 */
	String session;
	
	/*
	 * spring variables
	 */
//	@Autowired
//	EventBus eventBus;
//	@Value("${server.port}")
//	Integer port;

	/*
	 * session variables
	 */
	Integer counter = 0;
	transient XStream x = new XStream(new XppDriver() {
	    @Override
	    public HierarchicalStreamWriter createWriter(Writer out) {
	        return new CompactWriter(out, getNameCoder());
	    }
	});
	
	
//	List<MessageConsumer<Object>> consumers = new ArrayList<>();

	@Override
	public void start() throws Exception {
		super.start();
		
		x.omitField(AbstractVerticle.class, "context");
		x.omitField(AbstractVerticle.class, "vertx");

		session = config().getString("session");

		vertx.eventBus().consumer("emigrate-v-" + deploymentID(), msg -> {
			logger.info("undeploy " + "emigrate-v-" + deploymentID());
			vertx.undeploy(deploymentID(), h -> msg.reply("ok"));
		});
		
		vertx.sharedData().getClusterWideMap("VM", r -> {
			if (r.succeeded()) {
				AsyncMap<Object, Object> asyncMap = r.result();

				asyncMap.get(session, rr -> {
					if (rr.succeeded() && rr.result() != null) {
						logger.info("recovering ClickVerticle desde sesion {}", deploymentID());
						
//						try (ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) rr.result()); GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
//							x.fromXML(gzipIn, ClickVerticle.this);
//						} catch (Exception e) {
//							logger.error("", e);
//						}
						
						XStream x = new XStream();
						x.fromXML((String)rr.result(), this);

//						Kryo kryo = new Kryo();
//						kryo.setReferences(true);
//						FieldSerializer<AbstractVerticle> fs = new FieldSerializer<>(kryo, AbstractVerticle.class);
//						fs.removeField("context");
//						fs.removeField("vertx");
//						kryo.addDefaultSerializer(getClass(), fs);
//						byte[] b = (byte[]) rr.result();
//						Input i = new ByteBufferInput(b);
//						ClickVerticle oldClickVerticle = kryo.readObject(i, ClickVerticle.class);
//						setCounter(oldClickVerticle.getCounter());
					}
				});

//				consumers.add(
				vertx.eventBus().consumer("click-" + session, msg -> {
					counter++;
					logger.info("clicked {} {} ", counter, deploymentID());
					//asyncMap.put(session, counter, h -> msg.reply(counter));
					
//					Kryo kryo = new Kryo();
//					kryo.setReferences(true);
//					FieldSerializer<AbstractVerticle> fs = new FieldSerializer<>(kryo, AbstractVerticle.class);
//					fs.removeField("context");
//					fs.removeField("vertx");
//					kryo.addDefaultSerializer(getClass(), fs);
//					Output o = new Output(new ByteArrayOutputStream());
//					kryo.writeObject(o, ClickVerticle.this);
//					asyncMap.put(session, o.getBuffer(), h -> {
//						logger.info("ook");
//						msg.reply(counter);
//					});
					
//					try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
//						x.toXML(ClickVerticle.this, gzipOut);
//						byte[] bytes = baos.toByteArray();
//						asyncMap.put(session, bytes, h -> {
//							msg.reply(counter);
//						});
//					} catch (Exception e) {
//						logger.error("", e);
//					}
					
					String xml = x.toXML(ClickVerticle.this);
					asyncMap.put(session, xml, h -> {
						//logger.info(xml);
						msg.reply(counter);
					});
				})
//						)
				;
				
//				consumers.add(vertx.eventBus().consumer("click-" + session, msg -> {
//					counter++;
//					logger.info("clicked {} {}", counter, deploymentID());
//
//					vertx.sharedData().getClusterWideMap("VM", r2 -> {
//						if (r2.succeeded()) {
//							AsyncMap<Object, Object> asyncMap2 = r2.result();
//							logger.info("........ {}", session);
//							asyncMap2.put(session, counter, h -> msg.reply(counter));
//						} else {
//							logger.error("error", r2.cause());
//							 msg.reply(counter);
//						}
//					});
//				}));

//				consumers.add(
				vertx.eventBus().consumer("undeployClick-" + session, msg -> {
					logger.info("undeployClick at counter {} {}", counter, deploymentID());
					vertx.undeploy(deploymentID(), h -> {
						logger.info("undeployClick finished {}", h.succeeded());
						msg.reply("ok");
					});
				})
//				)
				;

//				vertx.eventBus().consumer("redeployV2-" + session, msg -> {
//
//					vertx.sharedData().getClusterWideMap("VM", r2 -> {
//						if (r2.succeeded() && r2.result() != null) {
//							logger.info("reinicializando ClickVerticle desde sesion {}", deploymentID());
//							r2.result().get(session, r3 -> {
//								logger.info("........> {}", session);
//								Integer oldCount = (Integer) r3.result();
//								setCounter(oldCount);
//								logger.info("redeployV2 {} {}", counter, deploymentID());
//								msg.reply(counter);
//							});
//						} else {
//							msg.reply(counter);
//						}
//					});
//				});

			} else {
				logger.error("error", r.cause());
			}
		});

	}

	@Override
	public void stop() throws Exception {
		super.stop();
		logger.info("finalizando ClickVerticle {}", deploymentID());
//		consumers.stream().forEach(c -> c.unregister());
	}

	public Integer getCounter() {
		return counter;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}
}
