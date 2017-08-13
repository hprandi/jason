package jason.infra.virtual.actuator;

import java.util.logging.Logger;

import jason.asSyntax.Literal;

public class SimpleActuator implements Actuator {

	protected Logger logger = Logger.getLogger(SimpleActuator.class.getName());

	@Override
	public boolean act(Literal action) {
		this.logger.severe("Executing action: " + action.toString());
		return true;
	}

}