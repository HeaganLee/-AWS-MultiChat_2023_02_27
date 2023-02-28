package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JFrame;

public class ServerApplication {
	
	public static void main(String[] args) {
		JFrame serverFrame = new JFrame("서버");
		// 창이 닫히면 메모리에서 제거
		serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		serverFrame.setSize(300, 300);
		serverFrame.setVisible(true);
		
		try {
			ServerSocket serverSocket = new ServerSocket(9090);
			
			while(true) {
				Socket socket =  serverSocket.accept();
				ConnectedSocket connectedSocket = new ConnectedSocket(socket);
				connectedSocket.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
