package jason.infra.virtual.sensor;

import java.util.Collection;

import jason.asSyntax.Literal;

public interface Sensor {

    Collection<Literal> perceive();

}
