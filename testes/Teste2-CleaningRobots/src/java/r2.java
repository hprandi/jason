import jason.infra.virtual.AgentDefinition;
import jason.infra.virtual.actuator.Actuator;
import jason.infra.virtual.sensor.Sensor;

public class r2 implements AgentDefinition {

	public String getName() {
		return "r2";
	}

	public Sensor getSensor() {
		return new MarsSensor(this.getName());
	}

	public Actuator getActuator() {
		return new MarsActuator(this.getName());
	}

}
