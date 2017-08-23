package jason.infra.centralised;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.activity.InvalidActivityException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import jason.JasonException;
import jason.asSemantics.Agent;
import jason.asSyntax.directives.DirectiveProcessor;
import jason.asSyntax.directives.Include;
import jason.bb.DefaultBeliefBase;
import jason.control.ExecutionControlGUI;
import jason.infra.components.CircumstanceListenerComponents;
import jason.infra.virtual.AgentDefinition;
import jason.infra.virtual.actuator.Actuator;
import jason.infra.virtual.sensor.Sensor;
import jason.mas2j.AgentParameters;
import jason.mas2j.ClassParameters;
import jason.mas2j.MAS2JProject;
import jason.mas2j.parser.ParseException;
import jason.runtime.MASConsoleGUI;
import jason.runtime.MASConsoleLogFormatter;
import jason.runtime.MASConsoleLogHandler;
import jason.runtime.Settings;
import jason.runtime.SourcePath;
import jason.util.Config;

/**
 * Runs MASProject using centralised infrastructure.
 */
public class RunCentralisedMAS extends BaseCentralisedMAS {

    private JButton btDebug;

    public RunCentralisedMAS() {
        super();
        runner = this;
    }

    @Override
    public boolean hasDebugControl() {
        return this.btDebug != null;
    }

    @Override
    public void enableDebugControl() {
        this.btDebug.setEnabled(true);
    }

    public static void main(String[] args) throws JasonException {
        RunCentralisedMAS r = new RunCentralisedMAS();
        runner = r;
        r.init(args);
        r.create();
        r.start();
        r.waitEnd();
        r.finish();
    }

