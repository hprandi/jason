package jason.infra.virtual;

import jason.infra.virtual.actuator.Actuator;
import jason.infra.virtual.sensor.Sensor;

public interface AgentDefinition {

    String getName();

    Actuator getActuator();

    Sensor getSensor();

}
