

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import jason.asSyntax.Literal;
import jason.infra.virtual.sensor.Sensor;

public class LemmingUnitySensor implements Sensor {

	int rounds = 0;

	public Collection<Literal> perceive() {
		Collection<Literal> perceptions = new ArrayList<Literal>();

		String lightStatus = null;
		try {
			lightStatus = UnityConnection.sendMessage("0"); // get light status
		} catch (IOException e) {
			e.printStackTrace();
		}

		Literal literal;
		if (lightStatus.equals("green")) {
			literal = Literal.parseLiteral(Believes.greenLight.toString());
		} else {
			literal = Literal.parseLiteral(Believes.redLight.toString());
		}

		perceptions.add(literal);

		return perceptions;
	}

}