    protected int init(String[] args) {
        String projectFileName = null;
        if (args.length < 1) {
            if (RunCentralisedMAS.class.getResource("/" + defaultProjectFileName) != null) {
                projectFileName = defaultProjectFileName;
                readFromJAR = true;
                Config.get(false); // to void to call fix/store the configuration in this case everything is read from a jar/jnlp file
            } else {
                System.out.println("Jason " + Config.get().getJasonVersion());
                System.err.println("You should inform the MAS project file.");
                // JOptionPane.showMessageDialog(null,"You should inform the project file as a parameter.\n\nJason version "+Config.get().getJasonVersion()+" library built on
                // "+Config.get().getJasonBuiltDate(),"Jason", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        } else {
            projectFileName = args[0];
        }

        if (Config.get().getJasonJar() == null) {
            System.out.println("Jason is not configured, creating a default configuration");
            Config.get().fix();
        }

        this.setupLogger();

        if (args.length >= 2) {
            if (args[1].equals("-debug")) {
                debug = true;
                Logger.getLogger("").setLevel(Level.FINE);
            }
        }

        // discover the handler
        for (Handler h : Logger.getLogger("").getHandlers()) {
            // if there is a MASConsoleLogHandler, show it
            if (h.getClass().toString().equals(MASConsoleLogHandler.class.toString())) {
                MASConsoleGUI.get().getFrame().setVisible(true);
                MASConsoleGUI.get().setAsDefaultOut();
            }
        }

        int errorCode = 0;

        try {
            if (projectFileName != null) {
                InputStream inProject;
                if (readFromJAR) {
                    inProject = RunCentralisedMAS.class.getResource("/" + defaultProjectFileName).openStream();
                    urlPrefix = SourcePath.CRPrefix + "/";
                } else {
                    URL file;
                    // test if the argument is an URL
                    try {
                        file = new URL(projectFileName);
                        if (projectFileName.startsWith("jar")) {
                            urlPrefix = projectFileName.substring(0, projectFileName.indexOf("!") + 1) + "/";
                        }
                    } catch (Exception e) {
                        file = new URL("file:" + projectFileName);
                    }
                    inProject = file.openStream();
                }
                jason.mas2j.parser.mas2j parser = new jason.mas2j.parser.mas2j(inProject);
                project = parser.mas();
            } else {
                project = new MAS2JProject();
            }

            project.setupDefault();
            project.getSourcePaths().setUrlPrefix(urlPrefix);
            project.registerDirectives();
            // set the aslSrcPath in the include
            ((Include) DirectiveProcessor.getDirective("include")).setSourcePath(project.getSourcePaths());

            project.fixAgentsSrc();

            if (MASConsoleGUI.hasConsole()) {
                MASConsoleGUI.get().setTitle("MAS Console - " + project.getSocName());

                this.createButtons();
            }

            // runner.waitEnd();
            errorCode = 0;

        } catch (FileNotFoundException e1) {
            logger.log(Level.SEVERE, "File " + projectFileName + " not found!");
            errorCode = 2;
        } catch (ParseException e) {
            logger.log(Level.SEVERE, "Error parsing file " + projectFileName + "!", e);
            errorCode = 3;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error!?: ", e);
            errorCode = 4;
        }

        System.out.flush();
        System.err.flush();

        if (!MASConsoleGUI.hasConsole() && errorCode != 0) {
            System.exit(errorCode);
        }
        return errorCode;
    }

    /** create environment, agents, controller */
    protected void create() throws JasonException {
        this.createEnvironment();
        this.createAgs();
        this.createController();
    }

    /** start agents, .... */
    protected void start() {
        this.startAgs();
        this.startSyncMode();
    }

    @Override
    public synchronized void setupLogger() {
        if (readFromJAR) {
            try {
                LogManager.getLogManager().readConfiguration(RunCentralisedMAS.class.getResource("/" + logPropFile).openStream());
            } catch (Exception e) {
                Handler[] hs = Logger.getLogger("").getHandlers();
                for (int i = 0; i < hs.length; i++) {
                    Logger.getLogger("").removeHandler(hs[i]);
                }
                Handler h = new MASConsoleLogHandler();
                h.setFormatter(new MASConsoleLogFormatter());
                Logger.getLogger("").addHandler(h);
                Logger.getLogger("").setLevel(Level.INFO);
            }
        } else {
            // checks a local log configuration file
            if (new File(logPropFile).exists()) {
                try {
                    LogManager.getLogManager().readConfiguration(new FileInputStream(logPropFile));
                } catch (Exception e) {
                    System.err.println("Error setting up logger:" + e);
                }
            } else {
                try {
                    if (runner != null) {
                        LogManager.getLogManager().readConfiguration(this.getDefaultLogProperties());
                    } else {
                        LogManager.getLogManager().readConfiguration(RunCentralisedMAS.class.getResource("/templates/" + logPropFile).openStream());
                    }
                } catch (Exception e) {
                    System.err.println("Error setting up logger:" + e);
                    e.printStackTrace();
                }
            }
        }
    }

    protected InputStream getDefaultLogProperties() throws IOException {
        return RunCentralisedMAS.class.getResource("/templates/" + logPropFile).openStream();
    }

    protected void setupDefaultConsoleLogger() {
        Handler[] hs = Logger.getLogger("").getHandlers();
        for (int i = 0; i < hs.length; i++) {
            Logger.getLogger("").removeHandler(hs[i]);
        }
        Handler h = new ConsoleHandler();
        h.setFormatter(new MASConsoleLogFormatter());
        Logger.getLogger("").addHandler(h);
        Logger.getLogger("").setLevel(Level.INFO);
    }

    protected void createButtons() {
        this.createStopButton();

        // add Button pause
        this.createPauseButton();

        // add Button debug
        this.btDebug = new JButton("Debug", new ImageIcon(RunCentralisedMAS.class.getResource("/images/debug.gif")));
        this.btDebug.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                RunCentralisedMAS.this.changeToDebugMode();
                RunCentralisedMAS.this.btDebug.setEnabled(false);
                if (runner.control != null) {
                    try {
                        runner.control.getUserControl().setRunningCycle(false);
                    } catch (Exception e) {
                    }
                }
            }
        });
        if (debug) {
            this.btDebug.setEnabled(false);
        }
        MASConsoleGUI.get().addButton(this.btDebug);

        // add Button start
        final JButton btStartAg = new JButton("New agent", new ImageIcon(RunCentralisedMAS.class.getResource("/images/newAgent.gif")));
        btStartAg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                new StartNewAgentGUI(MASConsoleGUI.get().getFrame(), "Start a new agent to run in current MAS", System.getProperty("user.dir"));
            }
        });
        MASConsoleGUI.get().addButton(btStartAg);

        // add Button kill
        final JButton btKillAg = new JButton("Kill agent", new ImageIcon(RunCentralisedMAS.class.getResource("/images/killAgent.gif")));
        btKillAg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                new KillAgentGUI(MASConsoleGUI.get().getFrame(), "Kill an agent of the current MAS");
            }
        });
        MASConsoleGUI.get().addButton(btKillAg);

        this.createNewReplAgButton();

        // add show sources button
        final JButton btShowSrc = new JButton("Sources", new ImageIcon(RunCentralisedMAS.class.getResource("/images/list.gif")));
        btShowSrc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showProjectSources(project);
            }
        });
        MASConsoleGUI.get().addButton(btShowSrc);

    }

    protected void createPauseButton() {
        final JButton btPause = new JButton("Pause", new ImageIcon(RunCentralisedMAS.class.getResource("/images/resume_co.gif")));
        btPause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (MASConsoleGUI.get().isPause()) {
                    btPause.setText("Pause");
                    MASConsoleGUI.get().setPause(false);
                } else {
                    btPause.setText("Continue");
                    MASConsoleGUI.get().setPause(true);
                }

            }
        });
        MASConsoleGUI.get().addButton(btPause);
    }

    protected void createStopButton() {
        // add Button
        JButton btStop = new JButton("Stop", new ImageIcon(RunCentralisedMAS.class.getResource("/images/suspend.gif")));
        btStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                MASConsoleGUI.get().setPause(false);
                runner.finish();
            }
        });
        MASConsoleGUI.get().addButton(btStop);
    }

    protected void createNewReplAgButton() {
        // Do nothing
    }

    protected void createEnvironment() throws JasonException {
        if (project.getEnvClass() != null && !project.getEnvClass().getClassName().equals(jason.environment.Environment.class.getName())) {
            logger.fine("Creating environment " + project.getEnvClass());
            this.env = new CentralisedEnvironment(project.getEnvClass(), this);
        }
    }

    protected void createAgs() throws JasonException {

        RConf generalConf = RConf.fromString(project.getInfrastructure().getParameter(0));

        int nbAg = 0;
        Agent pag = null;

        // create the agents
        for (AgentParameters ap : project.getAgents()) {
            try {

                String agName = ap.name;

                for (int cAg = 0; cAg < ap.getNbInstances(); cAg++) {
                    AgentDefinition definition = this.loadDefinition(agName);

                    nbAg++;

                    String numberedAg = agName;
                    if (ap.getNbInstances() > 1) {
                        numberedAg += cAg + 1;
                        // cannot add zeros before, it causes many compatibility problems and breaks dynamic creation
                        // numberedAg += String.format("%0"+String.valueOf(ap.qty).length()+"d", cAg + 1);
                    }

                    String nb = "";
                    int n = 1;
                    while (this.getAg(numberedAg + nb) != null) {
                        nb = "_" + n++;
                    }
                    numberedAg += nb;

                    logger.fine("Creating agent " + numberedAg + " (" + (cAg + 1) + "/" + ap.getNbInstances() + ")");
                    CentralisedAgArch agArch;

                    RConf agentConf;
                    if (ap.getOption("rc") == null) {
                        agentConf = generalConf;
                    } else {
                        agentConf = RConf.fromString(ap.getOption("rc"));
                    }

                    // Get the number of reasoning cycles or number of cycles for each stage
                    int cycles = -1; // -1 means default value of the platform
                    int cyclesSense = -1;
                    int cyclesDeliberate = -1;
                    int cyclesAct = -1;

                    if (ap.getOption("cycles") != null) {
                        cycles = Integer.valueOf(ap.getOption("cycles"));
                    }
                    if (ap.getOption("cycles_sense") != null) {
                        cyclesSense = Integer.valueOf(ap.getOption("cycles_sense"));
                    }
                    if (ap.getOption("cycles_deliberate") != null) {
                        cyclesDeliberate = Integer.valueOf(ap.getOption("cycles_deliberate"));
                    }
                    if (ap.getOption("cycles_act") != null) {
                        cyclesAct = Integer.valueOf(ap.getOption("cycles_act"));
                    }

                    // Create agents according to the specific architecture
                    if (agentConf == RConf.POOL_SYNCH) {
                        agArch = new CentralisedAgArchForPool(definition.getSensor(), definition.getActuator());
                    } else if (agentConf == RConf.POOL_SYNCH_SCHEDULED) {
                        agArch = new CentralisedAgArchSynchronousScheduled(definition.getSensor(), definition.getActuator());
                    } else if (agentConf == RConf.ASYNCH || agentConf == RConf.ASYNCH_SHARED_POOLS) {
                        agArch = new CentralisedAgArchAsynchronous(definition.getSensor(), definition.getActuator());
                    } else {
                        agArch = new CentralisedAgArch(definition.getSensor(), definition.getActuator());
                    }

                    agArch.setCycles(cycles);
                    agArch.setCyclesSense(cyclesSense);
                    agArch.setCyclesDeliberate(cyclesDeliberate);
                    agArch.setCyclesAct(cyclesAct);

                    agArch.setConf(agentConf);
                    agArch.setAgName(numberedAg);
                    agArch.setEnvInfraTier(this.env);
                    if (generalConf != RConf.THREADED && cAg > 0 && ap.getAgArchClasses().isEmpty() && ap.getBBClass().equals(DefaultBeliefBase.class.getName())) {
                        // creation by cloning previous agent (which is faster -- no parsing, for instance)
                        agArch.createArchs(ap.getAgArchClasses(), pag, this);
                    } else {
                        // normal creation
                        agArch.createArchs(ap.getAgArchClasses(), ap.agClass.getClassName(), ap.getBBClass(), ap.asSource.toString(),
                                ap.getAsSetts(debug, project.getControlClass() != null), this);
                    }
                    this.addAg(agArch);

                    pag = agArch.getTS().getAg();
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error creating agent " + ap.name, e);
            }
        }

        if (generalConf != RConf.THREADED) {
            logger.info("Created " + nbAg + " agents.");
        }
    }

    private AgentDefinition loadDefinition(String agName) {
        try {
            Class<?> clazz = Class.forName(agName);
            Object object = clazz.newInstance();
            logger.log(Level.SEVERE, "Found agent " + agName + " definition!");

            if (!(object instanceof AgentDefinition)) {
                throw new InvalidActivityException();
            }
            AgentDefinition definition = (AgentDefinition) object;
            logger.log(Level.SEVERE, "Agent name is " + definition.getName());
            return definition;
        } catch (ClassNotFoundException e) {
            BaseCentralisedMAS.logger.log(Level.SEVERE, "Agent " + agName + " definition (" + agName + ".java) not found.");
        } catch (InvalidActivityException e) {
            logger.log(Level.SEVERE, "Agent " + agName + " definition must implement AgentDefinition");
        } catch (InstantiationException e) {
            BaseCentralisedMAS.logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            BaseCentralisedMAS.logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    protected void createController() throws JasonException {
        ClassParameters controlClass = project.getControlClass();
        if (debug && controlClass == null) {
            controlClass = new ClassParameters(ExecutionControlGUI.class.getName());
        }
        if (controlClass != null) {
            logger.fine("Creating controller " + controlClass);
            this.control = new CentralisedExecutionControl(controlClass, this);
        }
    }

    protected void startAgs() {
        // run the agents
        if (project.getInfrastructure().hasParameter("pool") || project.getInfrastructure().hasParameter("synch_scheduled") || project.getInfrastructure().hasParameter("asynch")
                || project.getInfrastructure().hasParameter("asynch_shared")) {
            this.createThreadPool();
        } else {
            this.createAgsThreads();
        }
    }

    /** creates one thread per agent */
    private void createAgsThreads() {

        int cyclesSense = 1;
        int cyclesDeliberate = 1;
        int cyclesAct = 5;

        if (project.getInfrastructure().hasParameters()) {
            if (project.getInfrastructure().getParametersArray().length > 2) {
                cyclesSense = Integer.parseInt(project.getInfrastructure().getParameter(1));
                cyclesDeliberate = Integer.parseInt(project.getInfrastructure().getParameter(2));
                cyclesAct = Integer.parseInt(project.getInfrastructure().getParameter(3));
            } else if (project.getInfrastructure().getParametersArray().length > 1) {
                cyclesSense = cyclesDeliberate = cyclesAct = Integer.parseInt(project.getInfrastructure().getParameter(1));
            }

            // logger.info("Creating a threaded agents." + "Cycles: " + cyclesSense + ", " + cyclesDeliberate + ", " + cyclesAct);
        }

        for (CentralisedAgArch ag : this.ags.values()) {
            ag.setControlInfraTier(this.control);

            // if the agent hasn't override the values for cycles, use the platform values
            if (ag.getCyclesSense() == -1) {
                ag.setCyclesSense(cyclesSense);
            }
            if (ag.getCyclesDeliberate() == -1) {
                ag.setCyclesDeliberate(cyclesDeliberate);
            }
            if (ag.getCyclesAct() == -1) {
                ag.setCyclesAct(cyclesAct);
            }

            // create the agent thread
            Thread agThread = new Thread(ag);
            ag.setThread(agThread);
        }

        // logger.info("Creating threaded agents. Cycles: " + agTemp.getCyclesSense() + ", " + agTemp.getCyclesDeliberate() + ", " + agTemp.getCyclesAct());

        for (CentralisedAgArch ag : this.ags.values()) {
            ag.startThread();
        }
    }

    private Set<CentralisedAgArch> sleepingAgs;

    private ExecutorService executor = null;

    private ExecutorService executorSense = null;
    private ExecutorService executorDeliberate = null;
    private ExecutorService executorAct = null;

    /** creates a pool of threads shared by all agents */
    private void createThreadPool() {
        this.sleepingAgs = Collections.synchronizedSet(new HashSet<CentralisedAgArch>());

        int maxthreads = 10;

        int maxthreadsSense = 1;
        int maxthreadsDeliberate = 1;
        int maxthreadsAct = 1;

        int cycles = 1;
        int cyclesSense = 1;
        int cyclesDeliberate = 1;
        int cyclesAct = 5;

        try {
            ClassParameters infra = project.getInfrastructure();
            RConf conf = RConf.fromString(infra.getParameter(0));
            if (conf == RConf.ASYNCH) {
                maxthreadsSense = Integer.parseInt(infra.getParameter(1));
                maxthreadsDeliberate = Integer.parseInt(infra.getParameter(2));
                maxthreadsAct = Integer.parseInt(infra.getParameter(3));
                if (infra.getParametersArray().length > 5) {
                    cyclesSense = Integer.parseInt(infra.getParameter(4));
                    cyclesDeliberate = Integer.parseInt(infra.getParameter(5));
                    cyclesAct = Integer.parseInt(infra.getParameter(6));
                }
                logger.info("Creating agents with asynchronous reasoning cycle. Sense (" + maxthreadsSense + "), Deliberate (" + maxthreadsDeliberate + "), Act (" + maxthreadsAct
                        + "). Cycles: " + cyclesSense + ", " + cyclesDeliberate + ", " + cyclesAct);

                this.executorSense = Executors.newFixedThreadPool(maxthreadsSense);
                this.executorDeliberate = Executors.newFixedThreadPool(maxthreadsDeliberate);
                this.executorAct = Executors.newFixedThreadPool(maxthreadsAct);

            } else { // async shared and pool cases
                if (infra.getParametersArray().length > 1) {
                    maxthreads = Integer.parseInt(infra.getParameter(1));
                }
                if (infra.getParametersArray().length > 4) {
                    cyclesSense = Integer.parseInt(infra.getParameter(2));
                    cyclesDeliberate = Integer.parseInt(infra.getParameter(3));
                    cyclesAct = Integer.parseInt(infra.getParameter(4));
                }

                if (conf == RConf.ASYNCH_SHARED_POOLS) {
                    logger.info("Creating agents with asynchronous reasoning cycle (shared). Sense, Deliberate, Act (" + maxthreads + "). Cycles: " + cyclesSense + ", "
                            + cyclesDeliberate + ", " + cyclesAct);
                    this.executorSense = this.executorDeliberate = this.executorAct = Executors.newFixedThreadPool(maxthreads);

                } else { // pool c ases
                    if (conf == RConf.POOL_SYNCH) {
                        // redefine cycles
                        if (infra.getParametersArray().length == 3) {
                            cycles = Integer.parseInt(infra.getParameter(2));
                        } else if (infra.getParametersArray().length == 6) {
                            cycles = Integer.parseInt(infra.getParameter(5));
                        } else {
                            cycles = 5;
                        }
                    } else if (infra.getParametersArray().length == 3) {
                        cyclesSense = cyclesDeliberate = cyclesAct = Integer.parseInt(infra.getParameter(2));
                    }

                    int poolSize = Math.min(maxthreads, this.ags.size());
                    logger.info("Creating a thread pool with " + poolSize + " thread(s). Cycles: " + cyclesSense + ", " + cyclesDeliberate + ", " + cyclesAct
                            + ". Reasoning Cycles: " + cycles);

                    // create the pool
                    this.executor = Executors.newFixedThreadPool(poolSize);
                }
            }
        } catch (Exception e) {
            logger.warning("Error getting the number of thread for the pool.");
        }

        // initially, add all agents in the tasks
        for (CentralisedAgArch ag : this.ags.values()) {
            if (ag.getCycles() == -1) {
                ag.setCycles(cycles);
            }
            if (ag.getCyclesSense() == -1) {
                ag.setCyclesSense(cyclesSense);
            }
            if (ag.getCyclesDeliberate() == -1) {
                ag.setCyclesDeliberate(cyclesDeliberate);
            }
            if (ag.getCyclesAct() == -1) {
                ag.setCyclesAct(cyclesAct);
            }

            if (this.executor != null) {
                if (ag instanceof CentralisedAgArchForPool) {
                    ((CentralisedAgArchForPool) ag).setExecutor(this.executor);
                }
                this.executor.execute(ag);
            } else if (ag instanceof CentralisedAgArchAsynchronous) {
                CentralisedAgArchAsynchronous ag2 = (CentralisedAgArchAsynchronous) ag;

                ag2.addListenerToC(new CircumstanceListenerComponents(ag2));

                ag2.setExecutorAct(this.executorAct);
                this.executorAct.execute(ag2.getActComponent());

                ag2.setExecutorDeliberate(this.executorDeliberate);
                this.executorDeliberate.execute(ag2.getDeliberateComponent());

                ag2.setExecutorSense(this.executorSense);
                this.executorSense.execute(ag2.getSenseComponent());
            }
        }
    }

    /** an agent architecture for the infra based on thread pool */
    protected final class CentralisedAgArchSynchronousScheduled extends CentralisedAgArch {
        public CentralisedAgArchSynchronousScheduled(Sensor sensor, Actuator actuator) {
            super(sensor, actuator);
        }

        private volatile boolean runWakeAfterTS = false;
        private int currentStep = 0;

        @Override
        public void sleep() {
            RunCentralisedMAS.this.sleepingAgs.add(this);
        }

        @Override
        public void wake() {
            if (RunCentralisedMAS.this.sleepingAgs.remove(this)) {
                RunCentralisedMAS.this.executor.execute(this);
            } else {
                this.runWakeAfterTS = true;
            }
        }

        @Override
        public void sense() {
            int number_cycles = this.getCyclesSense();
            int i = 0;

            while (this.isRunning() && i < number_cycles) {
                this.runWakeAfterTS = false;
                this.getTS().sense();
                if (this.getTS().canSleepSense()) {
                    if (this.runWakeAfterTS) {
                        this.wake();
                    }
                    break;
                }
                i++;
            }

            if (this.isRunning()) {
                RunCentralisedMAS.this.executor.execute(this);
            }
        }

        @Override
        public void deliberate() {
            super.deliberate();

            if (this.isRunning()) {
                RunCentralisedMAS.this.executor.execute(this);
            }
        }

        @Override
        public void act() {
            super.act();

            if (this.isRunning()) {
                RunCentralisedMAS.this.executor.execute(this);
            }
        }

        @Override
        public void run() {
            switch (this.currentStep) {
            case 0:
                this.sense();
                this.currentStep = 1;
                break;
            case 1:
                this.deliberate();
                this.currentStep = 2;
                break;
            case 2:
                this.act();
                this.currentStep = 0;
                break;
            }
        }
    }

    protected void stopAgs() {
        // stop the agents
        for (CentralisedAgArch ag : this.ags.values()) {
            ag.stopAg();
        }
    }

    /** change the current running MAS to debug mode */
    protected void changeToDebugMode() {
        try {
            if (this.control == null) {
                this.control = new CentralisedExecutionControl(new ClassParameters(ExecutionControlGUI.class.getName()), this);
                for (CentralisedAgArch ag : this.ags.values()) {
                    ag.setControlInfraTier(this.control);
                    Settings stts = ag.getTS().getSettings();
                    stts.setVerbose(2);
                    stts.setSync(true);
                    ag.getLogger().setLevel(Level.FINE);
                    ag.getTS().getLogger().setLevel(Level.FINE);
                    ag.getTS().getAg().getLogger().setLevel(Level.FINE);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error entering in debug mode", e);
        }
    }

    protected void startSyncMode() {
        if (this.control != null) {
            // start the execution, if it is controlled
            try {
                Thread.sleep(500); // gives a time to agents enter in wait
                this.control.informAllAgsToPerformCycle(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void waitEnd() {
        try {
            // wait a file called .stop___MAS to be created!
            File stop = new File(stopMASFileName);
            if (stop.exists()) {
                stop.delete();
            }
            while (!stop.exists()) {
                Thread.sleep(1500);
                /*
                 * boolean allSleep = true;
                 * for (CentralisedAgArch ag : ags.values()) {
                 * //System.out.println(ag.getAgName()+"="+ag.canSleep());
                 * allSleep = allSleep && ag.canSleep();
                 * }
                 * if (allSleep)
                 * break;
                 */
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Boolean runningFinish = false;

    @Override
    public void finish() {
        // avoid two threads running finish!
        synchronized (this.runningFinish) {
            if (this.runningFinish) {
                return;
            }
            this.runningFinish = true;
        }
        try {
            // creates a thread that guarantees system.exit(0) in 5 seconds
            // (the stop of agents can block)
            new Thread() {
                @Override
                public void run() {
                    try {
                        sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    System.exit(0);
                }
            }.start();

            System.out.flush();
            System.err.flush();

            if (MASConsoleGUI.hasConsole()) { // should close first! (case where console is in pause)
                MASConsoleGUI.get().close();
            }

            if (this.control != null) {
                this.control.stop();
                this.control = null;
            }
            if (this.env != null) {
                this.env.stop();
                this.env = null;
            }

            this.stopAgs();

            runner = null;

            // remove the .stop___MAS file (note that GUI console.close(), above, creates this file)
            File stop = new File(stopMASFileName);
            if (stop.exists()) {
                stop.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    /** show the sources of the project */
    private static void showProjectSources(MAS2JProject project) {
        JFrame frame = new JFrame("Project " + project.getSocName() + " sources");
        JTabbedPane pane = new JTabbedPane();
        frame.getContentPane().add(pane);
        project.fixAgentsSrc();

        for (AgentParameters ap : project.getAgents()) {
            try {
                String tmpAsSrc = ap.asSource.toString();

                // read sources
                InputStream in = null;
                if (tmpAsSrc.startsWith(SourcePath.CRPrefix)) {
                    in = RunCentralisedMAS.class.getResource(tmpAsSrc.substring(SourcePath.CRPrefix.length())).openStream();
                } else {
                    try {
                        in = new URL(tmpAsSrc).openStream();
                    } catch (MalformedURLException e) {
                        in = new FileInputStream(tmpAsSrc);
                    }
                }
                StringBuilder s = new StringBuilder();
                int c = in.read();
                while (c > 0) {
                    s.append((char) c);
                    c = in.read();
                }

                // show sources
                JTextArea ta = new JTextArea(40, 50);
                ta.setEditable(false);
                ta.setText(s.toString());
                ta.setCaretPosition(0);
                JScrollPane sp = new JScrollPane(ta);
                pane.add(ap.name, sp);
            } catch (Exception e) {
                logger.info("Error:" + e);
            }
        }
        frame.pack();
        frame.setVisible(true);
    }
}
