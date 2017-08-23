package jason.infra.virtual.actuator;

import jason.asSemantics.ActionExec;

public interface Actuator {

    boolean act(ActionExec action);

}
