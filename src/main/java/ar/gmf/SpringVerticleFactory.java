package ar.gmf;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;

@Component
public class SpringVerticleFactory implements VerticleFactory {
	
	@Autowired
	BeanFactory beanFactory;
	
	@Override
	public String prefix() {
		return "spring";
	}
	
	@Override
	public boolean blockingCreate() {
		return true;
	}
	
	@Override
	public Verticle createVerticle(String verticleName, ClassLoader classLoader) throws Exception {
		verticleName = VerticleFactory.removePrefix(verticleName);
		return (Verticle) beanFactory.getBean(Class.forName(verticleName));
	}
}
