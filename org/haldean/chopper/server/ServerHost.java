package org.haldean.chopper.server;

import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class ServerHost extends JFrame {
    public final String heloName = new String("Horizon");
    public final DataReceiver r;
    public final WorldWindComponent lc;
    public final TiltComponent tc;
    public final ImageComponent ic;
    public final AccelerationComponent ac;
    public final Updatable status;
    public final UpdatableTextArea debug;
    public final SensorComponent sc;
    private final String hostPort[];

    private LinkedList<Component> leftTabPanes;
    private LinkedList<Component> rightTabPanes;

    public ServerHost(String s) {
	super();
	setTitle(heloName + " Control Server");

	Debug.log("Server exists in thread " + Thread.currentThread().getName());
	r = DataReceiver.getInstance();
	lc = new WorldWindComponent();
	tc = new TiltComponent();
	ic = new ImageComponent();
	ac = new AccelerationComponent();
	sc = new SensorComponent();
	status = new EchoUpdatable();
	debug = new UpdatableTextArea("Debug");

	Debug.setDebugOut(debug);

	String hostPortString;
	if (s == null)
	    hostPortString = JOptionPane.showInputDialog("Hostname and port of server machine");
	else
	    hostPortString = s;
	Debug.log("hostPortString is " + hostPortString);
	hostPort = hostPortString.split(":");
	r.initialize(hostPort[0], new Integer(hostPort[1]), new Integer(hostPort[1]) + 1);

	SensorParser sp = new SensorParser();
	sp.setWorldWindComponent(lc);
	sp.setTiltComponent(tc);
	sp.setAccelerationComponent(ac);
	sp.setSensorComponent(sc);

	r.tie(sp);
	r.tie(status);
	r.tieImage(ic);

	//r.tie(new EchoUpdatable());

	leftTabPanes = new LinkedList<Component>();
	rightTabPanes = new LinkedList<Component>();

	leftTabPanes.add(lc);
	rightTabPanes.add(ic);
	leftTabPanes.add(tc);
	rightTabPanes.add(ac);
	rightTabPanes.add(sc);
	leftTabPanes.add(debug);
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

    public void osInit() {
	if (System.getProperty("os.name").startsWith("Mac"))
            System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    public void start() {
	/* Update the Look and Feel of components created
	 * in the constructor */
	debug.updateUI();
	ac.updateUI();
	sc.updateUI();
	lc.updateUI();

	/* The right/left pane creator */
	JPanel horizontalPanel = new JPanel(new GridLayout(1,2));

	/* The two tab panes */
	JTabbedPane leftTabs = new JTabbedPane();
	JTabbedPane rightTabs = new JTabbedPane();

	for (int i=0; i<leftTabPanes.size(); i++) {
	    leftTabs.add(leftTabPanes.get(i));
	}

	for (int i=0; i<rightTabPanes.size(); i++) {
	    rightTabs.add(rightTabPanes.get(i));
	}

	horizontalPanel.add(leftTabs);
	add(horizontalPanel);

	/* The right pane */
	JPanel rawDataPanel = new JPanel(new BorderLayout());

	/* The title label*/
	JLabel titleLabel = new JLabel(heloName.toUpperCase() + " SERVER");
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

	/* Assemble right panel */
	rawDataPanel.add(titleLabel, BorderLayout.NORTH);
	rawDataPanel.add(rightTabs, BorderLayout.CENTER);
	rawDataPanel.add(statusPanel, BorderLayout.SOUTH);
	horizontalPanel.add(rawDataPanel);

	/* Show the frame */
	setPreferredSize(new Dimension(1100, 700));
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	pack();
	setVisible(true);
    }

    public static void main(String args[]) throws Exception {
	String serverURI = null;
	if (args.length > 0 && args[0].equals("debug"))
	    Debug.enable = true;
	else {
	    if (args.length > 1 && args[1].equals("debug")) {
		Debug.enable = true;
		serverURI = args[0];
	    } else
		Debug.enable = false;
	}

	final ServerHost s = new ServerHost(serverURI);
	s.osInit();

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