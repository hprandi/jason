package jason.infra.centralised;

import java.util.concurrent.ExecutorService;

import jason.asSemantics.ActionExec;
import jason.asSemantics.CircumstanceListener;
import jason.asSemantics.Message;
import jason.infra.components.ActComponent;
import jason.infra.components.DeliberateComponent;
import jason.infra.components.SenseComponent;
import jason.infra.virtual.actuator.Actuator;
import jason.infra.virtual.sensor.Sensor;

public class CentralisedAgArchAsynchronous extends CentralisedAgArch implements Runnable {
    private SenseComponent senseComponent;
    private DeliberateComponent deliberateComponent;
    private ActComponent actComponent;

    private ExecutorService executorSense;
    private ExecutorService executorDeliberate;
    private ExecutorService executorAct;

    public Object objSense = new Object();
    public Object objDeliberate = new Object();
    public Object objAct = new Object();

    public CentralisedAgArchAsynchronous(Sensor sensor, Actuator actuator) {
        super(sensor, actuator);

        this.senseComponent = new SenseComponent(this);
        this.deliberateComponent = new DeliberateComponent(this);
        this.actComponent = new ActComponent(this);
    }

    @Override
    public void wakeUpSense() {
        this.senseComponent.wakeUp();
    }

    @Override
    public void wakeUpDeliberate() {
        this.deliberateComponent.wakeUp();
    }

    @Override
    public void wakeUpAct() {
        this.actComponent.wakeUp();
    }

    public SenseComponent getSenseComponent() {
        return this.senseComponent;
    }

    public DeliberateComponent getDeliberateComponent() {
        return this.deliberateComponent;
    }

    public ActComponent getActComponent() {
        return this.actComponent;
    }

    public ExecutorService getExecutorSense() {
        return this.executorSense;
    }

    public ExecutorService getExecutorDeliberate() {
        return this.executorDeliberate;
    }

    public ExecutorService getExecutorAct() {
        return this.executorAct;
    }

    public void setExecutorAct(ExecutorService executorAct) {
        this.executorAct = executorAct;
    }

    public void setExecutorSense(ExecutorService executorSense) {
        this.executorSense = executorSense;
    }

    public void setExecutorDeliberate(ExecutorService executorDeliberate) {
        this.executorDeliberate = executorDeliberate;
    }

    public void setSenseComponent(SenseComponent senseComponent) {
        this.senseComponent = senseComponent;
    }

    public void addListenerToC(CircumstanceListener listener) {
        this.getTS().getC().addEventListener(listener);
    }

    @Override
    public void receiveMsg(Message m) {
        synchronized (this.objSense) {
            super.receiveMsg(m);
        }
    }

    /** called the the environment when the action was executed */
    @Override
    public void actionExecuted(ActionExec action) {
        synchronized (this.objAct) {
            super.actionExecuted(action);
        }
    }
}
