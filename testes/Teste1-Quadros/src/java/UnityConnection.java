import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class UnityConnection {

	public static String sendMessage(String sentence) throws IOException {
		Socket clientSocket = new Socket("127.0.0.1", 27000);

		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		outToServer.writeBytes(sentence + "\n");
		System.out.println("Sent to server: " + sentence);

		String response = inFromServer.readLine();
		System.out.println("Received from server:" + response);

		clientSocket.close();
		return response;
	}

}