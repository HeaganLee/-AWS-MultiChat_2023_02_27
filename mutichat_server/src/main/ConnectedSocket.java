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
	
	// 현재 접속한 소켓
	private static List<ConnectedSocket> connectedSocketList = new ArrayList<>();
	// 현재 만들어진 방
	private static List<Room> roomList = new ArrayList<>();
	private Socket socket;
	private String username;
	
	private Gson gson;
	
	public ConnectedSocket(Socket socket) {
		this.socket = socket;
		gson = new Gson();
	}
	
	// 소켓을 받을 동안 계속해서 돌고 있으며
	// 소켓 연결이 끊기면 소켓 리스트에서 삭제를 시킨다.
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
	
	// 클라이언트가 보낸 요청사항을 확인하고 처리하는 메소드 입니다.
	private void requestMapping(String requestJson) {
		RequestDto<?> requestDto = gson.fromJson(requestJson, RequestDto.class);
		
		Room room = null;
		
		// switch문을 통해 클라이언트가 보낸 메세지를 requestDto를 통해서 확인을 하고 
		// 요청에 맞는 작업을 수행합니다.
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
	
	// 로그인 요청을 보낸 상황에서 유저네임이 동일한것이 있는지 확인합니다.
	// 확인 후 sendToMe를 통해서 다시 처리합니다.
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
	
	// 룸 리스트를 갱신하는 메소드 입니다.
	private ResponseDto<List<Map<String, String>>> refleshRommList() {
		List<Map<String, String>> roomNameList = new ArrayList<>();
		
		// 룸이 만들어짐과 동시에 룸 객체에 방장정보와 룸의 이름을 넣습니다.
		for(Room room : roomList) {
			Map<String, String> roomInfo = new HashMap<>();
			roomInfo.put("roomName", room.getRoomName());
			roomInfo.put("owner", room.getOwner());
			roomNameList.add(roomInfo);
		}
		ResponseDto<List<Map<String, String>>> responseDto = new ResponseDto<List<Map<String, String>>>("refreshRoomList", roomNameList);
		return responseDto;
	}
	
	// 현재 방에 접속된 유저가 누구인지 찾아주는 메소드 입니다.
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
	
	// 현재 생성된 룸의 이름을 찾게 해주는 메소드입니다.
	private Room findRoom(Map<String, String> roomInfo) {
		for(Room room : roomList) {
			if(room.getRoomName().equals(roomInfo.get("roomName")) 
					&& room.getOwner().equals(roomInfo.get("owner"))) {
				return room;
			}
		}
		return null;
	}
	
	// 방에 접속되어 있는 유저의 리스트를 갱신을 해주는 메소드 입니다.
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
	
	// 만약 방장이 나가면 해당 방 유저에게 방을 나가게 해주는 메소드 입니다.
	private void exitRoomAll(Room room) {
		sendToAll(new ResponseDto<String>("exitRoom", null), room.getUsers());
		roomList.remove(room);
		sendToAll(refleshRommList(), connectedSocketList);
	}
	
	// 만약 방장이 아닌 일반 유저가 나갈시에 실행되는 메소드 입니다.
	private void exitRoom(Room room) {
		room.getUsers().remove(this);
		sendToMe(new ResponseDto<String>("exitRoom", null));
		refreshUsernameList(room);
	}
	
	// 클라이언트가 요청을 한 후에 해당 클라이언트에 응답을 해주는 메소드입니다.
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
	
	// 클라이언트가 요청 후 모든 클라이언트에게 정보를 보내는 용도로 사용되는 메소드입니다.
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
