import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import engine.core.MarioGame;
import engine.core.MarioResult;

public class PlayLevel {
    public static void printResults(MarioResult result) {
        System.out.println("****************************************************************");
        System.out.println("Game Status: " + result.getGameStatus().toString() +
                " Percentage Completion: " + result.getCompletionPercentage());
        System.out.println("Lives: " + result.getCurrentLives() + " Coins: " + result.getCurrentCoins() +
                " Remaining Time: " + (int) Math.ceil(result.getRemainingTime() / 1000f));
        System.out.println("Mario State: " + result.getMarioMode() +
                " (Mushrooms: " + result.getNumCollectedMushrooms() + " Fire Flowers: " + result.getNumCollectedFireflower() + ")");
        System.out.println("Total Kills: " + result.getKillsTotal() + " (Stomps: " + result.getKillsByStomp() +
                " Fireballs: " + result.getKillsByFire() + " Shells: " + result.getKillsByShell() +
                " Falls: " + result.getKillsByFall() + ")");
        System.out.println("Bricks: " + result.getNumDestroyedBricks() + " Jumps: " + result.getNumJumps() +
                " Max X Jump: " + result.getMaxXJump() + " Max Air Time: " + result.getMaxJumpAirTime());
        System.out.println("****************************************************************");
    }

    private static String getLevel(String filepath) throws IOException {
        String content = "";
        content = new String(Files.readAllBytes(Paths.get(filepath)));
        return content;
    }

    public static void main(String[] args) throws IOException {


//		BufferedReader reader =
//				new BufferedReader(new InputStreamReader(System.in));
//		String name = null;
//		while(true) {
//            name = reader.readLine();
//            System.out.println(name);
//        }

        MarioGame game = new MarioGame();
        printResults(game.playGame(getLevel("levels/original/lvl-1.txt"), 200, 0));
//	 	printResults(game.runGame(new agents.robinBaumgarten.Agent(), getLevel("levels/original/lvl-15.txt"), 200, 0, true));
    }
}
