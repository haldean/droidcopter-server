package org.haldean.chopper.server;

import net.java.games.input.*;
import javax.swing.*;

public class PadController implements Runnable {
    private ServerHost ui;
    private Controller ctrl;
    private StatusLabel sl;

    private Component buttons[];
    private final int BUTTON_A = 0;
    private final int BUTTON_B = 1;
    private final int BUTTON_X = 2;
    private final int BUTTON_Y = 3;
    private final int BUTTON_L = 4;
    private final int BUTTON_R = 5;
    private final int BUTTON_START = 6;
    private final int BUTTON_XBOX = 7;
    private final int BUTTON_BACK = 8;
    private final int JOYSTICK_L = 9;
    private final int JOYSTICK_R = 10;
    private final int BUTTON_COUNT = 11;

    private Component axes[];
    private float lastAxisValue[];
    private final int AXIS_L_H = 0;
    private final int AXIS_L_V = 1;
    private final int AXIS_L_TRIGGER = 2;
    private final int AXIS_R_H = 3;
    private final int AXIS_R_V = 4;
    private final int AXIS_R_TRIGGER = 5;
    private final int D_PAD = 6;
    private final int AXIS_COUNT = 7;

    private int lastButtonMask = 0;

    private boolean globeMovement = false;

    public PadController(ServerHost _ui, StatusLabel _sl) {
	ui = _ui;
	sl = _sl;
	
	ControllerEnvironment env = ControllerEnvironment.getDefaultEnvironment();
	Controller controllers[] = env.getControllers();

	if (controllers.length == 0) {
	    Debug.log("No controllers found");
	    return;
	} else {
	    Debug.log("Found " + controllers.length + " controllers");
	}

	ctrl = null;
	for (Controller c : controllers)
	    if (c.getType() == Controller.Type.GAMEPAD)
		ctrl = c;

	if (ctrl == null) 
	    return;

	Component components[] = ctrl.getComponents();
	Debug.log("Using game pad with " + components.length + " components");

	buttons = new Component[BUTTON_COUNT];
	axes = new Component[AXIS_COUNT];
	lastAxisValue = new float[AXIS_COUNT];

	for (Component c : components) {
	    if (!c.isAnalog() && !c.isRelative()) {
		Component.Identifier id = c.getIdentifier();
		if (id == Component.Identifier.Button.A)
		    buttons[BUTTON_A] = c;
		else if (id == Component.Identifier.Button.B)
		    buttons[BUTTON_B] = c;
		else if (id == Component.Identifier.Button.X)
		    buttons[BUTTON_X] = c;
		else if (id == Component.Identifier.Button.Y)
		    buttons[BUTTON_Y] = c;
		else if (id == Component.Identifier.Button.BACK)
		    buttons[BUTTON_BACK] = c;
		else if (id == Component.Identifier.Button.LEFT_THUMB)
		    buttons[BUTTON_L] = c;
		else if (id == Component.Identifier.Button.RIGHT_THUMB)
		    buttons[BUTTON_R] = c;
		else if (id == Component.Identifier.Button.UNKNOWN)
		    buttons[BUTTON_START] = c;
		else if (id == Component.Identifier.Button.MODE)
		    buttons[BUTTON_XBOX] = c;
		else if (id == Component.Identifier.Button.LEFT_THUMB3)
		    buttons[JOYSTICK_L] = c;
		else if (id == Component.Identifier.Button.RIGHT_THUMB3)
		    buttons[JOYSTICK_R] = c;
	    } else {
		Component.Identifier id = c.getIdentifier();
		if (id == Component.Identifier.Axis.X)
		    axes[AXIS_L_H] = c;
		else if (id == Component.Identifier.Axis.Y)
		    axes[AXIS_L_V] = c;
		else if (id == Component.Identifier.Axis.Z)
		    axes[AXIS_L_TRIGGER] = c;
		else if (id == Component.Identifier.Axis.RX)
		    axes[AXIS_R_H] = c;
		else if (id == Component.Identifier.Axis.RY)
		    axes[AXIS_R_V] = c;
		else if (id == Component.Identifier.Axis.RZ)
		    axes[AXIS_R_TRIGGER] = c;
		else if (id == Component.Identifier.Axis.POV)
		    axes[D_PAD] = c;
	    }
	}
    }

    private int buttonMask() {
	int mask = 0;
	for (int i=0; i<buttons.length; i++)
	    if (buttons[i] != null && buttons[i].getPollData() == 1)
		mask += (1 << i);
	return mask;
    }

    private boolean buttonIsSet(int mask, int button) {
	return ((mask >> button) & 1) == 1;
    }

    private void buttonAction(int mask) {
	int currentMask = buttonMask();

	/* Left button moves to the tab to the left.
	 * Holding A moves left tab pane, else right tab pane */
	if (buttonIsSet(mask, BUTTON_L)) {
	    JTabbedPane p;
	    if (buttonIsSet(currentMask, BUTTON_A)) 
		p = ui.leftTabs;
	    else
		p = ui.rightTabs;

	    int tabIndex = p.getSelectedIndex() - 1;
	    if (tabIndex < 0)
		tabIndex = p.getTabCount() - 1;
	    p.setSelectedIndex(tabIndex);
	}

	/* Right button moves to the tab to the right */
	if (buttonIsSet(mask, BUTTON_R)) {
	    JTabbedPane p;
	    if (buttonIsSet(currentMask, BUTTON_A)) 
		p = ui.leftTabs;
	    else
		p = ui.rightTabs;

	    int tabIndex = p.getSelectedIndex() + 1;
	    if (tabIndex >= p.getTabCount())
		tabIndex = 0;
	    p.setSelectedIndex(tabIndex);
	}

	/* Toggle Globe Movement state */
	if (buttonIsSet(mask, BUTTON_BACK)) {
	    globeMovement = ! globeMovement;
	    sl.setGlobeMode(globeMovement);
	    Debug.log("Globe control is now " + ((globeMovement) ? "on" : "off"));
	}

	/* In Globe Mode, B activates or disactivates follow mode */
	if (globeMovement && buttonIsSet(mask, BUTTON_B))
	    ui.lc.toggleFollow();
    }

    private float getAxis(int axis) {
	if (Math.abs(axes[axis].getPollData()) < 0.1)
	    return 0;
	else
	    return axes[axis].getPollData();
    }

    private void updateLastAxis() {
	for (int i=0; i<AXIS_COUNT; i++)
	    if (axes[i] != null)
		lastAxisValue[i] = axes[i].getPollData();
    }

    private void axesAction() {
	if (globeMovement) {
	    float zoom = getAxis(AXIS_L_TRIGGER) - getAxis(AXIS_R_TRIGGER);
	    ui.lc.moveView(getAxis(AXIS_L_H), getAxis(AXIS_L_V), zoom, getAxis(AXIS_R_V), getAxis(AXIS_R_H));
	}

	updateLastAxis();
    }

    public void run() {
	while (ctrl != null) {
	    ctrl.poll();
	    axesAction();
	    int mask = buttonMask();
	    int newButtons = (mask ^ lastButtonMask) & mask;
	    if (newButtons != 0) {
		buttonAction(newButtons);
	    }

	    lastButtonMask = mask;

	    try {
		Thread.sleep(10);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }
}