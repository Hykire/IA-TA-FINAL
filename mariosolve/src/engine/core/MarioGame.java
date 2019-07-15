package engine.core;

import java.awt.image.VolatileImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.awt.*;
import java.awt.event.KeyAdapter;

import javax.swing.JFrame;

import agents.human.Agent;
import engine.helper.GameStatus;
import engine.helper.MarioActions;
import engine.sprites.*;

public class MarioGame {

    private Float yAnt = 0f;
    private Float xAnt = 0f;
    private Integer killByStomp  = 0;
    private Integer contSamePosition = 0;
    private Float varXPosicion = 0.7f;
    private Float varXEnmiesShort = 40.0f;
    private Float varXEnmiesMiddle = 80.0f;
    private Float varYEnmiesShort = 40.0f;
    private Float varYEnmiesMiddle = 80.0f;
    private boolean existEnemieShort = false;
    private boolean existEnemieMiddle = false;
    private boolean stuck = false;
    private boolean object1=false;
    private boolean object2=false;
    private boolean object3=false;
    private boolean object4=false;
    private boolean object5=false;
    private boolean object6=false;
	private int marioMode=0;	
    private String colisiono;

    Float y = 0f;
    Float x = 0f;
    /**
     * the maximum time that agent takes for each step
     */
    public static final long maxTime = 40;
    /**
     * extra time before reporting that the agent is taking more time that it should
     */
    public static final long graceTime = 10;
    /**
     * Screen width
     */
    public static final int width = 256;
    /**
     * Screen height
     */
    public static final int height = 256;
    /**
     * Screen width in tiles
     */
    public static final int tileWidth = width / 16;
    /**
     * Screen height in tiles
     */
    public static final int tileHeight = height / 16;
    /**
     * print debug details
     */
    public static final boolean verbose = false;

    /**
     * pauses the whole game at any moment
     */
    public boolean pause = false;

    /**
     * events that kills the player when it happens only care about type and param
     */
    private MarioEvent[] killEvents;

    //visualization
    private JFrame window = null;
    private MarioRender render = null;
    private MarioAgent agent = null;
    private MarioWorld world = null;

    /**
     * Create a mario game to be played
     */
    public MarioGame() {

    }

    /**
     * Create a mario game with a different forward model where the player on certain event
     *
     * @param killEvents events that will kill the player
     */
    public MarioGame(MarioEvent[] killEvents) {
        this.killEvents = killEvents;
    }

    private int getDelay(int fps) {
        if (fps <= 0) {
            return 0;
        }
        return 1000 / fps;
    }

    private void setAgent(MarioAgent agent) {
        this.agent = agent;
//        if (agent instanceof KeyAdapter) {
//            this.render.addKeyListener((KeyAdapter) this.agent);
//        }
    }

    /**
     * Play a certain mario level
     *
     * @param level a string that constitutes the mario level, it uses the same representation as the VGLC but with more details. for more details about each symbol check the json file in the levels folder.
     * @param timer number of ticks for that level to be played. Setting timer to anything <=0 will make the time infinite
     * @return statistics about the current game
     */
    public MarioResult playGame(String level, int timer) throws IOException {
        return this.runGame(new Agent(), level, timer, 0, true, 30, 2);
    }

    /**
     * Play a certain mario level
     *
     * @param level      a string that constitutes the mario level, it uses the same representation as the VGLC but with more details. for more details about each symbol check the json file in the levels folder.
     * @param timer      number of ticks for that level to be played. Setting timer to anything <=0 will make the time infinite
     * @param marioState the initial state that mario appears in. 0 small mario, 1 large mario, and 2 fire mario.
     * @return statistics about the current game
     */
    public MarioResult playGame(String level, int timer, int marioState) throws IOException {
        return this.runGame(new Agent(), level, timer, marioState, true, 30, 2);
    }

