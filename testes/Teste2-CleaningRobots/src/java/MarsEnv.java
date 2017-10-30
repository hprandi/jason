import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;

import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

public class MarsEnv extends Environment {

	public static final int GSize = 7; // grid size
	public static final int GARB = 16; // garbage code in grid model

	public static final Term ns = Literal.parseLiteral("next(slot)");
	public static final Term pg = Literal.parseLiteral("pick(garb)");
	public static final Term dg = Literal.parseLiteral("drop(garb)");
	public static final Term bg = Literal.parseLiteral("burn(garb)");
	public static final Literal g1 = Literal.parseLiteral("garbage(r1)");
	public static final Literal g2 = Literal.parseLiteral("garbage(r2)");

	static Logger logger = Logger.getLogger(MarsEnv.class.getName());

	private MarsModel model;
	private MarsView view;

	@Override
	public void init(String[] args) {
		this.model = new MarsModel();
		this.view = new MarsView(this.model);
		this.model.setView(this.view);
		this.updatePercepts();
		MarsEnv.instance = this;
	}

	private static MarsEnv instance;

	public static MarsEnv getInstance() {
		return instance;
	}

	@Override
	public boolean executeAction(String ag, Structure action) {
		logger.info(ag + " doing: " + action);
		try {
			if (action.equals(ns)) {
				this.model.nextSlot();
			} else if (action.getFunctor().equals("move_towards")) {
				int x = (int) ((NumberTerm) action.getTerm(0)).solve();
				int y = (int) ((NumberTerm) action.getTerm(1)).solve();
				this.model.moveTowards(x, y);
			} else if (action.equals(pg)) {
				this.model.pickGarb();
			} else if (action.equals(dg)) {
				this.model.dropGarb();
			} else if (action.equals(bg)) {
				this.model.burnGarb();
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.updatePercepts();

		try {
			Thread.sleep(200);
		} catch (Exception e) {
		}
		this.informAgsEnvironmentChanged();
		return true;
	}

	/** creates the agents perception based on the MarsModel */
	void updatePercepts() {
		this.clearPercepts();

		Location r1Loc = this.model.getAgPos(0);
		Location r2Loc = this.model.getAgPos(1);

		Literal pos1 = Literal.parseLiteral("pos(r1," + r1Loc.x + "," + r1Loc.y + ")");
		Literal pos2 = Literal.parseLiteral("pos(r2," + r2Loc.x + "," + r2Loc.y + ")");

		this.addPercept(pos1);
		this.addPercept(pos2);

		if (this.model.hasObject(GARB, r1Loc)) {
			this.addPercept(g1);
		}
		if (this.model.hasObject(GARB, r2Loc)) {
			this.addPercept(g2);
		}
	}

	class MarsModel extends GridWorldModel {

		public static final int MErr = 2; // max error in pick garb
		int nerr; // number of tries of pick garb
		boolean r1HasGarb = false; // whether r1 is carrying garbage or not

		Random random = new Random(System.currentTimeMillis());

		private MarsModel() {
			super(GSize, GSize, 2);

			// initial location of agents
			try {
				this.setAgPos(0, 0, 0);

				Location r2Loc = new Location(GSize / 2, GSize / 2);
				this.setAgPos(1, r2Loc);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// initial location of garbage
			this.add(GARB, 3, 0);
			this.add(GARB, GSize - 1, 0);
			this.add(GARB, 1, 2);
			this.add(GARB, 0, GSize - 2);
			this.add(GARB, GSize - 1, GSize - 1);
		}

		void nextSlot() throws Exception {
			Location r1 = this.getAgPos(0);
			r1.x++;
			if (r1.x == this.getWidth()) {
				r1.x = 0;
				r1.y++;
			}
			// finished searching the whole grid
			if (r1.y == this.getHeight()) {
				return;
			}
			this.setAgPos(0, r1);
			this.setAgPos(1, this.getAgPos(1)); // just to draw it in the view
		}

		void moveTowards(int x, int y) throws Exception {
			Location r1 = this.getAgPos(0);
			if (r1.x < x) {
				r1.x++;
			} else if (r1.x > x) {
				r1.x--;
			}
			if (r1.y < y) {
				r1.y++;
			} else if (r1.y > y) {
				r1.y--;
			}
			this.setAgPos(0, r1);
			this.setAgPos(1, this.getAgPos(1)); // just to draw it in the view
		}

		void pickGarb() {
			// r1 location has garbage
			if (MarsEnv.this.model.hasObject(GARB, this.getAgPos(0))) {
				// sometimes the "picking" action doesn't work
				// but never more than MErr times
				if (this.random.nextBoolean() || this.nerr == MErr) {
					this.remove(GARB, this.getAgPos(0));
					this.nerr = 0;
					this.r1HasGarb = true;
				} else {
					this.nerr++;
				}
			}
		}

		void dropGarb() {
			if (this.r1HasGarb) {
				this.r1HasGarb = false;
				this.add(GARB, this.getAgPos(0));
			}
		}

		void burnGarb() {
			// r2 location has garbage
			if (MarsEnv.this.model.hasObject(GARB, this.getAgPos(1))) {
				this.remove(GARB, this.getAgPos(1));
			}
		}
	}

	class MarsView extends GridWorldView {

		public MarsView(MarsModel model) {
			super(model, "Mars World", 600);
			this.defaultFont = new Font("Arial", Font.BOLD, 18); // change default font
			this.setVisible(true);
			this.repaint();
		}

		/** draw application objects */
		@Override
		public void draw(Graphics g, int x, int y, int object) {
			switch (object) {
			case MarsEnv.GARB:
				this.drawGarb(g, x, y);
				break;
			}
		}

		@Override
		public void drawAgent(Graphics g, int x, int y, Color c, int id) {
			String label = "R" + (id + 1);
			c = Color.blue;
			if (id == 0) {
				c = Color.yellow;
				if (((MarsModel) this.model).r1HasGarb) {
					label += " - G";
					c = Color.orange;
				}
			}
			super.drawAgent(g, x, y, c, -1);
			if (id == 0) {
				g.setColor(Color.black);
			} else {
				g.setColor(Color.white);
			}
			super.drawString(g, x, y, this.defaultFont, label);
			this.repaint();
		}

		public void drawGarb(Graphics g, int x, int y) {
			super.drawObstacle(g, x, y);
			g.setColor(Color.white);
			this.drawString(g, x, y, this.defaultFont, "G");
		}

	}
}
