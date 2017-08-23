package jason.infra.virtual.actuator;

import java.util.logging.Logger;

import jason.asSemantics.ActionExec;

public class SimpleActuator implements Actuator {

    protected Logger logger = Logger.getLogger(SimpleActuator.class.getName());

    @Override
    public boolean act(ActionExec action) {
        this.logger.severe("Executing action: " + action.toString());
        return true;
    }

}