    /**
     * Play a certain mario level
     *
     * @param level      a string that constitutes the mario level, it uses the same representation as the VGLC but with more details. for more details about each symbol check the json file in the levels folder.
     * @param timer      number of ticks for that level to be played. Setting timer to anything <=0 will make the time infinite
     * @param marioState the initial state that mario appears in. 0 small mario, 1 large mario, and 2 fire mario.
     * @param fps        the number of frames per second that the update function is following
     * @return statistics about the current game
     */
    public MarioResult playGame(String level, int timer, int marioState, int fps) throws IOException {
        return this.runGame(new Agent(), level, timer, marioState, true, fps, 2);
    }

    /**
     * Play a certain mario level
     *
     * @param level      a string that constitutes the mario level, it uses the same representation as the VGLC but with more details. for more details about each symbol check the json file in the levels folder.
     * @param timer      number of ticks for that level to be played. Setting timer to anything <=0 will make the time infinite
     * @param marioState the initial state that mario appears in. 0 small mario, 1 large mario, and 2 fire mario.
     * @param fps        the number of frames per second that the update function is following
     * @param scale      the screen scale, that scale value is multiplied by the actual width and height
     * @return statistics about the current game
     */
    public MarioResult playGame(String level, int timer, int marioState, int fps, float scale) throws IOException {
        return this.runGame(new Agent(), level, timer, marioState, true, fps, scale);
    }

    /**
     * Run a certain mario level with a certain agent
     *
     * @param agent the current AI agent used to play the game
     * @param level a string that constitutes the mario level, it uses the same representation as the VGLC but with more details. for more details about each symbol check the json file in the levels folder.
     * @param timer number of ticks for that level to be played. Setting timer to anything <=0 will make the time infinite
     * @return statistics about the current game
     */
    public MarioResult runGame(MarioAgent agent, String level, int timer) throws IOException {
        return this.runGame(agent, level, timer, 0, false, 0, 2);
    }

    /**
     * Run a certain mario level with a certain agent
     *
     * @param agent      the current AI agent used to play the game
     * @param level      a string that constitutes the mario level, it uses the same representation as the VGLC but with more details. for more details about each symbol check the json file in the levels folder.
     * @param timer      number of ticks for that level to be played. Setting timer to anything <=0 will make the time infinite
     * @param marioState the initial state that mario appears in. 0 small mario, 1 large mario, and 2 fire mario.
     * @return statistics about the current game
     */
    public MarioResult runGame(MarioAgent agent, String level, int timer, int marioState) throws IOException {
        return this.runGame(agent, level, timer, marioState, false, 0, 2);
    }

    /**
     * Run a certain mario level with a certain agent
     *
     * @param agent      the current AI agent used to play the game
     * @param level      a string that constitutes the mario level, it uses the same representation as the VGLC but with more details. for more details about each symbol check the json file in the levels folder.
     * @param timer      number of ticks for that level to be played. Setting timer to anything <=0 will make the time infinite
     * @param marioState the initial state that mario appears in. 0 small mario, 1 large mario, and 2 fire mario.
     * @param visuals    show the game visuals if it is true and false otherwise
     * @return statistics about the current game
     */
    public MarioResult runGame(MarioAgent agent, String level, int timer, int marioState, boolean visuals) throws IOException {
        return this.runGame(agent, level, timer, marioState, visuals, visuals ? 30 : 0, 2);
    }

