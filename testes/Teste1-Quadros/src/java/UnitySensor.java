import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import jason.asSyntax.Literal;
import jason.infra.virtual.sensor.Sensor;

public class UnitySensor implements Sensor {

	public static final Literal foundSomethingR1 = Literal.parseLiteral("foundSomething(r1)");

	public Collection<Literal> perceive() {
		Collection<Literal> perceptions = new ArrayList<Literal>();

		String objectName = null;

		try {
			objectName = UnityConnection.sendMessage("0"); // get perceptions
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (objectName != null && !objectName.equals("none")) {
			perceptions.add(foundSomethingR1);
			perceptions.add(Literal.parseLiteral("found_" + objectName));
		}

		return perceptions;
	}

}
