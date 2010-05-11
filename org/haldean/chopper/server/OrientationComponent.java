package org.haldean.chopper.server;

import javax.media.j3d.*;
import javax.vecmath.*;
import javax.swing.*;
import java.awt.*;
import com.sun.j3d.utils.universe.*; 
import com.sun.j3d.utils.geometry.*;

public class OrientationComponent extends JPanel {
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

    private BranchGroup createSceneGraph() {
	BranchGroup objectRoot = new BranchGroup();

	TransformGroup rotateGroup = new TransformGroup();
	rotateGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

	objectRoot.addChild(rotateGroup);
	rotateGroup.addChild(getChopperModel());

	Alpha rotationAlpha = new Alpha(-1, 4000);
	RotationInterpolator rotator = 
	    new RotationInterpolator(rotationAlpha, rotateGroup);

	/* The region in which the behavior is allowed to take place */
	BoundingSphere bounds = new BoundingSphere(new Point3d(0, 0, 0), 100);
	rotator.setSchedulingBounds(bounds);
	rotateGroup.addChild(rotator);

	/* Optimize! Enhance! */
	objectRoot.compile();
	return objectRoot;
    }

    private Node getChopperModel() {
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

	Transform3D sampleRotation = new Transform3D();
	sampleRotation.rotX(Math.PI / 4d);
	TransformGroup sampleGroup = new TransformGroup(sampleRotation);
	sampleGroup.addChild(node);

	return sampleGroup;
    }

    public static void main(String args[]) {
	JFrame frame = new JFrame();
	frame.setPreferredSize(new Dimension(300, 300));
	frame.add(new OrientationComponent());
	frame.pack();
	frame.setVisible(true);
    }
}