import java.util.Collection;

import jason.asSyntax.Literal;
import jason.infra.virtual.sensor.Sensor;

public class MarsSensor implements Sensor {

	private String agName;

	public MarsSensor(String agName) {
		this.agName = agName;
	}

	public Collection<Literal> perceive() {
		return MarsEnv.getInstance().consultPercepts(this.agName);
	}

}
