package org.haldean.chopper.server;

import javax.media.j3d.*;
import javax.vecmath.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.sun.j3d.utils.universe.*; 
import com.sun.j3d.utils.geometry.*;
import java.util.*;

public class OrientationComponent extends JPanel {
    TransformGroup chopperRotator;
    SetAngleBehavior angleBehavior;

    public OrientationComponent() {
	super(new GridLayout(1,1));
	GraphicsConfiguration config = 
	    SimpleUniverse.getPreferredConfiguration();
	Canvas3D c3d = new Canvas3D(config);
	add(c3d);

	SimpleUniverse u = new SimpleUniverse(c3d);
	u.getViewingPlatform().setNominalViewingTransform();

	u.addBranchGraph(createSceneGraph());
    }

    public String getName() {
	return "Orientation";
    }

    private BranchGroup createSceneGraph() {
	BranchGroup objectRoot = new BranchGroup();

	TransformGroup chopperModel = getChopperModel();

	/* The region in which the behavior is allowed to take place */
	BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), 100);

	angleBehavior = new SetAngleBehavior(chopperModel);
	angleBehavior.setSchedulingBounds(bounds);

	TransformGroup rotateGroup = new TransformGroup();
	rotateGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

	Alpha rotationAlpha = new Alpha(-1, 8000);
	RotationInterpolator rotator = 
	    new RotationInterpolator(rotationAlpha, rotateGroup);

	rotator.setSchedulingBounds(bounds);

	objectRoot.addChild(rotateGroup);
	rotateGroup.addChild(chopperModel);
	rotateGroup.addChild(angleBehavior);
	rotateGroup.addChild(rotator);

	/* Optimize! Enhance! */
	objectRoot.compile();
	return objectRoot;
    }

    private TransformGroup getChopperModel() {
	BranchGroup node = new BranchGroup();

	Appearance metal = new Appearance();
	metal.setColoringAttributes(new ColoringAttributes(0.5f, 0.5f, 0.5f,
						       ColoringAttributes.FASTEST));
	Appearance red = new Appearance();
	red.setColoringAttributes(new ColoringAttributes(1.0f, 0f, 0f,
						       ColoringAttributes.FASTEST));

	/* The horizontal plane. Name ambiguity between Swing and Java3D. */
	com.sun.j3d.utils.geometry.Box plane = 
	    new com.sun.j3d.utils.geometry.Box(0.5f, 0.01f, 0.5f, metal);

	/* The downwards-pointing vector */
	Cylinder cyl = new Cylinder(0.005f, 0.25f, red);
	
	Transform3D translate = new Transform3D();
	translate.set(new Vector3f(0f, -0.125f, 0f));
	TransformGroup cylTransform = new TransformGroup(translate);
	    
	node.addChild(plane);
	cylTransform.addChild(cyl);
	node.addChild(cylTransform);

	chopperRotator = new TransformGroup();
	chopperRotator.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

	chopperRotator.addChild(node);
	return chopperRotator;
    }

    public void setOrientation(Orientation o) {
	angleBehavior.setAngle(o);
	angleBehavior.processStimulus(null);
    }

    public class SetAngleBehavior extends Behavior {
        private TransformGroup targetTG;
        private Transform3D rotationX = new Transform3D();
	private Transform3D rotationZ = new Transform3D();
	private Orientation angle;

	public SetAngleBehavior(TransformGroup _targetTG) {
	    targetTG = _targetTG;
	    angle = new Orientation(0, 0, 0);
	}

	public void initialize(){
	    ;
	}

	public void processStimulus(Enumeration criteria){
	    rotationX.rotX(angle.getTilt(Orientation.RADIANS));
	    rotationZ.rotZ(angle.getPitch(Orientation.RADIANS));
	    rotationX.mul(rotationZ);
	    targetTG.setTransform(rotationX);
	}

	public void setAngle(Orientation _angle) {
	    angle = _angle;
	}
    }

    public static void main(String args[]) {
	Debug.enable = true;
	JFrame frame = new JFrame();
	frame.setPreferredSize(new Dimension(300, 300));
	OrientationComponent o = new OrientationComponent();
	frame.add(o);
	frame.pack();
	frame.setVisible(true);

	try{
	    Thread.sleep(1000);
	} catch (Exception e) { ; }
	o.setOrientation(new Orientation(0, 45, 45));
    }
}