import jason.infra.virtual.AgentDefinition;
import jason.infra.virtual.actuator.Actuator;
import jason.infra.virtual.sensor.Sensor;

public class r1 implements AgentDefinition {

	public String getName() {
		return "r1";
	}

	public Sensor getSensor() {
		return new UnitySensor();
	}

	public Actuator getActuator() {
		return new UnityActuactor();
	}

}
