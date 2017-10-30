

import java.io.IOException;
import java.util.logging.Logger;

import jason.asSemantics.ActionExec;
import jason.infra.virtual.actuator.Actuator;

public class LemmingUnityActuactor implements Actuator {
	protected Logger logger = Logger.getLogger(LemmingUnityActuactor.class.getName());

	public void act(ActionExec actionExec) {
		Actions action = Actions.valueOf(actionExec.getActionTerm().getFunctor());

		this.logger.severe("acting");

		if (action == null) {
			this.logger.severe("Action " + actionExec.getActionTerm().getFunctor() + " not found.");
			return;
		}

		this.logger.severe("Agent executing '" + action.toString() + "' action.");
		boolean actionExecuted = false;

		switch (action) {
		case startWalking:
			actionExecuted = this.sendUnityMessage("1");
			break;
		case stopWalking:
			actionExecuted = this.sendUnityMessage("2");
			break;
		}

		if (actionExecuted) {
			this.logger.severe("Action executed.");
		}

		return;
	}

	private boolean sendUnityMessage(String str) {
		try {
			UnityConnection.sendMessage(str);
			return true;
		} catch (IOException e) {
			this.logger.severe("Error sending Unity message:");
			this.logger.severe(e.toString());
			return false;
		}
	}

}