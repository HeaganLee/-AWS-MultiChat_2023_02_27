package views;

import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.google.gson.Gson;

import dto.request.RequestDto;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ClientApplication extends JFrame {

	private static final long serialVersionUID = -4753767777928836759L;
	private static ClientApplication instance;
	
	private Gson gson;
	private Socket socket;
	
	private JPanel mainPanel;
	
	private CardLayout mainCard;
	
	private JTextField usernameFiled;
	
	private JTextField sendMessageField;
	
	private List<Map<String, String>> roomInfoList;
	private DefaultListModel<String> roomNameListModel;
	private DefaultListModel<String> usernameListModel;
	
	
	public static ClientApplication getInstance() {
		if(instance == null) {
			instance = new ClientApplication();
		}
		return instance;
	}

	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ClientApplication frame = ClientApplication.getInstance();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	
	private ClientApplication() {
		
		/*============<< init >>=============*/
		
		
		gson = new Gson();
		try {
			socket = new Socket("127.0.0.1", 9090);
			ClientRecive clientRecive = new ClientRecive(socket);
			clientRecive.start();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (ConnectException e1) {
			JOptionPane.showMessageDialog(this, "서버에 접속할 수 없습니다.", "접속오류", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		} catch (IOException e1) {
			e1.printStackTrace();
		} 
		
		
		
		/*============<< frame set >>=============*/

		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(600, 150, 480, 800);
		
	
		/*============<< panel >>=============*/
		
		
		mainPanel = new JPanel();
		JPanel loginPanel = new JPanel();
		JPanel rooListPanel = new JPanel();
		JPanel RoomPanel = new JPanel();
		
		
		
		/*============<< layout >>=============*/
		
		
		mainCard = new CardLayout();
		
		mainPanel.setLayout(mainCard);
		loginPanel.setLayout(null);
		rooListPanel.setLayout(null);
		RoomPanel.setLayout(null);
		
		
		
		/*============<< panel set >>=============*/
		
		
		setContentPane(mainPanel);
		mainPanel.add(loginPanel, "loginPanel");
		mainPanel.add(rooListPanel, "roomListPanel");
		mainPanel.add(RoomPanel, "RoomPanel");
		
		
		/*============<< login panel >>=============*/
		
		JButton enterButton = new JButton("접속하기");
		
		usernameFiled = new JTextField();
		usernameFiled.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					RequestDto<String> usernameChekReqDto = 
							new RequestDto<String>("usernameCheck", usernameFiled.getText());
					sendRequest(usernameChekReqDto);
				}
			}
		});
		
		
		usernameFiled.setBounds(27, 449, 398, 60);
		loginPanel.add(usernameFiled);
		usernameFiled.setColumns(10);
		
		enterButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				RequestDto<String> usernameChekReqDto = 
						new RequestDto<String>("usernameCheck", usernameFiled.getText());
				sendRequest(usernameChekReqDto);
			}
		});
		enterButton.setBounds(27, 514, 398, 60);
		loginPanel.add(enterButton);
		
		
		
		/*============<< roomList panel >>=============*/
		
		
		JScrollPane roomListScroll = new JScrollPane();
		roomListScroll.setBounds(113, 0, 343, 774);
		rooListPanel.add(roomListScroll);
		
		roomNameListModel = new DefaultListModel<String>();
		JList roomList = new JList(roomNameListModel);
		roomList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {
					int selectedIndex = roomList.getSelectedIndex();
					
					RequestDto<Map<String, String>> requestDto = 
							new RequestDto<Map<String,String>>("enterRoom", roomInfoList.get(selectedIndex));
					sendRequest(requestDto);
				}
			}
		});
		roomListScroll.setViewportView(roomList);
		
		JButton createRoomButton = new JButton("방생성");
		createRoomButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String roomName = null;
				while(true) {
				roomName = JOptionPane.showInputDialog(null, "생성할 방의 제목을 입력하세요", "방생성", JOptionPane.PLAIN_MESSAGE );
				if(!roomName.isBlank()) {
					break;
					}
					JOptionPane.showConfirmDialog(null, "공백은 사용할 수 없습니다.", "방생성 오류", JOptionPane.ERROR_MESSAGE);
				}
				RequestDto<String> requestDto = new RequestDto<String>("createRoom", roomName);
				sendRequest(requestDto);
			}
		});
		createRoomButton.setBounds(0, 10, 111, 109);
		rooListPanel.add(createRoomButton);
		
		
		
		/*============<< room panel >>=============*/
		
		
		JScrollPane joinUserLIstScroll = new JScrollPane();
		joinUserLIstScroll.setBounds(12, 10, 323, 110);
		RoomPanel.add(joinUserLIstScroll);
		
		usernameListModel = new DefaultListModel<String>();
		JList joinUserList = new JList(usernameListModel);
		joinUserLIstScroll.setViewportView(joinUserList);
		
		JButton exitButton = new JButton("나가기");
		exitButton.setBounds(336, 10, 120, 110);
		RoomPanel.add(exitButton);
		
		JScrollPane chattingContentScroll = new JScrollPane();
		chattingContentScroll.setBounds(12, 120, 444, 567);
		RoomPanel.add(chattingContentScroll);
		
		JTextArea chattingContent = new JTextArea();
		chattingContentScroll.setViewportView(chattingContent);
		
		sendMessageField = new JTextField();
		sendMessageField.setBounds(12, 687, 362, 66);
		RoomPanel.add(sendMessageField);
		sendMessageField.setColumns(10);
		
		JButton sendButton = new JButton("전송");
		sendButton.setBounds(375, 687, 81, 66);
		RoomPanel.add(sendButton);
	}
	
	private void sendRequest(RequestDto<?> requestDto) {
		String reqJson = gson.toJson(requestDto);
		OutputStream outputStream = null;
		PrintWriter printWriter = null;
		try {
			outputStream = socket.getOutputStream();
			printWriter = new PrintWriter(outputStream, true);
			printWriter.println(reqJson);
			System.out.println("클라이언트 -> 서버: " + reqJson);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}


	
}
