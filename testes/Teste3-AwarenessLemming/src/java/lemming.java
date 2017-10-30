

import jason.infra.virtual.AgentDefinition;
import jason.infra.virtual.actuator.Actuator;
import jason.infra.virtual.sensor.Sensor;

public class lemming implements AgentDefinition {

	public String getName() {
		return "lemming";
	}

	public Sensor getSensor() {
		return new LemmingUnitySensor();
	}

	public Actuator getActuator() {
		return new LemmingUnityActuactor();
	}

}
