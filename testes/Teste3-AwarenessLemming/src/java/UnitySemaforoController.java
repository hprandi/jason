

import java.io.IOException;

public class UnitySemaforoController {
	public static void main(String[] args) {
		try {
			String sendMessage = UnityConnection.sendMessage("3");
			System.out.println(sendMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}