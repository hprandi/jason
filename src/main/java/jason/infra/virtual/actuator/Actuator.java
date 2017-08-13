package jason.infra.virtual.actuator;

import jason.asSyntax.Literal;

public interface Actuator {

	boolean act(Literal action);

}