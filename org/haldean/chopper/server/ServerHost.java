package org.haldean.chopper.server;

import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/** The superclass! This is the frame that encompasses everything else. */
public class ServerHost extends JFrame {
    /* The chopper name. We've been changing it enough that
     * it's just easier to have it be an easily-changable string */
    public final String heloName = new String("Horizon");

    /* All sorts of components */
    public final DataReceiver r;
    public final WorldWindComponent lc;
    public final TiltComponent tc;
    public final ImageComponent ic;
    public final AccelerationComponent ac;
    public final Updatable status;
    public final UpdatableTextArea debug;
    public final SensorComponent sc;

    /* The string of the form hostname:port representing
     * the server to connect to for data. */
    private final String hostPort[];

    /* The components to put in the left and right tab
     * panes in the UI */
    private LinkedList<Component> leftTabPanes;
    private LinkedList<Component> rightTabPanes;

    /** Create a new ServerHost
     *  @param s The server address and port in the form hostname:port. If passed null,
     *           it will automatically show a JOptionPane to ask for one */
    public ServerHost(String s) {
	super();
	/* Set the title of the JFrame */
	setTitle(heloName + " Control Server");

	Debug.log("Server exists in thread " + Thread.currentThread().getName());

	/* Create all the necessary components so we can feed them
	 * into each other */
	r = DataReceiver.getInstance();
	lc = new WorldWindComponent();
	tc = new TiltComponent();
	ic = new ImageComponent();
	ac = new AccelerationComponent();
	sc = new SensorComponent();
	status = new EchoUpdatable();
	debug = new UpdatableTextArea("Debug");

	/* Sets the output for all the glorious error messages */
	Debug.setDebugOut(debug);

	/* If a hostPortString wasn't passed in, ask for one */
	String hostPortString;
	if (s == null)
	    hostPortString = JOptionPane.showInputDialog("Hostname and port of server machine");
	else
	    hostPortString = s;
	Debug.log("hostPortString is " + hostPortString);

	/* Split it on the colon and initialize the DataReceiver. The image port is
	 * assumed to be one greater than the data port */
	hostPort = hostPortString.split(":");
	r.initialize(hostPort[0], new Integer(hostPort[1]), new Integer(hostPort[1]) + 1);

	/* Create the sensor parser and tell it where to
	 * find all of the appropriate components */
	SensorParser sp = new SensorParser();
	sp.setWorldWindComponent(lc);
	sp.setTiltComponent(tc);
	sp.setAccelerationComponent(ac);
	sp.setSensorComponent(sc);

	/* Tie the updatables to the DataReceiver */
	r.tie(sp);
	r.tie(status);
	r.tieImage(ic);

	leftTabPanes = new LinkedList<Component>();
	rightTabPanes = new LinkedList<Component>();

	/* The left tab pane has the globe, the tilt and the debug feed */
	leftTabPanes.add(lc);
	leftTabPanes.add(tc);
	leftTabPanes.add(debug);

	/* The right has the telemetry, the acceleration and the sensor data */
	rightTabPanes.add(ic);
	rightTabPanes.add(ac);
	rightTabPanes.add(sc);
    }

    /** Start accepting data */
    public void accept() {
	/* Start the DataReceiver thread */
	(new Thread(r)).start();
    }

    /** Get the UI font
     *  @param size The size in pixels */
    private Font getFont(int size) {
	return getFont(size, false);
    }

    /** Get the UI font
     *  @param size The size in pixels
     *  @param bold True if bold font is desired, false if not */
    private Font getFont(int size, boolean bold) {
	return new Font("Helvetica", (bold) ? Font.BOLD : Font.PLAIN, size);
    }

    /** Initialize operating system specific stuff */
    public void osInit() {
	if (System.getProperty("os.name").startsWith("Mac"))
            System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    /** Start the ServerHost thread */
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

	/* Add all of the stuff on the left to the tabbed pane */
	for (int i=0; i<leftTabPanes.size(); i++) {
	    leftTabs.add(leftTabPanes.get(i));
	}

	/* Do the same for the right */
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
		    /* If connected, stop the DataReceiver and switch the 
		     * text of the button. If not connected, restart the
		     * DataReceiver thread */
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

	/* The quit button */
	JButton quitButton = new JButton("Quit");
	statusPanel.add(quitButton);
	quitButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    /* Tell the DataReceiver to suck it and sleep until
		     * it does */
		    r.die();
		    try {
			while (r.connected())
			    Thread.sleep(200);
		    } catch (InterruptedException ex) {
			;
		    }
		    /* Exit without error */
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

    /** Run the chopper host */
    public static void main(String args[]) throws Exception {
	/* Parse command line arguments */
	String serverURI = null;
	if (args.length > 0 && args[0].equals("debug"))
	    Debug.enable = true;
	else if (args.length == 1) {
	    serverURI = args[0];
	} else {
	    if (args.length > 1 && args[1].equals("debug")) {
		Debug.enable = true;
		serverURI = args[0];
	    } else
		Debug.enable = false;
	}

	/* Initialize the server host */
	final ServerHost s = new ServerHost(serverURI);
	s.osInit();

      	/* Set Look and Feel */
	SwingUtilities.invokeAndWait(new Runnable() {
		public void run() {
		    try { 
			UIManager.setLookAndFeel("org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel");
		    } catch (Exception e) {
			Debug.log("Could not load LaF: " + e.toString());
		    }
		    /* Show the frame */
		    s.start();
		}
	    });

	/* Start the DataReceiver thread */
	s.accept();
    }
}