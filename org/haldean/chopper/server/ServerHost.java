package org.haldean.chopper.server;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class ServerHost extends JFrame {
    public final String heloName = new String("Chopper");
    public final DataReceiver r;
    public final LocationComponent lc;
    public final TiltComponent tc;
    public final ImageComponent ic;
    public final UpdatableTextArea status;
    private final String hostPort[];

    public ServerHost() {
	super();
	setTitle(heloName + " Control Server");

	Debug.log("Server exists in thread " + Thread.currentThread().getName());
	r = DataReceiver.getInstance();
	lc = new LocationComponent();
	tc = new TiltComponent();
	ic = new ImageComponent();
	status = new UpdatableTextArea();

	String hostPortString = JOptionPane.showInputDialog("Hostname and port of server machine");
	hostPort = hostPortString.split(":");
	r.initialize(hostPort[0], new Integer(hostPort[1]), new Integer(hostPort[1]) + 1);

	SensorParser sp = new SensorParser();
	sp.setLocationComponent(lc);
	sp.setTiltComponent(tc);

	r.tie(sp);
	r.tie(status);
	r.tieImage(ic);

	//r.tie(new EchoUpdatable());
    }

    public void accept() {
	(new Thread(r)).start();
    }

    private Font getFont(int size) {
	return getFont(size, false);
    }

    private Font getFont(int size, boolean bold) {
	if (bold)
	    return new Font("Helvetica", Font.BOLD, size);
	else 
	    return new Font("Helvetica", Font.PLAIN, size);
    }

    public void start() {
	/* Update the Look and Feel of components created
	 * in the constructor */
	status.updateUI();

	/* The right/left pane creator */
	JPanel horizontalPanel = new JPanel(new GridLayout(1,2));
	JTabbedPane leftTabs = new JTabbedPane();
	leftTabs.add("GPS", lc);
	leftTabs.add("Telemetry", ic);
	horizontalPanel.add(leftTabs);
	add(horizontalPanel);

	/* The right pane */
	JPanel rawDataPanel = new JPanel(new BorderLayout());

	/* The title label*/
	JLabel titleLabel = new JLabel(heloName.toUpperCase() + " CONTROL");
	titleLabel.setFont(getFont(24, true));
	titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

	/* The status bar */
	JPanel statusPanel = new JPanel(new FlowLayout());
	statusPanel.add(new JLabel(hostPort[0] + ":" + hostPort[1]));

	/* The disconnect button */
	final JButton disconnectButton = new JButton("Disconnect");
	statusPanel.add(disconnectButton);
	disconnectButton.addActionListener(new ActionListener() {
		private boolean connected = true;
		public void actionPerformed(ActionEvent e) {
		    if (connected) {
			r.stop();
			connected = false;
			disconnectButton.setText("Connect");
		    } else {
			(new Thread(r)).start();
			connected = true;
			disconnectButton.setText("Disconnect");
		    }
		}
	    });
	JButton quitButton = new JButton("Quit");
	statusPanel.add(quitButton);
	quitButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    Debug.log("Exiting gracefully.");
		    r.die();
		    try {
			while (r.connected())
			    Thread.sleep(200);
		    } catch (InterruptedException ex) {
			;
		    }
		    System.exit(0);
		}
	    });

	/* Assemble tab pane for right panel */
	JTabbedPane rightTabPane = new JTabbedPane(JTabbedPane.TOP);
	rightTabPane.add("Orientation", tc);
	rightTabPane.add("Raw Input", status.getComponent());

	/* Assemble right panel */
	rawDataPanel.add(titleLabel, BorderLayout.NORTH);
	rawDataPanel.add(rightTabPane, BorderLayout.CENTER);
	rawDataPanel.add(statusPanel, BorderLayout.SOUTH);
	horizontalPanel.add(rawDataPanel);

	/* Show the frame */
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	pack();
	setVisible(true);
    }

    public static void main(String args[]) throws Exception {
	if (args.length > 0 && args[0].equals("debug"))
	    Debug.enable = true;
	else
	    Debug.enable = false;

	final ServerHost s = new ServerHost();
      	/* Set Look and Feel */
	SwingUtilities.invokeAndWait(new Runnable() {
		public void run() {
		    try { 
			UIManager.setLookAndFeel("org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel");
		    } catch (Exception e) {
			System.err.println("Could not load LaF: " + e.toString());
		    }
		    /* Show the frame */
		    s.start();
		}
	    });

	/* Start the DataReceiver thread */
	s.accept();
    }
}