package main;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import dto.request.RequestDto;
import dto.response.ResponseDto;
import entity.Room;
import lombok.Getter;

@Getter
public class ConnectedSocket extends Thread{
	
	
	private static List<ConnectedSocket> connectedSocketList = new ArrayList<>();
	private static List<Room> roomList = new ArrayList<>();
	private static int index = 0;
	private Socket socket;
	private String username;
	
	private Gson gson;
	
	public ConnectedSocket(Socket socket) {
		this.socket = socket;
		gson = new Gson();
	}
	
	@Override
	public void run() {
		BufferedReader bufferedReader;
		try {
			while(true) { 
				bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String requestJson = bufferedReader.readLine();
				
				System.out.println("요청: " + requestJson);
				requestMapping(requestJson);
				}
			}catch (SocketException e) {
				connectedSocketList.remove(this);
				System.out.println(username + ": 클라이언트 종료");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	
	
	private void requestMapping(String requestJson) {
		RequestDto<?> requestDto = gson.fromJson(requestJson, RequestDto.class);
		
		Room room = null;
		
		
		switch(requestDto.getResource()) {
			case "usernameCheck":
				checkUsername((String)requestDto.getBody());
				break;
			case "createRoom":
				room = new Room((String) requestDto.getBody(), username);
				room.getUsers().add(this);
				roomList.add(room);
				sendToMe(new ResponseDto<String>("createdRoomSuccessfully", null));
				refreshUsernameList(room);
				sendToAll(refleshRommList(), connectedSocketList);
				break;
			case "enterRoom":
				room = findRoom((Map<String, String>) requestDto.getBody());
				room.getUsers().add(this);
				sendToMe(new ResponseDto<String>("enterRoomSuccessfully", null));
				refreshUsernameList(room);
				break;
			case "sendMessage":
				room = findConnectedRoom(username);
				sendToAll(new ResponseDto<String>("reciveMessage", username + ">>>" + (String) requestDto.getBody()), room.getUsers());
				break;
				
			case "exitRoom":
				room = findConnectedRoom(username);
				try {
					if(room.getOwner().equals(username)) {
						exitRoomAll(room);
					}else {
						exitRoom(room);
					}
				} catch (NullPointerException e) {
					System.out.println("클라이언트 강제 종료됨");
					
				}
				break;
				
				
		}
	}
	
	private void checkUsername(String username) {
		if(username.isBlank()) {
			sendToMe(new ResponseDto<String>("usernameCheckIsBlank", "사용자 이름은 공백일 수 없습니다."));
			return;
		}
		for(ConnectedSocket connectedSocket : connectedSocketList) {
			if(connectedSocket.getUsername().equals(username)) {
				sendToMe(new ResponseDto<String>("usernameCheckIsDuplicate", "이미 사용중인 이름입니다."));
				return;
				
			}
		}
		this.username = username;
		connectedSocketList.add(this);
		sendToMe(new ResponseDto<String>("usernameCheckSuccessfully", null));
		sendToMe(refleshRommList());
	}
	
	private ResponseDto<List<Map<String, String>>> refleshRommList() {
		List<Map<String, String>> roomNameList = new ArrayList<>();
		
		for(Room room : roomList) {
			Map<String, String> roomInfo = new HashMap<>();
			roomInfo.put("roomName", room.getRoomName());
			roomInfo.put("owner", room.getOwner());
			roomNameList.add(roomInfo);
		}
		ResponseDto<List<Map<String, String>>> responseDto = new ResponseDto<List<Map<String, String>>>("refreshRoomList", roomNameList);
		return responseDto;
	}
	
	private Room findConnectedRoom(String username) {
		Room room = null;
		for(Room r : roomList) {
			
			for(ConnectedSocket cs : r.getUsers()) {
				if(cs.getUsername().equals(username)) {
					return r;
				}
			}
		}
		return null;
	}
	
	private Room findRoom(Map<String, String> roomInfo) {
		for(Room room : roomList) {
			if(room.getRoomName().equals(roomInfo.get("roomName")) 
					&& room.getOwner().equals(roomInfo.get("owner"))) {
				return room;
			}
		}
		return null;
	}
	
	private void refreshUsernameList(Room room){
		List<String> usernameList = new ArrayList<>();
		usernameList.add("방제목: " + room.getRoomName());
		for (ConnectedSocket connectedSocket : room.getUsers()) {
			if(connectedSocket.getUsername().equals(room.getOwner())) {
				usernameList.add(connectedSocket.getUsername() + "(방장)");
				continue;
			}
			usernameList.add(connectedSocket.getUsername());
		}
		ResponseDto<List<String>> responseDto = new ResponseDto<List<String>>("refreshUsernameList", usernameList);
		sendToAll(responseDto, room.getUsers());
	}
	
	private void exitRoomAll(Room room) {
		sendToAll(new ResponseDto<String>("exitRoom", null), room.getUsers());
		roomList.remove(room);
		sendToAll(refleshRommList(), connectedSocketList);
	}
	
	
	private void exitRoom(Room room) {
		room.getUsers().remove(this);
		sendToMe(new ResponseDto<String>("exitRoom", null));
		refreshUsernameList(room);
	}
	
	private void sendToMe(ResponseDto<?> responseDto) {
		
		try {
			OutputStream outputStream = socket.getOutputStream();
			PrintWriter printWriter = new PrintWriter(outputStream, true);
			
			String responesJson = gson.toJson(responseDto);
			printWriter.println(responesJson);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void sendToAll(ResponseDto<?> responseDto, List<ConnectedSocket> connectedSockets) {
		for(ConnectedSocket connectedSocket : connectedSockets) {
			try {
				OutputStream outputStream = connectedSocket.getSocket().getOutputStream();
				PrintWriter printWriter = new PrintWriter(outputStream, true);
				
				String responesJson = gson.toJson(responseDto);
				printWriter.println(responesJson);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		
	}

}
