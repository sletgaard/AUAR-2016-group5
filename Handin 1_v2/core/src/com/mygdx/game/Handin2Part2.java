package com.mygdx.game;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
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

public class Handin2Part2 extends ApplicationAdapter {
	
	public PerspectiveOffCenterCamera libGdxCam;
	public Environment environment;
    public ModelBatch modelBatch;
    public ModelInstance boxInstances[];
	public Model boxModel;
	Mat cameraMatrix;
	
	VideoCapture camera;
	MatOfPoint3f chessboard3dPos;
	
	boolean isCalibrated = false;
	List<Mat> calibObjectPoints;
	List<Mat> calibImagePoints;
	private Size calibChessboardSize = new Size(9,6);
	private int calibCounter = 0;
	private MatOfDouble distCoeffs;
	private int frameCounter;
	
	
	@Override
	public void create() {
		calibObjectPoints = new ArrayList<Mat>();
		calibImagePoints = new ArrayList<Mat>();
		
		distCoeffs = UtilAR.getDefaultDistortionCoefficients();
		
		libGdxCam = new PerspectiveOffCenterCamera();
		cameraMatrix = UtilAR.getDefaultIntrinsics(640f, 480f);
		libGdxCam.setByIntrinsics(cameraMatrix, 640f, 480f);
		libGdxCam.update();
		
		Point3[] positions = new Point3[54];
		for (int i=0; i<6; i++) {
			for (int j=0; j<9; j++) {
				positions[i*9+j] = new Point3(i+1, j+1, 0f);
			}
		}
		
		camera = new VideoCapture(0);
		chessboard3dPos = new MatOfPoint3f(positions);
		

		environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
		
        modelBatch = new ModelBatch();
        
		ModelBuilder modelBuilder = new ModelBuilder();
        boxModel = modelBuilder.createBox(1f, 1f, 1f, 
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            Usage.Position | Usage.Normal);
		
        boxInstances = new ModelInstance[35];
        int boxCounter = 0;
        for (int i=0; i<10; i++) {
        	for (int j=0; (i%2==0 && j<7) || (i%2==1 && j<6); j=j+2) {
        		int k = j;
        		if (i%2 == 1) {
        			k++;
        		}
        		ModelInstance instance = new ModelInstance(boxModel); 
        		instance.transform.translate(k+0.5F, i+0.5F, 0.5F);
	        	boxInstances[boxCounter] = instance;
	        	boxCounter++;
        	}
        }
        System.out.println(boxCounter);
        
        
	}
	
	@Override
	public void render() {
		Mat cameraImage = new Mat();
		camera.read(cameraImage);
		
		Mat greyImage = new Mat();
		Imgproc.cvtColor(cameraImage, greyImage, Imgproc.COLOR_BGR2GRAY);
		
		MatOfPoint2f corners = new MatOfPoint2f();
		corners.alloc(54);
		boolean found = Calib3d.findChessboardCorners(greyImage, new Size(9, 6),
				corners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_FAST_CHECK);
		
		frameCounter++;
		
		if (!isCalibrated && found) {
			// Tilføj vores hjørner til kaliberingslisterne
			calibImagePoints.add(corners);
			MatOfPoint3f objects = new MatOfPoint3f();
			objects.alloc(54);
			
			/*Point3[] positions = new Point3[54];
			for (int i=0; i<9; i++) {
				for (int j=0; j<6; j++) {
					positions[i*6+j] = new Point3(j+1, i+1, 0f);
				}
			}*/
			calibObjectPoints.add(new MatOfPoint3f(chessboard3dPos));
			
			System.out.println("Sleeping now " + calibCounter);
			try { Thread.sleep(3000); } catch (Exception e) {}
			calibCounter++;
			if (calibCounter > 5) {
				cameraMatrix = new Mat(3,3,CvType.CV_64F);
				//cameraMatrix.put(0, 0, 1);
				//cameraMatrix.put(1, 1, 1);
				distCoeffs = new MatOfDouble();
				List<Mat> rvecs = new ArrayList<Mat>();
				List<Mat> tvecs = new ArrayList<Mat>();
				
				// Calibrate
				Calib3d.calibrateCamera(calibObjectPoints, calibImagePoints, cameraImage.size(),
						cameraMatrix, distCoeffs, rvecs, tvecs);
				
				// Set camera
				libGdxCam.setByIntrinsics(cameraMatrix, 640f, 480f);
				libGdxCam.update();
				isCalibrated = true;
			}

		}
		
		if (found && isCalibrated) {
			//Calib3d.drawChessboardCorners(cameraImage, new Size(9,6), corners, true);
			
			Mat rvec = new Mat();
			Mat tvec = new Mat();
			Calib3d.solvePnP(chessboard3dPos, corners, cameraMatrix,
					distCoeffs, rvec, tvec);
			
			UtilAR.setCameraByRT(rvec, tvec, libGdxCam);
		}
		
		
		UtilAR.imDrawBackground(cameraImage);
		
		// Vent med at tegne modellen til vi er kalibreret. Skal denne del også være
		// EFTER imDrawBackground?
		if (isCalibrated && found) {
			modelBatch.begin(libGdxCam);
			for (ModelInstance m : boxInstances) {
				modelBatch.render(m, environment);			
			}
	        modelBatch.end();
		}
	}
	
	@Override
	public void dispose() {
		camera.release();
		modelBatch.dispose();
		boxModel.dispose();
	}
	
}