    /**
     * Run a certain mario level with a certain agent
     *
     * @param agent      the current AI agent used to play the game
     * @param level      a string that constitutes the mario level, it uses the same representation as the VGLC but with more details. for more details about each symbol check the json file in the levels folder.
     * @param timer      number of ticks for that level to be played. Setting timer to anything <=0 will make the time infinite
     * @param marioState the initial state that mario appears in. 0 small mario, 1 large mario, and 2 fire mario.
     * @param visuals    show the game visuals if it is true and false otherwise
     * @param fps        the number of frames per second that the update function is following
     * @return statistics about the current game
     */
    public MarioResult runGame(MarioAgent agent, String level, int timer, int marioState, boolean visuals, int fps) throws IOException {
        return this.runGame(agent, level, timer, marioState, visuals, fps, 2);
    }
    private void printResultsWorld(MarioResult result){
        float x = result.world.mario.x;
        float y = result.world.mario.y;
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j <15 ; j++) {

                if ((i ==8) && (j ==8))                  System.out.print("x");
                else if ((i ==8) && (j ==9))                  System.out.print("y");
                else if ((i ==7) && (j ==9))                  System.out.print("y");
                else if ((i ==6) && (j ==9))                  System.out.print("y");
                else if ((i ==5) && (j ==9))                  System.out.print("y");

                else System.out.print(result.world.getSceneObservation(x,y,0)[j][i]);
            }
            System.out.println();
        }
        System.out.println("===================================");
    }

    private String resultToForT(boolean val ){
        if (val) return "1";
        else return "0";
    }
    private void printResults(MarioResult result) {
//        x frame = 240

        this.x = result.world.mario.x;
        this.y = result.world.mario.y;
        this.existEnemieShort = false;
        this.existEnemieMiddle = false;
        this.object1 = false;
        this.object2 = false;
        this.object3 = false;
        this.object4 = false;
        this.object5 = false;
        this.object6 = false;

		marioMode = result.getMarioMode();

		GameStatus status = this.world.gameStatus;
		String gano = resultToForT(false);
		String perdio = resultToForT(false);
		if (status == GameStatus.WIN){
			gano = resultToForT(true);
		}else if (status == GameStatus.LOSE || status == GameStatus.TIME_OUT){
			perdio = resultToForT(true);
		}
//    Si Mario colisionó o no con un enemigo.
		String colisiono = resultToForT(false);
		if (Enemy.colisiono && Mario.invulnerableTime == Mario.default_invulnerable_time){
			colisiono = resultToForT(true);
		}else if (Mario.collide_on_small_mario){
			colisiono = resultToForT(true);
		}
		if (Enemy.colisiono){
			Enemy.colisiono = false;
		}
//    Si Mario está o no en el suelo.
        String suelo = resultToForT(yAnt.equals(y));
//    Si Mario está o no saltando.
        String saltando = resultToForT(!(yAnt.equals(y)));
//    Si hay enemigos hacia la derecha o izquierda a corto rango (3x4 con Mario en el centro). La columna donde se encuentra Mario se considera como derecha para priorizar ese movimiento.
        if (result.world.getEnemies().size() > 0 ) {
            for (int i = 0; i < result.world.getEnemies().size(); i++) {
                float xEnemie = result.world.getEnemies().get(i).x;
                float yEnemie = result.world.getEnemies().get(i).y;
                if (Math.abs(xEnemie-this.x)<varXEnmiesShort && Math.abs(yEnemie-this.y)<varYEnmiesShort) existEnemieShort=true;
//    Si hay enemigos hacia la derecha o izquierda a mediano rango (9x8 con Mario en el centro). La columna donde se encuentra Mario se considera como derecha para priorizar ese movimiento.
                else if (Math.abs(xEnemie-this.x)<varXEnmiesMiddle  && Math.abs(yEnemie-this.y)<varYEnmiesMiddle) existEnemieMiddle=true;
//                System.out.println(existEnemieShort +"  " + existEnemieMiddle);
            }
        }
        String enemigosCorta = resultToForT(existEnemieShort);
        String enemigosMedia = resultToForT(existEnemieMiddle);
        //    Si Mario se encuentra o no stuck. Esto se valida con los frames anteriores.
        if (Math.abs(result.world.mario.x - this.xAnt)< varXPosicion ) contSamePosition++;
        else contSamePosition = 0;
        if (contSamePosition > 10) this.stuck = true;
        else stuck = false;
        String atascado = resultToForT(this.stuck);

//    Dirección del movimiento de Mario. 9 direcciones posibles contando que esté quieto.
//        <-
        String d1= resultToForT((x< xAnt) && (y.equals(yAnt)));
//        <\
        String d2= resultToForT((x< xAnt) && (y>yAnt));
//        <|
        String d3= resultToForT((x.equals(xAnt)) && (y>yAnt));
//        />
        String d4= resultToForT((x>xAnt) && (y>yAnt));
//        ->
        String d5= resultToForT((x>xAnt) && (y.equals(yAnt)));
//        \>
        String d6= resultToForT((x>xAnt) && (y>yAnt));
//      |>
        String d7= resultToForT((x.equals(xAnt)) && (y<yAnt));
//      </
        String d8= resultToForT((x< xAnt) && (y<yAnt));

//    Si Mario mató o no al enemigo con un pisotón.
        String killEnemie = resultToForT(!(this.killByStomp == result.getKillsByStomp()));
//    Si hay obstáculos o no en los píxeles frente a engine.sprites.Mario con una distancia igual a 1.
        if (result.world.getSceneObservation(x,y,0)[9][8]!= 0) this.object1 = true;
        if (result.world.getSceneObservation(x,y,0)[9][7]!= 0) this.object2 = true;
        if (result.world.getSceneObservation(x,y,0)[9][6]!= 0) this.object3 = true;
        if (result.world.getSceneObservation(x,y,0)[9][5]!= 0) this.object4 = true;
        if (result.world.getSceneObservation(x,y,0)[9][4]!= 0) this.object5 = true;
        if (result.world.getSceneObservation(x,y,0)[9][3]!= 0) this.object6 = true;

        String objetoArriba= resultToForT(this.object1);
        String objetoMedio2= resultToForT(this.object2);
        String objetoMedio1= resultToForT(this.object3);
        String objetoAbajo= resultToForT(this.object4);
        String objetoAbajo1= resultToForT(this.object5);
        String objetoAbajo2= resultToForT(this.object6);

        System.out.println(gano+","+perdio+","+marioMode+","+suelo+","+saltando+","+colisiono+","+enemigosCorta+","+enemigosMedia+","+atascado+","+d1+","+d2+","+d3+","+d4+","+d5+","+d6+","+d7+","+d8+","+objetoArriba+","+objetoMedio2+","+objetoMedio1+","+objetoAbajo+","+objetoAbajo1+","+objetoAbajo2+","+killEnemie);
        this.yAnt = result.world.mario.y;
        this.xAnt = result.world.mario.x;
        this.killByStomp = result.getKillsByStomp();
    }

    /**
     * Run a certain mario level with a certain agent
     *
     * @param agent      the current AI agent used to play the game
     * @param level      a string that constitutes the mario level, it uses the same representation as the VGLC but with more details. for more details about each symbol check the json file in the levels folder.
     * @param timer      number of ticks for that level to be played. Setting timer to anything <=0 will make the time infinite
     * @param marioState the initial state that mario appears in. 0 small mario, 1 large mario, and 2 fire mario.
     * @param visuals    show the game visuals if it is true and false otherwise
     * @param fps        the number of frames per second that the update function is following
     * @param scale      the screen scale, that scale value is multiplied by the actual width and height
     * @return statistics about the current game
     */


    public MarioResult runGame(MarioAgent agent, String level, int timer, int marioState, boolean visuals, int fps, float scale) throws IOException {
        //System.out.println("entraRunGame");

        if (visuals) {
            this.window = new JFrame("Mario AI Framework");
            this.render = new MarioRender(scale);
            this.window.setContentPane(this.render);
            this.window.pack();
            this.window.setResizable(false);
            this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.render.init();
            this.window.setVisible(true);
        }
        this.setAgent(agent);
        return this.gameLoop(level, timer, marioState, visuals, fps);
    }

    private boolean[] stringToArrBoolean(String arr) {
        boolean[] booleans = new boolean[5];
        String[] actionsSplits = arr.split(",");
        for (int i = 0; i < 5; i++) {
            booleans[i] = actionsSplits[i].compareTo("t") == 0;
        }
//        System.out.println(actionsSplits[0]+""+actionsSplits[1]+""+actionsSplits[2]+""+actionsSplits[3]+""+actionsSplits[4]);
        return booleans;
    }

    private MarioResult gameLoop(String level, int timer, int marioState, boolean visual, int fps) throws IOException {
        this.world = new MarioWorld(this.killEvents);
        this.world.visuals = visual;
        this.world.initializeLevel(level, 1000 * timer);
        if (visual) {
            this.world.initializeVisuals(this.render.getGraphicsConfiguration());
        }
        this.world.mario.isLarge = marioState > 0;
        this.world.mario.isFire = marioState > 1;
        this.world.update(new boolean[MarioActions.numberOfActions()]);
        long currentTime = System.currentTimeMillis();

        //initialize graphics
        VolatileImage renderTarget = null;
        Graphics backBuffer = null;
        Graphics currentBuffer = null;
        if (visual) {
            renderTarget = this.render.createVolatileImage(MarioGame.width, MarioGame.height);
            backBuffer = this.render.getGraphics();
            currentBuffer = renderTarget.getGraphics();
            this.render.addFocusListener(this.render);
        }

        MarioTimer agentTimer = new MarioTimer(MarioGame.maxTime);
        this.agent.initialize(new MarioForwardModel(this.world.clone()), agentTimer);

        ArrayList<MarioEvent> gameEvents = new ArrayList<>();
        ArrayList<MarioAgentEvent> agentEvents = new ArrayList<>();
        Integer i = 0;

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String actionRecive = "f,f,f,f,f";
        boolean[] actions = new boolean[4];
        while (this.world.gameStatus == GameStatus.RUNNING) {
            if (!this.pause) {
                //get actions
//                actionRecive = "f,f,f,f,f";
                actionRecive = reader.readLine();
                actions = stringToArrBoolean(actionRecive);
//                agentTimer = new MarioTimer(MarioGame.maxTime);

//                actions = this.agent.getActions(new MarioForwardModel(this.world.clone()), agentTimer);
//                boolean[] actions = new boolean[] {false,false,false,false,true};


//                if( i < 10 ){
//                     actions = new boolean[]{false, false, false, false, false};
//                }
//                i++;
//                System.out.println(actions[0]+","+actions[1]+","+actions[2]+","+actions[3]+","+actions[4]);


                if (MarioGame.verbose) {
                    if (agentTimer.getRemainingTime() < 0 && Math.abs(agentTimer.getRemainingTime()) > MarioGame.graceTime) {
                        //System.out.println("The Agent is slowing down the game by: "
                        //        + Math.abs(agentTimer.getRemainingTime()) + " msec.");
                    }
                }

                // update world
                this.world.update(actions);
                gameEvents.addAll(this.world.lastFrameEvents);
                agentEvents.add(new MarioAgentEvent(actions, this.world.mario.x,
                        this.world.mario.y, (this.world.mario.isLarge ? 1 : 0) + (this.world.mario.isFire ? 1 : 0),
                        this.world.mario.onGround, this.world.currentTick));
            }

            //render world
            if (visual) {
                this.render.renderWorld(this.world, renderTarget, backBuffer, currentBuffer);
            }
            //check if delay needed
            if (this.getDelay(fps) > 0) {
                try {
                    currentTime += this.getDelay(fps);
                    Thread.sleep(Math.max(0, currentTime - System.currentTimeMillis()));
                } catch (InterruptedException e) {
                    break;
                }
            }

//            System.out.println(actionRecive);
            printResults(new MarioResult(this.world, gameEvents, agentEvents));

        }
        System.exit(1);
        return new MarioResult(this.world, gameEvents, agentEvents);
    }

}
