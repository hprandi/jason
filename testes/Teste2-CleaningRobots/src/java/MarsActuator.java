import jason.asSemantics.ActionExec;
import jason.infra.virtual.actuator.Actuator;

public class MarsActuator implements Actuator {

	private String agName;

	public MarsActuator(String agName) {
		this.agName = agName;
	}

	public void act(ActionExec action) {
		MarsEnv.getInstance().scheduleAction(this.agName, action.getActionTerm(), action);
	}

}
