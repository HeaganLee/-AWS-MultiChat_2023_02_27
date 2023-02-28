package entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import main.ConnectedSocket;

@Getter
public class Room {
	private String roomName;
	private String owner;
	private List<ConnectedSocket> users;
	
	public Room(String roomName, String owner) {
		this.roomName = roomName;
		this.owner = owner;
		users = new ArrayList<>();
	}
	
	public List<String> getUsernameList(){
		List<String> usernameList = new ArrayList<>();
		for(ConnectedSocket connectedSocket : users) {
			usernameList.add(connectedSocket.getUsername());
		}
		return usernameList;
	}
}
