package jason.infra.virtual.sensor;

import java.util.ArrayList;
import java.util.Collection;

import jason.asSyntax.Literal;

public class SimpleSensor implements Sensor {

    @Override
    public Collection<Literal> perceive() {
        Collection<Literal> perceptions = new ArrayList<>();
        Literal ltrue = Literal.parseLiteral("worldIsOk in sensor");
        perceptions.add(ltrue);

        return perceptions;
    }

}
