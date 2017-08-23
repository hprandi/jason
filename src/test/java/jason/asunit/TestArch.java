package jason.asunit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jason.JasonException;
import jason.asSemantics.ActionExec;
import jason.asSyntax.Literal;
import jason.environment.Environment;
import jason.infra.centralised.BaseCentralisedMAS;
import jason.infra.centralised.CentralisedAgArch;
import jason.infra.centralised.CentralisedEnvironment;
import jason.infra.virtual.actuator.SimpleActuator;
import jason.infra.virtual.sensor.SimpleSensor;

public class TestArch extends CentralisedAgArch implements Runnable {

    private static int nameCount = 0;

    private Condition condition;
    private int cycle = 0;

    private List<Literal> actions = new ArrayList<>();

    StringBuilder output = new StringBuilder();

    public TestArch() {
        this("ASUnitTest" + nameCount++);
    }

    public TestArch(String agName) {
        super(new SimpleSensor(), new SimpleActuator());
        try {
            this.setAgName(agName);
            BaseCentralisedMAS.getRunner().addAg(this);
        } catch (JasonException e) {
            e.printStackTrace();
        }
    }

    public int getCycle() {
        return this.cycle;
    }

    public List<Literal> getActions() {
        return this.actions;
    }

    public void start(Condition c) {
        this.condition = c;
        this.cycle = 0;
        this.actions.clear();
        new Thread(this).start();
    }

    @Override
    public void run() {
        synchronized (this.condition) {
            while (this.condition.test(this)) {
                this.cycle++;
                this.getTS().reasoningCycle();
                if (this.getTS().canSleep()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            this.condition.notifyAll();
        }
    }

    public void setEnv(Environment env) {
        try {
            CentralisedEnvironment infraEnv = new CentralisedEnvironment(null, BaseCentralisedMAS.getRunner());
            infraEnv.setUserEnvironment(env);
            env.setEnvironmentInfraTier(infraEnv);
            this.setEnvInfraTier(infraEnv);
        } catch (JasonException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Collection<Literal> perceive() {
        // System.out.println(super.perceive()+"*"+getEnvInfraTier());
        if (this.getEnvInfraTier() != null) {
            return super.perceive();
        } else {
            return null;
        }
    }

    @Override
    public void act(ActionExec action) { // , List<ActionExec> feedback) {
        this.actions.add(action.getActionTerm());
        if (this.getEnvInfraTier() != null) {
            super.act(action); // , feedback); //env.scheduleAction(getAgName(), action.getActionTerm(), action);
        } else {
            action.setResult(true);
            this.actionExecuted(action); // feedback.add(action);
        }
    }

    public void print(String s) {
        System.out.println(s);
        this.output.append(s + "\n");
    }

    public StringBuilder getOutput() {
        return this.output;
    }

    public void clearOutput() {
        this.output = new StringBuilder();
    }
}
