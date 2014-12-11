 /*
  * Author: Azadi Bogolubov
  */
 
package com.tutorazadi.cs594project;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

public class Client extends JFrame
{
	// Variable declarations
	private static final long serialVersionUID = 1L;
	private Socket socket;
    private String currentChannel = "";
    private BufferedReader in;

    // Swing UI components
    private JButton sendBtn;
	private JTextArea messages;
	private JTextField sendTxt;
	private JLabel currentChannelLbl;
    private JScrollPane scrollBar;

    /**
     *  Default constructor
     *  Initializes the GUI and starts the event loop.
     */
	public Client()
	{
		try
		{
			//socket = new Socket("54.148.215.231", 5000);
			socket = new Socket("localhost", 5000);  
			try 
			{
				intializeGUI();

				currentChannel = "COMMON";
		    	in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  

				Thread t1 = new Thread(new Runnable() 
				{
				     public void run() 
				     {
				    	 try 
				    	 {
					    	 while (true)
					    	 {
					    		 getData();
					    	 }
				    	 }
				    	 // Client can gracefully handle server crashes (2 pts)
				    	 catch (NullPointerException e)
				    	 {
				    		 messages.append("Disconnecting.\n");
				    		 sendTxt.setEnabled(false);
				    		 sendBtn.setEnabled(false);
				    	 }
				     }
				});  
				t1.start();			
			}
			catch (Exception e) { e.printStackTrace(); }
		}
		catch (IOException e) { e.printStackTrace(); }
	}

	/** Initializes the GUI window.
	 * @param sendBtn The button used to send messages to the server.
	 * @param messages The message window where messages returned from the server are displayed.
	 * @param scrollBar The vertical scrollbar on the messages window.
	 * @param sendTxt The text area where messages are entered by the client.
	 * @param currentChannelLbl A label displaying the channel the client is currently in.
	 */
	public void intializeGUI()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("IRC Client");
		setLayout(null);
		setSize(500, 300);
		sendBtn = new JButton("Send");
		sendBtn.setSize(100, 20);
		sendBtn.setLocation(190, 240);
		sendBtn.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e)
			{
				sendData();
			}
		});
		add(sendBtn);

		messages = new JTextArea(20, 20);
		messages.setSize(480, 200);
		messages.setLocation(10,10);
		messages.setEnabled(false);

		scrollBar = new JScrollPane(messages);
        scrollBar.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollBar.setLocation(10, 10);
        scrollBar.setSize(480, 200);
        scrollBar.setVisible(true);
        
		add(scrollBar);
		
		sendTxt = new JTextField();
		sendTxt.setSize(180, 20);
		sendTxt.setLocation(10, 240);
		add(sendTxt);

		currentChannelLbl = new JLabel("Current channel: COMMON");
		currentChannelLbl.setSize(200, 20);
		currentChannelLbl.setLocation(10, 210);
		add(currentChannelLbl);
		
		// Show the GUI window.
		setVisible(true);
	}

	/** 
	 * Sends the message entered in the send message text area to the server for processing.
	 * Regular expressions are used to validate messages for correct format. 
	 * If a message is not in the correct format, it will be sent as a general message to the current channel. 
	 * @param out A print writer which will send the message stream to the server.
	 * @param msg The message being sent to the server.
	 */
	public void sendData()
    {
    	try
    	{
    		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);     
        	String msg;
        	if (sendTxt.getText().matches("/((\\bSWITCH\\b)|(\\bJOIN\\b)|(\\bPART\\b)|(\\bMEMBERS\\b))([ A-Z0-9])*"))
	        	msg = MessageHandler.handleChannelMessage(sendTxt.getText());
	        else if (sendTxt.getText().matches("/((\\bQUIT\\b)|(\\bNICK\\b)|(\\bLIST\\b))([ ]([ A-Za-z0-9])*)*"))
	        	msg = MessageHandler.handleSystemMessage(sendTxt.getText());
	        else if (sendTxt.getText().matches("/(\\bMULT\\b)[ ]([\\[\\],A-Z0-9])*[ ].*"))
	        {
	        	msg = MessageHandler.handleMultiChannelMessage(sendTxt.getText());
	        	System.out.println(msg);
	        }
	        else if (sendTxt.getText().matches("/(\\bMSG\\b)[ #A-Za-z0-9]*"))
	        	msg = MessageHandler.handleP2PMessage(sendTxt.getText());
	        else if (sendTxt.getText().contains("DAMN"))
	        	msg = MessageHandler.kick();
	        else if (sendTxt.getText().matches("/(\\bHELP\\b)"))
	        {
	        	JOptionPane.showMessageDialog(null,	showValidOptions());
	        	msg = "0;;;" + "Help requested";
	        }
	        else
	        	msg = MessageHandler.handleSingleChannelMessage(currentChannel + ";" + sendTxt.getText());
    		out.println(msg);
    		sendTxt.setText("");
    	}
    	catch (IOException e) { JOptionPane.showMessageDialog(null, "Bad command. Please enter /HELP for a list of valid commands."); }
    }
    
	/**
	 * A function which shows all valid options for the client to use when communicating with the server.
	 * These options are invoked using the /HELP message.
	 * @return A list of valid options to be shown to the user in a JOptionPane.
	 * @param options The string containing the list of options to be displayed to the user.
	 */
    private String showValidOptions()
    {
    	String options = "Valid options:\n";
    	options += "Set nickname: /NICK <nickname>\n";
    	options += "Join a channel: /JOIN <channelname>\n";
    	options += "Private message: /MSG <nickname>\n";
    	options += "List all channels: /LIST\n";
    	options += "Send message to multiple channels: /MULT [<channelName>, ..., <channelname>] someMessage\n";
    	options += "Leave a channel: /PART <channelname>\n";
    	options += "Send a message to the current channel: someMessage\n";
    	options += "Quit IRC: /QUIT\n";
    	return options;
    }

    /**
     * This method receives the data from the server and processes it on the client side.
     * 1) If the response starts with newMsg, then we know that a new message has been received.
     * The client will check if the message is for the current channel, and if so, will update the message window.
     * Otherwise, nothing is displayed.
     * 2) If the message starts with currentChannel, then we are entering a new channel. The current channel label will be updated, 
     * as well as the current channel we are in. Messages from the new channel will be loaded to the message window as well.
     * @param response The message received from the server.
     */
    public void getData()
    {
    	try
    	{
			String response = in.readLine();
			// Get the broadcastCode...
			if (response.startsWith("newMsg="))
			{
				if (response.substring(7).equals(currentChannel))
				{
					messages.setText("");
					PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
					out.println(MessageHandler.handleChannelMessage("/SWITCH " + currentChannel));
				}
    		}
			else if (response.startsWith("currentChannel="))
			{
				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
				currentChannel = response.substring(15);
				currentChannelLbl.setText("Current channel: " + currentChannel);
				out.println(MessageHandler.handleChannelMessage("/SWITCH " + currentChannel));
			}
			else
				messages.append(response + '\n');
    	}
    	catch (IOException e) { e.printStackTrace(); }
    }
    
    /**
     * The main method. Since we are dealing with sockets, there is always a chance of running into an IOException, 
     * so a throws declaration has been added for safety.
     * @param args
     * @throws IOException
     */
	public static void main(String[] args) throws IOException 
	{
		new Client();
	}
}  