package jason.infra.centralised;

import java.util.concurrent.ExecutorService;

import jason.infra.virtual.actuator.Actuator;
import jason.infra.virtual.sensor.Sensor;

/** an agent architecture for the infra based on thread pool */
public final class CentralisedAgArchForPool extends CentralisedAgArch {
    private volatile boolean isSleeping = false;
    private ExecutorService executor;

    public CentralisedAgArchForPool(Sensor sensor, Actuator actuator) {
        super(sensor, actuator);
    }

    public void setExecutor(ExecutorService e) {
        this.executor = e;
    }

    @Override
    public void sleep() {
        this.isSleeping = true;
        /*
         * Agent.getScheduler().schedule(new Runnable() {
         * public void run() {
         * wake();
         * }
         * }, MAX_SLEEP, TimeUnit.MILLISECONDS);
         */
    }

    @Override
    public void wake() {
        synchronized (this) {
            if (this.isSleeping) {
                this.isSleeping = false;
                this.executor.execute(this);
            }
        }
    }

    @Override
    public void run() {
        int number_cycles = this.getCycles();
        int i = 0;

        while (this.isRunning() && i++ < number_cycles) {
            this.reasoningCycle();
            synchronized (this) {
                if (this.getTS().canSleep()) {
                    this.sleep();
                    return;
                } else if (i == number_cycles) {
                    this.executor.execute(this);
                    return;
                }
            }
        }
    }
}
