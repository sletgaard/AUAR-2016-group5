package com.mygdx.game;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class Handin3 extends ApplicationAdapter {
	
	public PerspectiveOffCenterCamera libGdxCam;
	Mat cameraMatrix;
	VideoCapture camera;
	int cameraResolutionX = 640;
	int cameraResolutionY = 480;
	
	public Environment environment;
    public ModelBatch modelBatch;
    public ModelInstance boxInstance;
    public Model boxModel;
	
	@Override
	public void create() {
		libGdxCam = new PerspectiveOffCenterCamera();
		cameraMatrix = UtilAR.getDefaultIntrinsics(640f, 480f);
		libGdxCam.setByIntrinsics(cameraMatrix, 640f, 480f);
		libGdxCam.position.set(5f, 5f, 5f);
		libGdxCam.lookAt(0,0,0);
		libGdxCam.update();
		
		camera = new VideoCapture(0);
		
		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
		
        modelBatch = new ModelBatch();
        
		ModelBuilder modelBuilder = new ModelBuilder();
        boxModel = modelBuilder.createBox(1f, 1f, 1f, 
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            Usage.Position | Usage.Normal);
        
        boxInstance = new ModelInstance(boxModel); 
		//boxInstance.transform.translate(0.5F, 0.5F, 0.5F);
	}
	
	@Override
	public void render() {
		Mat cameraImage = new Mat();
		camera.read(cameraImage);
		
		Mat greyImage = new Mat();
		Imgproc.cvtColor(cameraImage, greyImage, Imgproc.COLOR_BGR2GRAY);
		
		Mat binaryImage = new Mat(greyImage.size(), greyImage.type());
		Imgproc.threshold(greyImage, binaryImage, 100, 255, Imgproc.THRESH_BINARY);
		
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(binaryImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		
		ArrayList<MatOfPoint> ap;
		ArrayList<MatOfPoint> results = new ArrayList<MatOfPoint>();
		outerloop:
		for (int i=0; i<contours.size(); i++) {
			Point[] contour = contours.get(i).toArray();
			for (Point p : contour) {
				if (p.x <= 1 || p.y <=1 || p.x >= cameraResolutionX-2 || p.y >= cameraResolutionY-2) {
					continue outerloop;
				}
			}
			MatOfPoint2f curve = new MatOfPoint2f(contour);
			MatOfPoint2f approxCurve = new MatOfPoint2f();
			Imgproc.approxPolyDP(curve, approxCurve, Imgproc.arcLength(curve, true)*0.02, true);
			if (approxCurve.toList().size() == 4 && Imgproc.contourArea(approxCurve) > 2000) {
				
				 
				ap = new ArrayList<MatOfPoint>();
				ap.add(new MatOfPoint(approxCurve.toArray()));
				Imgproc.drawContours(cameraImage, ap, 0, new Scalar(0,0,255));
				results.add(new MatOfPoint(approxCurve.toArray()));
			}
		}
		
		Point3[] objPoints = new Point3[4];
		objPoints[0] = new Point3(-5,-5,0);
		objPoints[1] = new Point3(5,-5,0);
		objPoints[2] = new Point3(5,5,0);
		objPoints[3] = new Point3(-5,5,0);
		MatOfPoint3f objectPoints = new MatOfPoint3f(objPoints); // TODO: move to create()
		
		if (!results.isEmpty()) {
			MatOfPoint2f imagePoints = new MatOfPoint2f(results.get(0).toArray());
			//System.out.println(imagePoints.toList());
			
			Mat rvec = new Mat();
			Mat tvec = new Mat();
			Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix,
					UtilAR.getDefaultDistortionCoefficients(), rvec, tvec);
			
			UtilAR.setCameraByRT(rvec, tvec, libGdxCam);
			
			UtilAR.imDrawBackground(cameraImage);
			modelBatch.begin(libGdxCam);
			modelBatch.render(boxInstance, environment);
	        modelBatch.end();
		} else {
			UtilAR.imDrawBackground(cameraImage);
		}
		
	}

	@Override
	public void dispose() {
		camera.release();
		modelBatch.dispose();
		boxModel.dispose();
	}
}
