import java.io.IOException;
import java.util.logging.Logger;

import jason.asSemantics.ActionExec;
import jason.infra.virtual.actuator.Actuator;

public class UnityActuactor implements Actuator {

	public static final String turnRight = "turnRight";

	protected Logger logger = Logger.getLogger(UnityActuactor.class.getName());

	public void act(ActionExec action) {
		String functor = action.getActionTerm().getFunctor();

		this.logger.severe("Agent is acting.");

		if (functor.equals(turnRight)) {
			this.logger.severe("Agent executing 'turnRight' action.");
			try {
				UnityConnection.sendMessage("1"); // rotate
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
